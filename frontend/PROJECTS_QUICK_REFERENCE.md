# Projects Quick Reference

## Quick Links
- 📋 Full Guide: `frontend/PROJECT_IMPLEMENTATION_GUIDE.md`
- 📊 Summary: `../IMPLEMENTATION_SUMMARY.md`

---

## Import Statements

### Using Projects
```tsx
import { useProjects } from "@/lib/project-context";
```

### Using Permissions
```tsx
import { 
  hasProjectPermission, 
  hasMinimumProjectRole,
  ProjectPermission 
} from "@/lib/permissions";
```

### Components
```tsx
import { ProjectSidebar } from "@/components/projects/project-sidebar";
import { ProjectBreadcrumbs } from "@/components/projects/project-breadcrumbs";
```

---

## useProjects Hook

### Available Properties & Methods

```typescript
// State
projects: Project[]                    // All projects in workspace
currentProject: Project | null         // Currently selected project
isLoading: boolean                     // Loading state
error: string | null                  // Error message if any

// Actions
setCurrentProject(project)             // Set/clear current project
refreshProjects(workspaceId)           // Refresh from server
createProject(workspaceId, data)       // Create new project
updateProject(projectId, data)         // Update name/color
deleteProject(projectId)               // Delete project
```

---

## Common Operations

### 1. Get All Projects
```tsx
const { projects } = useProjects();
console.log(projects); // Array of Project objects
```

### 2. Get Current Project
```tsx
const { currentProject } = useProjects();
if (currentProject) {
  console.log(`Working on: ${currentProject.name}`);
}
```

### 3. Create Project
```tsx
const { createProject } = useProjects();
const { currentWorkspace } = useAuth();

const newProject = await createProject(currentWorkspace.id, {
  name: "My New Project",
  color: "#3b82f6"
});
```

### 4. Create Sub-Project
```tsx
const newSubProject = await createProject(workspaceId, {
  name: "Sub-project",
  color: "#ef4444",
  parentId: parentProject.id  // ← Set parent ID
});
```

### 5. Delete Project
```tsx
const { deleteProject } = useProjects();

await deleteProject(projectId);
// Also deletes all sub-projects
```

### 6. Switch to Project
```tsx
const { setCurrentProject } = useProjects();

setCurrentProject(project);
// Updates breadcrumbs, sidebar, etc.
```

---

## Permission Checks

### Check Single Permission
```tsx
if (hasProjectPermission(project.role, ProjectPermission.UPDATE_PROJECT)) {
  // Show edit button
}
```

### Check Role Level
```tsx
if (hasMinimumProjectRole(project.role, "ADMIN")) {
  // Show admin controls
}
```

### Available Permissions
```typescript
ProjectPermission.UPDATE_PROJECT        // OWNER, ADMIN
ProjectPermission.DELETE_PROJECT        // OWNER only
ProjectPermission.CREATE_SUBPROJECT     // OWNER, ADMIN, EDITOR
ProjectPermission.ADD_MEMBER            // OWNER, ADMIN
ProjectPermission.REMOVE_MEMBER         // OWNER, ADMIN
ProjectPermission.UPDATE_MEMBER_ROLE    // OWNER, ADMIN
ProjectPermission.VIEW_PROJECT          // All roles
ProjectPermission.VIEW_MEMBERS          // All roles
```

### Available Roles
```typescript
"OWNER"   // Full access
"ADMIN"   // Can manage members & settings
"EDITOR"  // Can create sub-projects
"VIEWER"  // Read-only
```

---

## Components

### ProjectSidebar
```tsx
<ProjectSidebar 
  onProjectSelect={(project) => setCurrentProject(project)} 
/>
```
- Renders hierarchical project tree
- Shows create/delete options based on permissions
- Expandable sub-projects

### ProjectBreadcrumbs
```tsx
<ProjectBreadcrumbs 
  onNavigate={(projectId) => {
    const project = projects.find(p => p.id === projectId);
    setCurrentProject(project);
  }} 
/>
```
- Shows path: Dashboard > Parent > Current
- Click to navigate up hierarchy
- Auto-hides when no project selected

---

## Component Integration

### In Page Component
```tsx
"use client";

import { useProjects } from "@/lib/project-context";

export default function ProjectPage() {
  const { currentProject } = useProjects();
  
  return (
    <div>
      <h1>{currentProject?.name || "No project"}</h1>
      {/* Your content */}
    </div>
  );
}
```

### Permission-Based Rendering
```tsx
import { ProjectPermission, hasProjectPermission } from "@/lib/permissions";

{hasProjectPermission(project.role, ProjectPermission.UPDATE_PROJECT) && (
  <div>
    <Button onClick={handleEdit}>Edit</Button>
    <Button onClick={handleDelete}>Delete</Button>
  </div>
)}
```

---

## Data Types

### Project
```typescript
{
  id: string              // UUID
  name: string            // Project name
  color?: string          // Hex color (e.g., "#3b82f6")
  parentId?: string       // Parent project ID (null for root)
  role: ProjectRole       // User's role (OWNER, ADMIN, EDITOR, VIEWER)
  isArchived?: boolean    // Soft delete status
}
```

### ProjectResponse (from API)
```typescript
{
  id: UUID
  name: string
  color?: string
  parentId?: UUID
  role: string
  isArchived: boolean
}
```

### CreateProjectRequest
```typescript
{
  name: string              // Required
  color?: string            // Optional, hex color
  parentId?: UUID | null    // Optional, for sub-projects
}
```

---

## API Methods (in lib/api.ts)

```typescript
// Projects
apiClient.getProjects(workspaceId)
apiClient.createProject(workspaceId, data)
apiClient.updateProject(projectId, data)
apiClient.deleteProject(projectId)

// Members
apiClient.getProjectMembers(projectId)
apiClient.addProjectMember(projectId, userId, role)
apiClient.updateProjectMemberRole(projectId, userId, role)
apiClient.removeProjectMember(projectId, userId)
apiClient.transferProjectOwnership(projectId, newOwnerId)
```

---

## Error Handling

```tsx
const { createProject, error, isLoading } = useProjects();

try {
  const newProject = await createProject(workspaceId, data);
  console.log("Success:", newProject);
} catch (err) {
  console.error("Failed:", err.message);
}

// Or use error state
if (error) {
  return <p>Error: {error}</p>;
}

if (isLoading) {
  return <p>Loading...</p>;
}
```

---

## Hooks

### useProjects()
Get project state and actions
```tsx
const { 
  projects, 
  currentProject, 
  createProject,
  isLoading,
  error 
} = useProjects();
```

### useAuth()
Get workspace context (needed for workspaceId)
```tsx
const { currentWorkspace } = useAuth();
```

---

## Common Patterns

### 1. Load Projects on Mount
```tsx
useEffect(() => {
  if (currentWorkspace?.id) {
    refreshProjects(currentWorkspace.id);
  }
}, [currentWorkspace?.id, refreshProjects]);
```
✅ Already done in ProjectSidebar & ProjectProvider

### 2. Handle Create with Confirmation
```tsx
const handleCreate = async () => {
  if (!name.trim()) return;
  
  try {
    await createProject(workspaceId, { name });
    // Success handled by context
  } catch (error) {
    toast.error("Failed to create");
  }
};
```
✅ Already done in ProjectSidebar

### 3. Find Project by ID
```tsx
const findProject = (id: string) => projects.find(p => p.id === id);
const parent = findProject(currentProject?.parentId);
```

### 4. Get All Ancestors
```tsx
const getAncestors = (project: Project) => {
  const ancestors = [];
  let current = project;
  
  while (current.parentId) {
    const parent = projects.find(p => p.id === current.parentId);
    if (!parent) break;
    ancestors.unshift(parent);
    current = parent;
  }
  
  return ancestors;
};
```
✅ Already done in ProjectBreadcrumbs

---

## Troubleshooting

### Projects not showing?
1. Check `currentWorkspace` is set
2. Verify API endpoint URL
3. Check browser console for errors
4. Ensure user is in workspace

### Permissions not working?
1. Verify role comes from API
2. Check permission enum spelling
3. Use `console.log(project.role)` to debug
4. Ensure `hasProjectPermission` called correctly

### Sidebar not toggling?
1. Check `currentProject` is not null
2. Verify `showProjectSidebar` state updates
3. Check ResizablePanelGroup renders
4. Ensure ProjectProvider wraps component

---

## Next: Adding Features

### Add Task Management
1. Create `lib/task-context.tsx`
2. Add task API methods to `lib/api.ts`
3. Create `components/tasks/task-list.tsx`
4. Follow same pattern as projects

### Add Project Settings
1. Create settings dialog in `components/projects/project-settings.tsx`
2. Add members management UI
3. Add archive/delete options
4. Show project info

### Add Multiple Views
1. Create view switcher component
2. Implement Kanban, Gantt, List views
3. Store view preference in localStorage
4. Switch between views dynamically

---

## File Structure
```
frontend/
├── lib/
│   ├── project-context.tsx        ← State management
│   ├── types.ts                   ← Project types
│   ├── permissions.ts             ← RBAC
│   └── api.ts                     ← API methods
├── components/
│   ├── projects/
│   │   ├── project-sidebar.tsx    ← Tree view
│   │   └── project-breadcrumbs.tsx ← Navigation
│   └── dashboard/
│       └── dashboard-layout.tsx   ← Main layout
├── app/
│   ├── layout.tsx                 ← Provider setup
│   └── [domain]/
│       ├── dashboard/
│       │   └── page.tsx           ← Uses projects
│       └── layout.tsx             ← Domain layout
└── PROJECT_IMPLEMENTATION_GUIDE.md ← Full docs
```

---

## Quick Copy-Paste

### Component using Projects
```tsx
"use client";

import { useProjects } from "@/lib/project-context";
import { useAuth } from "@/lib/auth-context";

export function MyProjectComponent() {
  const { projects, currentProject, createProject } = useProjects();
  const { currentWorkspace } = useAuth();

  const handleCreate = async () => {
    if (!currentWorkspace) return;
    
    const newProject = await createProject(currentWorkspace.id, {
      name: "New Project",
      color: "#3b82f6"
    });
    
    console.log("Created:", newProject);
  };

  return (
    <div>
      <h2>Projects: {projects.length}</h2>
      <p>Current: {currentProject?.name || "None"}</p>
      <button onClick={handleCreate}>Create</button>
    </div>
  );
}
```

---

## Cheat Sheet

| Task | Code |
|------|------|
| Get projects | `const { projects } = useProjects()` |
| Get current | `const { currentProject } = useProjects()` |
| Create | `await createProject(wsId, { name })` |
| Delete | `await deleteProject(projectId)` |
| Switch | `setCurrentProject(project)` |
| Check perm | `hasProjectPermission(role, perm)` |
| Check role | `hasMinimumProjectRole(role, "ADMIN")` |
| Parent ID | `project.parentId` |
| Is root | `!project.parentId` |
| User role | `project.role` |

---

## Support

- 📖 Full Documentation: `PROJECT_IMPLEMENTATION_GUIDE.md`
- 📝 Summary: `../IMPLEMENTATION_SUMMARY.md`
- 🔗 Backend APIs: See `backend/src/main/java/com/fractal/backend/controller/ProjectController.java`
