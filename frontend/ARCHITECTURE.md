# Projects Feature Architecture

## System Overview

```
┌─────────────────────────────────────────────────────────────────┐
│                      Application Root                           │
│  (frontend/app/layout.tsx)                                      │
│                                                                 │
│  └─ AuthProvider ────────────────────────────────────────────┐  │
│     │ Manages: user, workspaces, currentWorkspace           │  │
│     │ Auth token, login/logout                              │  │
│     │                                                        │  │
│     └─ ProjectProvider ───────────────────────────────────┐ │  │
│        │ Manages: projects, currentProject                │ │  │
│        │ CRUD operations on projects                      │ │  │
│        │                                                  │ │  │
│        └─ Application Pages & Components ────────────────┘ │  │
│           - Dashboard                                       │  │
│           - Projects                                        │  │
│           - Tasks (future)                                 │  │
│                                                             │  │
└─────────────────────────────────────────────────────────────────┘
```

---

## State Management Architecture

### Context Layer
```
┌─────────────────────────────────────────────────────────────┐
│                    AuthContext                              │
│  ┌───────────────────────────────────────────────────────┐ │
│  │ State:                                                │ │
│  │  • user: User                                        │ │
│  │  • workspaces: Workspace[]                           │ │
│  │  • currentWorkspace: Workspace | null               │ │
│  │  • currentRole: WorkspaceRole | null                │ │
│  │                                                      │ │
│  │ Actions:                                             │ │
│  │  • login() → OAuth redirect                         │ │
│  │  • logout() → Clear tokens                          │ │
│  │  • setCurrentWorkspace(ws) → Update active          │ │
│  │  • refreshWorkspaces() → Fetch from API             │ │
│  └───────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────┘
                          │
                          ▼
┌─────────────────────────────────────────────────────────────┐
│                  ProjectContext                             │
│  ┌───────────────────────────────────────────────────────┐ │
│  │ State:                                                │ │
│  │  • projects: Project[]                               │ │
│  │  • currentProject: Project | null                   │ │
│  │  • isLoading: boolean                               │ │
│  │  • error: string | null                             │ │
│  │                                                      │ │
│  │ Actions:                                             │ │
│  │  • setCurrentProject(p) → Update active             │ │
│  │  • refreshProjects(wsId) → Fetch all               │ │
│  │  • createProject(wsId, data) → POST                │ │
│  │  • updateProject(pId, data) → PUT                  │ │
│  │  • deleteProject(pId) → DELETE                     │ │
│  └───────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────┘
```

---

## Component Hierarchy

```
┌─ app/[domain]/layout.tsx
│  │ (Validates workspace access)
│  │
│  └─ DashboardLayout
│     │ (Main page layout)
│     │
│     ├─ Sidebar (Workspace selection)
│     │  └─ WorkspaceSelector
│     │     │ Select workspace
│     │     │ Create new workspace
│     │     └─ Workspace settings
│     │
│     ├─ Header
│     │  ├─ Toggle project sidebar button
│     │  └─ User menu
│     │
│     ├─ ProjectBreadcrumbs (if project selected)
│     │  │ Shows: Home > Parent > Current
│     │  │ Interactive navigation
│     │  └─ Click to navigate hierarchy
│     │
│     ├─ Content Area (Resizable if project selected)
│     │  │
│     │  ├─ ProjectSidebar (if shown)
│     │  │  │ (Resizable panel)
│     │  │  │
│     │  │  └─ Project Tree
│     │  │     ├─ Root projects
│     │  │     └─ Sub-projects (expandable)
│     │  │        ├─ New Project button
│     │  │        ├─ Project selection
│     │  │        ├─ Right-click menu
│     │  │        └─ Create Sub-project
│     │  │
│     │  └─ Main Content (Resizable panel)
│     │     │
│     │     └─ app/[domain]/dashboard/page.tsx
│     │        └─ DashboardContent
│     │           ├─ Welcome message (shows current project)
│     │           ├─ Statistics cards
│     │           ├─ Quick actions
│     │           └─ Getting started (future)
│     │
│     └─ Footer
│        └─ (Expandable in future)
```

---

## Data Flow Diagram

### Creating a Project
```
User UI
  │
  └─► ProjectSidebar (component)
      │
      └─► Dialog: "New Project"
          │ (name input, optional parent)
          │
          ▼
      createProject() hook
          │ (from useProjects)
          │
          ▼
      apiClient.createProject()
          │ (frontend/lib/api.ts)
          │
          ▼
      HTTP POST /api/workspaces/{id}/projects
          │ (JSON body: {name, color, parentId})
          │
          ├─────────────────────────────────────┐
          │                                     │
          ▼                                     ▼
      [BACKEND]                          [FRONTEND]
      ProjectController                 
          │                              updateProjectsState()
          ▼                                  │
      ProjectService                        ▼
          │                              Update projects array
          ├─► Validate access           Add new project
          ├─► Create project entity     Trigger re-render
          ├─► Build hierarchy
          ├─► Add creator as OWNER      ▼
          ├─► Inherit members           ProjectSidebar
          └─► Return ProjectResponse        │
              (HTTP 200)                    └─► Tree updates
          │                                 Close dialog
          ▼                                 New project visible
      Response: {id, name, color, 
                 parentId, role, 
                 isArchived}
```

### Selecting a Project
```
User clicks project in sidebar
  │
  ▼
ProjectSidebar.onProjectSelect(project)
  │
  ▼
useProjects.setCurrentProject(project)
  │
  ▼
ProjectContext updates currentProject state
  │
  ├─► DashboardContent re-renders
  │   (shows project in welcome message)
  │
  ├─► ProjectBreadcrumbs re-renders
  │   (builds path to current project)
  │
  ├─► ProjectSidebar highlights project
  │   (visual feedback)
  │
  └─► MainContent ready for project-specific data
      (Tasks, settings, etc.)
```

### Deleting a Project
```
User right-clicks project
  │
  ▼
Context menu: "Delete"
  │
  ▼
Confirmation dialog
  │
  ▼ (if confirmed)
deleteProject(projectId) hook
  │
  ▼
apiClient.deleteProject()
  │
  ▼
HTTP DELETE /api/projects/{id}
  │
  ├─────────────────────────────────────┐
  │                                     │
  ▼                                     ▼
[BACKEND]                          [FRONTEND]
ProjectService                     
  │                                updateProjectsState()
  ├─► Get project + descendants       │
  ├─► Soft delete all (setDeletedAt)  ▼
  ├─► Return 204 No Content       Remove from array
  │                                Trigger re-render
  ▼                                
Response: 204 No Content           ▼
                              ProjectSidebar updates
                              Project removed from tree
                              If was current project,
                              clear currentProject
```

---

## Permission System Architecture

### Backend → Frontend Flow
```
Backend Verification
  │
  ├─► Check workspace access
  │   (user is workspace member)
  │
  ├─► Check project membership
  │   (user is project member)
  │
  └─► Check role level
      (OWNER > ADMIN > EDITOR > VIEWER)

      ▼ ─────────────────────────────────┐
      │ Return ProjectResponse with role │
      └─────────────────────────────────►│
                                         ▼
                              Frontend (ProjectContext)
                                         │
                                         ▼
                              Store role in Project object
                                         │
                                         ▼
                              Components check permissions
                                         │
                      ┌───────────────────┼───────────────────┐
                      │                   │                   │
                      ▼                   ▼                   ▼
            hasProjectPermission()  hasMinimumProjectRole()  Direct role check
                      │                   │                   │
                      ▼                   ▼                   ▼
            ProjectPermission enum  Role hierarchy check   Custom logic
                      │                   │                   │
                      ▼                   ▼                   ▼
            Show/hide UI element   Show/hide UI element   Show/hide UI element
```

### Permission Checks
```
┌─ hasProjectPermission(role, permission)
│  │
│  ├─► Look up PROJECT_ROLE_PERMISSIONS[role]
│  ├─► Check if permission in array
│  └─► Return boolean
│
├─ hasMinimumProjectRole(role, requiredRole)
│  │
│  ├─► Look up roleHierarchy[role]
│  ├─► Look up roleHierarchy[requiredRole]
│  ├─► Compare numbers
│  └─► Return role >= required
│
└─ Direct: if (project.role === "OWNER")
```

---

## API Integration

### Endpoint Structure
```
WORKSPACE LEVEL
  │
  ├─► GET    /api/workspaces
  │          Get user's workspaces
  │
  ├─► POST   /api/workspaces
  │          Create workspace
  │
  ├─► PUT    /api/workspaces/{id}
  │          Update workspace
  │
  ├─► DELETE /api/workspaces/{id}
  │          Delete workspace
  │
  └─► PROJECT LEVEL (scoped to workspace)
      │
      ├─► GET    /api/workspaces/{wsId}/projects
      │          List all projects
      │
      ├─► POST   /api/workspaces/{wsId}/projects
      │          Create project (root or child)
      │
      ├─► PUT    /api/projects/{id}
      │          Update project
      │
      ├─► DELETE /api/projects/{id}
      │          Soft delete project + children
      │
      └─► MEMBER LEVEL (scoped to project)
          │
          ├─► GET    /api/projects/{id}/members
          │          List project members
          │
          ├─► POST   /api/projects/{id}/members
          │          Add member to project
          │
          ├─► PUT    /api/projects/{id}/members/{userId}
          │          Update member role
          │
          ├─► DELETE /api/projects/{id}/members/{userId}
          │          Remove member from project
          │
          └─► POST   /api/projects/{id}/transfer-ownership
              Transfer ownership to new owner
```

---

## File Organization

### By Responsibility

#### State Management (`lib/`)
```
lib/
├── auth-context.tsx          ← Authentication & workspaces
├── project-context.tsx       ← Projects CRUD & current project
├── types.ts                  ← TypeScript interfaces
├── permissions.ts            ← RBAC system
├── api.ts                    ← API client
└── utils.ts                  ← Helpers
```

#### Components (`components/`)
```
components/
├── dashboard/
│   ├── dashboard-layout.tsx  ← Main layout with projects
│   └── dashboard-content.tsx ← Dashboard home
├── projects/
│   ├── project-sidebar.tsx   ← Project tree view
│   └── project-breadcrumbs.tsx ← Navigation breadcrumbs
├── workspace/
│   └── workspace-selector.tsx ← Workspace switcher
├── rbac/
│   └── permission-guard.tsx  ← Permission wrapper
└── ui/                       ← shadcn/ui components
```

#### Pages (`app/`)
```
app/
├── layout.tsx                ← Root layout (providers)
├── (site)/                   ← Public pages
│   ├── login/
│   ├── auth/
│   └── ...
└── [domain]/                 ← Workspace domain
    ├── layout.tsx            ← Workspace layout
    ├── page.tsx              ← Workspace home
    ├── dashboard/
    │   └── page.tsx          ← Dashboard
    └── settings/
        └── page.tsx          ← Settings
```

---

## Error Handling Architecture

```
┌─ API Call (apiClient)
│  │
│  ├─ Success (200)
│  │  └─► Parse JSON → Return data
│  │
│  ├─ No Content (204)
│  │  └─► Return undefined
│  │
│  ├─ Client Error (4xx)
│  │  └─► Parse error message
│  │      └─► Throw Error
│  │
│  └─ Server Error (5xx)
│     └─► Throw Error
│
▼
ProjectContext catches error
  │
  ├─► Set error state
  ├─► Log to console
  └─► Re-throw for component handling
      │
      ▼
    Component error boundary
      │
      ├─► Display error message to user
      ├─► Log for debugging
      └─► Allow retry
```

---

## Hierarchy Storage (Closure Table)

### Database Representation
```
projects table
├─ id (UUID)
├─ workspaceId (UUID)
├─ parentId (UUID, nullable)
├─ name (String)
├─ color (String)
├─ createdBy (UUID)
├─ createdAt (DateTime)
├─ deletedAt (DateTime, nullable) ← Soft delete
└─ isArchived (Boolean)

project_hierarchy (closure table)
├─ ancestor (UUID)
├─ descendant (UUID)
└─ depth (Integer)
```

### How It Works
```
Create: Project 1
  INSERT projects (id, name, parentId)
  INSERT project_hierarchy (ancestor, descendant, depth)
    VALUES (id, id, 0)  ← Self reference

Create: Sub-Project (parent = Project 1)
  INSERT projects (id, name, parentId)
  INSERT project_hierarchy
    VALUES (id, id, 0)                           ← Self
    VALUES (Project1_id, SubProj_id, 1)         ← Child
    VALUES (Project1_ancestor, SubProj_id, 2)   ← Grandchild...

Delete: Project 1
  UPDATE projects SET deletedAt = NOW()
    WHERE id IN (SELECT descendant FROM hierarchy
                 WHERE ancestor = Project1_id)
    ← Cascades to all descendants
```

### Frontend Tree Building
```
1. GET /api/workspaces/{id}/projects
   Response: [
     {id: "1", name: "Project 1", parentId: null},
     {id: "2", name: "Sub-proj", parentId: "1"},
     {id: "3", name: "Grand-child", parentId: "2"}
   ]

2. Build tree:
   rootProjects = filter(p => !p.parentId)
     ▼ [Project 1]
   
   getChildProjects("1") = [Sub-proj]
     ▼ Project 1
        └─ Sub-proj
   
   getChildProjects("2") = [Grand-child]
     ▼ Project 1
        └─ Sub-proj
           └─ Grand-child

3. Render recursively with indentation
```

---

## Performance Considerations

### Optimization Strategies

```
1. Context Updates
   ├─► Only trigger re-render if state actually changed
   ├─► Use selective state updates (update only project, not all)
   └─► Memoize components to prevent unnecessary re-renders

2. API Caching
   ├─► Projects loaded once per workspace
   ├─► Refresh on specific actions (create, update, delete)
   └─► No automatic polling

3. Tree Rendering
   ├─► Only expanded projects render children
   ├─► Collapsible projects don't render hidden items
   └─► Lazy expansion on demand

4. Permissions
   ├─► Check once on component mount
   ├─► Re-check on project change
   ├─► Cache permission results
   └─► Memoize permission-check functions
```

---

## Extension Points

### Add New Features
```
1. Task Management
   ├─► Create lib/task-context.tsx (similar to project-context)
   ├─► Add TaskProvider to app/layout.tsx
   ├─► Create components/tasks/task-list.tsx
   └─► Add project-task association

2. Project Settings
   ├─► Create components/projects/project-settings.tsx
   ├─► Add member management UI
   ├─► Show project metadata
   └─► Archive/restore options

3. Multiple Views
   ├─► Add view switcher in header
   ├─► Create components/projects/kanban-view.tsx
   ├─► Create components/projects/gantt-view.tsx
   ├─► Store preference in localStorage

4. Activity Feed
   ├─► Create components/projects/activity-feed.tsx
   ├─► Add action logging to backend
   ├─► Display in timeline format
   └─► Filter by user/type
```

---

## Testing Strategy

### Unit Tests
```
✓ Permissions: hasProjectPermission, hasMinimumProjectRole
✓ Utilities: getChildProjects, findProject, getAncestors
✓ Hooks: useProjects, useAuth
```

### Integration Tests
```
✓ Create project → appears in sidebar
✓ Create sub-project → nested under parent
✓ Delete project → removed from tree
✓ Select project → updates breadcrumbs
✓ Permission checks → show/hide UI
```

### E2E Tests
```
✓ Full project lifecycle (create → update → delete)
✓ Project navigation (select → breadcrumb → navigate)
✓ Permission-based workflows (different roles)
✓ Error handling (network failures, validation)
```

---

## Deployment Considerations

### Frontend Build
```
✓ No build-time dependencies on backend
✓ API URL configured via environment variables
✓ Works with backend at any URL (CORS compatible)
✓ Error messages user-friendly
```

### Backend Requirements
```
✓ Database migrations for project tables
✓ Closure table for hierarchy
✓ Project hierarchy queries optimized
✓ RBAC checks on all endpoints
✓ Soft delete cascade logic
```

### Environment Variables
```
NEXT_PUBLIC_API_URL=http://localhost:8080
NEXT_PUBLIC_ROOT_DOMAIN=localhost:3000
```

---

## Summary

This architecture provides:

1. **Scalability** - Supports infinite project nesting
2. **Security** - Role-based access control at all levels
3. **Maintainability** - Clear separation of concerns
4. **Extensibility** - Easy to add features (tasks, views, etc.)
5. **Performance** - Efficient tree rendering and API usage
6. **User Experience** - Intuitive navigation and clear feedback
