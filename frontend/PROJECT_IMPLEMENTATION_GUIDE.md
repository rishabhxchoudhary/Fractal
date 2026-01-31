# Project-Level UI Implementation Guide

## Overview
This document describes the implementation of project-level features in the Fractal frontend, allowing users to create, manage, and navigate projects and sub-projects within their workspace.

## Architecture

### New Files Created

#### 1. **lib/project-context.tsx**
- **Purpose**: Global project state management
- **Key Features**:
  - Manages list of projects for current workspace
  - Tracks currently selected project
  - Provides actions: `createProject`, `updateProject`, `deleteProject`, `refreshProjects`
  - Handles loading and error states
- **Usage**: Wrap your app with `<ProjectProvider>` to use `useProjects()` hook

#### 2. **lib/types.ts** (Extended)
- **New Types Added**:
  - `ProjectRole`: "OWNER" | "ADMIN" | "EDITOR" | "VIEWER"
  - `Project`: Project data structure with name, color, parentId, role
  - `ProjectMember`: Member information within a project
  - `CreateProjectRequest`: Request payload for creating projects
  - `ProjectResponse`: API response structure

#### 3. **lib/permissions.ts** (Extended)
- **New Enums**:
  - `ProjectPermission`: Fine-grained permissions for project operations
  - `ProjectRole`: 4-level hierarchy (OWNER > ADMIN > EDITOR > VIEWER)
- **New Functions**:
  - `hasProjectPermission()`: Check if role has specific permission
  - `hasMinimumProjectRole()`: Check role hierarchy
  - `PROJECT_ROLE_PERMISSIONS`: Permission matrix mapping

#### 4. **lib/api.ts** (Extended)
- **New Methods**:
  - `getProjects(workspaceId)`: Fetch all projects in workspace
  - `createProject(workspaceId, data)`: Create new project with optional parent
  - `updateProject(projectId, data)`: Update project name/color
  - `deleteProject(projectId)`: Soft delete project (cascades to children)
  - `getProjectMembers(projectId)`: Get project members list
  - `addProjectMember(projectId, userId, role)`: Add member to project
  - `updateProjectMemberRole(projectId, userId, role)`: Change member role
  - `removeProjectMember(projectId, userId)`: Remove member from project
  - `transferProjectOwnership(projectId, newOwnerId)`: Transfer project ownership

#### 5. **components/projects/project-sidebar.tsx**
- **Features**:
  - Hierarchical project tree view (infinite nesting)
  - Expandable/collapsible sub-projects
  - "New Project" button with dialog
  - Create sub-projects directly from context menu
  - Delete projects (with confirmation)
  - Permission-aware UI (only shows options user can perform)
- **Props**:
  - `onProjectSelect`: Callback when project is selected
- **State Management**:
  - Tracks expanded projects with Set
  - Shows create dialog with parent context
  - Builds tree recursively based on parentId relationships

#### 6. **components/projects/project-breadcrumbs.tsx**
- **Features**:
  - Shows full path to current project (Dashboard > Parent > Current)
  - Clickable breadcrumb navigation
  - Traverses parent relationships to build path
  - Home button to return to dashboard
- **Props**:
  - `onNavigate`: Callback for breadcrumb clicks
- **Auto-hides** when no project selected

#### 7. **components/dashboard/dashboard-layout.tsx** (Updated)
- **New Features**:
  - Integrated ProjectSidebar on left (resizable)
  - ProjectBreadcrumbs below header when project selected
  - Toggle button to show/hide project sidebar
  - ResizablePanelGroup for flexible layout
- **Layout Structure**:
  ```
  [Workspace Sidebar] | [Project Sidebar (toggleable)] | [Main Content]
  ```

#### 8. **components/dashboard/dashboard-content.tsx** (Updated)
- **Shows**:
  - Current project context in welcome message
  - Different greeting when project is selected
  - Project-specific dashboard in future

#### 9. **app/layout.tsx** (Updated)
- **Added**: ProjectProvider wraps all children
- **Ensures**: All pages have access to `useProjects()` hook

## Data Flow

### Creating a Project
```
User clicks "New Project" button
  ↓
ProjectSidebar dialog opens
  ↓
User enters name (and optional parent)
  ↓
createProject action called
  ↓
API POST /api/workspaces/{id}/projects
  ↓
Backend creates project with creator as OWNER
  ↓
ProjectProvider updates projects state
  ↓
UI reflects new project in tree
```

### Creating a Sub-Project
```
User right-clicks project → "Create Sub-project"
  ↓
Dialog opens with parent pre-selected
  ↓
Same flow as above, but with parentId set
  ↓
Backend sets up hierarchy with closure table
  ↓
Sub-project appears nested under parent
```

### Deleting a Project
```
User right-clicks → "Delete"
  ↓
Confirmation dialog shown
  ↓
deleteProject action called
  ↓
API DELETE /api/projects/{projectId}
  ↓
Backend soft-deletes project AND all descendants
  ↓
Frontend removes from state
```

### Navigating Projects
```
User clicks project in sidebar
  ↓
onProjectSelect called with project
  ↓
setCurrentProject updates context
  ↓
Breadcrumbs update automatically
  ↓
Sidebar highlights selected project
  ↓
MainContent can now display project-specific data
```

## Permission System

### Role Hierarchy
```
OWNER (4)   - Can do everything
  ↓
ADMIN (3)   - Can manage except delete
  ↓
EDITOR (2)  - Can create sub-projects, view
  ↓
VIEWER (1)  - Read-only access
```

### Permission Matrix

| Permission | OWNER | ADMIN | EDITOR | VIEWER |
|-----------|-------|-------|--------|--------|
| UPDATE_PROJECT | ✓ | ✓ | ✗ | ✗ |
| DELETE_PROJECT | ✓ | ✗ | ✗ | ✗ |
| CREATE_SUBPROJECT | ✓ | ✓ | ✓ | ✗ |
| ADD_MEMBER | ✓ | ✓ | ✗ | ✗ |
| REMOVE_MEMBER | ✓ | ✓ | ✗ | ✗ |
| UPDATE_MEMBER_ROLE | ✓ | ✓ | ✗ | ✗ |
| VIEW_PROJECT | ✓ | ✓ | ✓ | ✓ |
| VIEW_MEMBERS | ✓ | ✓ | ✓ | ✓ |

### Usage in Components
```tsx
import { hasProjectPermission, ProjectPermission } from "@/lib/permissions";

// In component
if (hasProjectPermission(project.role, ProjectPermission.UPDATE_PROJECT)) {
  // Show edit button
}

// For multiple permissions
if (hasMinimumProjectRole(project.role, "ADMIN")) {
  // Show admin controls
}
```

## API Integration

### Backend Endpoints Used

**Projects Management**
- `POST /api/workspaces/{workspaceId}/projects` - Create project
- `GET /api/workspaces/{workspaceId}/projects` - List projects
- `PUT /api/projects/{projectId}` - Update project
- `DELETE /api/projects/{projectId}` - Delete project

**Project Members**
- `GET /api/projects/{projectId}/members` - List members
- `POST /api/projects/{projectId}/members` - Add member
- `PUT /api/projects/{projectId}/members/{userId}` - Update role
- `DELETE /api/projects/{projectId}/members/{userId}` - Remove member
- `POST /api/projects/{projectId}/transfer-ownership` - Transfer ownership

### Error Handling
All API methods in `apiClient` automatically:
- Handle 204 No Content responses
- Parse error messages
- Set appropriate error state
- Throw errors for component-level handling

## Usage Examples

### 1. Using Projects in a Component
```tsx
"use client";

import { useProjects } from "@/lib/project-context";

export function MyComponent() {
  const { projects, currentProject, createProject } = useProjects();
  
  return (
    <div>
      <h1>{currentProject?.name || "No project selected"}</h1>
      <p>Total projects: {projects.length}</p>
    </div>
  );
}
```

### 2. Creating a Project Programmatically
```tsx
const { createProject } = useProjects();

try {
  const newProject = await createProject(workspaceId, {
    name: "My New Project",
    color: "#3b82f6",
    parentId: null // Root project
  });
  console.log("Created:", newProject);
} catch (error) {
  console.error("Failed:", error);
}
```

### 3. Permission-Based Rendering
```tsx
import { ProjectPermission, hasProjectPermission } from "@/lib/permissions";

{hasProjectPermission(project.role, ProjectPermission.UPDATE_PROJECT) && (
  <Button onClick={handleEdit}>Edit Project</Button>
)}
```

### 4. Breadcrumb Navigation
```tsx
import { ProjectBreadcrumbs } from "@/components/projects/project-breadcrumbs";

<ProjectBreadcrumbs onNavigate={(projectId) => {
  const project = projects.find(p => p.id === projectId);
  setCurrentProject(project);
}} />
```

## UI Components

### Project Sidebar
- **Location**: Left panel (resizable, toggleable)
- **Shows**: Hierarchical tree of projects
- **Interactive**: Select project, expand/collapse, create, delete
- **Responsive**: 20-40% width, min/max configurable

### Breadcrumbs
- **Location**: Below header when project selected
- **Shows**: Path from root to current project
- **Interactive**: Click to navigate up the hierarchy

### Dialogs
- **Create Project**: Form with name input, parent selection
- **Confirmation**: Before deleting projects

## State Management Flow

```
AuthContext (workspace-level)
  └─ ProjectContext (project-level)
      ├─ projects: Project[]
      ├─ currentProject: Project | null
      ├─ isLoading: boolean
      ├─ error: string | null
      └─ Actions:
          ├─ setCurrentProject
          ├─ refreshProjects
          ├─ createProject
          ├─ updateProject
          └─ deleteProject
```

## Next Steps for Features

### Ready to Add
1. **Task Management**: Within projects, create tasks with descriptions, due dates
2. **Project Settings**: Members, permissions, archived state
3. **Project Views**: Kanban, Gantt, List views
4. **Collaboration**: Real-time updates, activity feed

### Backend Work Needed
- Task CRUD endpoints
- Project settings endpoints
- View/filtering endpoints
- Activity logging

### Frontend Work Needed
- Task components
- Project settings dialog
- View switcher
- Activity timeline

## Troubleshooting

### Projects not loading
- Check if workspace is selected (`currentWorkspace` in auth context)
- Verify API base URL is correct
- Check browser console for API errors

### Permission issues
- Ensure user role is correctly set in API response
- Verify permission enums match backend values
- Use `hasProjectPermission` to debug role capabilities

### Sidebar not showing
- Ensure `currentProject` is not null
- Check `showProjectSidebar` state in DashboardLayout
- Verify ProjectProvider wraps the app

## File Summary

| File | Type | Purpose |
|------|------|---------|
| lib/project-context.tsx | Context | Global project state |
| lib/types.ts | Types | Project TypeScript types |
| lib/permissions.ts | Permissions | Project RBAC system |
| lib/api.ts | API | Backend integration |
| components/projects/project-sidebar.tsx | Component | Project tree view |
| components/projects/project-breadcrumbs.tsx | Component | Navigation breadcrumbs |
| components/dashboard/dashboard-layout.tsx | Layout | Main dashboard layout |
| components/dashboard/dashboard-content.tsx | Component | Dashboard home |
| app/layout.tsx | Layout | Root layout with providers |

## Backend Integration Points

The frontend expects these backend APIs to work with projects:

1. **Workspace-Project Relationship**: Projects scoped to workspaces
2. **Project Hierarchy**: Parent-child relationships via closure table
3. **RBAC**: 4-level role system (OWNER, ADMIN, EDITOR, VIEWER)
4. **Soft Deletes**: Projects marked deleted, not removed
5. **Cascading Operations**: Deleting parent affects children
6. **Member Inheritance**: Sub-projects inherit members from parent

All these are already implemented in:
- `backend/src/main/java/com/fractal/backend/controller/ProjectController.java`
- `backend/src/main/java/com/fractal/backend/service/ProjectService.java`
