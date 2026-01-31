# Project Management Routes

## Route Structure

### Dashboard Routes
- `/[domain]/dashboard` - Main dashboard with quick actions and recent projects
- `/[domain]/dashboard/inbox` - Inbox (future feature)
- `/[domain]/dashboard/tasks` - Task list (future feature)
- `/[domain]/dashboard/calendar` - Calendar view (future feature)

### Project Routes
- `/[domain]/projects` - **[NEW]** Projects overview page
  - Search and filter projects
  - Create new project button
  - Grid of project cards with actions
  
- `/[domain]/projects/[projectId]` - **[NEW]** Project detail page
  - Project header with name and color
  - Stats cards (sub-projects, role, status)
  - Sub-projects list with navigation
  - Tasks section (coming soon)
  - Action menu (Edit, Members, Delete)

### Settings Route
- `/[domain]/settings` - Workspace settings

---

## Route Components

### `/projects` - Projects Overview Page
**File:** `/frontend/app/[domain]/projects/page.tsx`

**Features:**
- List all projects in workspace
- Search/filter by project name
- Create new project button
- Project cards with color indicator
- Role badges
- Action menu per project
- Empty state when no projects

**State Management:**
- Loads from `useProjects()` context
- Filters based on `currentWorkspace`
- Auto-refreshes on mount

**Dialogs:**
- Create Project
- Edit Project
- Delete Project
- Project Members

---

### `/projects/[projectId]` - Project Detail Page
**File:** `/frontend/app/[domain]/projects/[projectId]/page.tsx`

**Features:**
- Project header with navigation
- Back button to projects list
- Color indicator and role badge
- Stats cards (sub-projects, role, status)
- Sub-projects section
  - Shows nested projects
  - Click to navigate deeper
  - "New Sub-project" button
- Tasks section (placeholder for future)
- Action menu (Edit, Members, Delete)

**State Management:**
- Sets `currentProject` in context
- Filters sub-projects from all projects
- Shows error if project not found

**Dialogs:**
- Create Sub-project
- Edit Project
- Delete Project
- Project Members

---

## Navigation Flow

### From Dashboard
```
/[domain]/dashboard
    ↓ (click "View Projects")
/[domain]/projects
    ↓ (click project card)
/[domain]/projects/[projectId]
    ↓ (click sub-project)
/[domain]/projects/[subprojectId]
    ↓ (click back)
/[domain]/projects
```

### From Sidebar
```
Dashboard Sidebar Navigation:
- Home → /[domain]/dashboard
- Projects → /[domain]/projects  [NEW]
- Inbox → /[domain]/dashboard/inbox
- My Tasks → /[domain]/dashboard/tasks
```

---

## New Files Created

### Pages
1. `/frontend/app/[domain]/projects/page.tsx`
   - Projects overview

2. `/frontend/app/[domain]/projects/[projectId]/page.tsx`
   - Project detail

### Components
1. `/frontend/components/projects/create-project-dialog.tsx`
   - Form to create new project
   - Color selector
   - Parent ID support for sub-projects

2. `/frontend/components/projects/edit-project-dialog.tsx`
   - Form to edit project name and color
   - Pre-populated values

3. `/frontend/components/projects/delete-project-dialog.tsx`
   - Confirmation dialog
   - Name confirmation required
   - Warning about cascading deletion

4. `/frontend/components/projects/project-members-dialog.tsx`
   - Add members form
   - Members table with role selector
   - Remove member functionality

### Existing Components Updated
1. `/frontend/components/dashboard/dashboard-content.tsx`
   - Shows active projects count
   - Recent projects section
   - Links to projects page

2. `/frontend/components/dashboard/dashboard-layout.tsx`
   - Added Projects link to sidebar navigation
   - Imports FolderOpen icon

---

## API Endpoints Used

### Get Projects
```
GET /api/workspaces/{workspaceId}/projects
```
**Used in:** Projects page, Dashboard

### Create Project
```
POST /api/workspaces/{workspaceId}/projects
Body: {
  name: string
  color?: string
  parentId?: string | null
}
```
**Used in:** Create Project Dialog

### Update Project
```
PUT /api/projects/{projectId}
Body: {
  name?: string
  color?: string
}
```
**Used in:** Edit Project Dialog

### Delete Project
```
DELETE /api/projects/{projectId}
```
**Used in:** Delete Project Dialog

### Get Project Members
```
GET /api/projects/{projectId}/members
```
**Used in:** Project Members Dialog

### Add Project Member
```
POST /api/projects/{projectId}/members
Body: {
  userId: string
  role: string
}
```
**Used in:** Project Members Dialog

### Update Project Member Role
```
PUT /api/projects/{projectId}/members/{userId}
Body: {
  role: string
}
```
**Used in:** Project Members Dialog

### Remove Project Member
```
DELETE /api/projects/{projectId}/members/{userId}
```
**Used in:** Project Members Dialog

---

## Query Parameters

Currently not used, but can be extended with:
- `?sort=name|date|role` - Sort projects
- `?filter=owner|admin|all` - Filter by role
- `?search=query` - Search projects
- `?page=1` - Pagination (for many projects)

---

## Breadcrumb Navigation

**Location:** Top of page when project sidebar visible

**Shows:**
- Workspace > Project > Sub-project > Current

**Interactions:**
- Click any breadcrumb to jump to that level
- Click X to close project view and go to projects list

**File:** `/frontend/components/projects/project-breadcrumbs.tsx`

---

## Sidebar Project Navigation

**Location:** Left sidebar when project is selected

**Shows:**
- Expandable tree of projects and sub-projects
- Create, edit, delete buttons
- Filter/search

**File:** `/frontend/components/projects/project-sidebar.tsx`

---

## Responsive Behavior

### Mobile (< 768px)
- Projects grid: single column
- Sidebar: hidden by default
- Full-width modals
- Touch-optimized buttons

### Tablet (768px - 1024px)
- Projects grid: 2 columns
- Sidebar: collapsible
- Resizable panels

### Desktop (> 1024px)
- Projects grid: 3 columns
- Sidebar: visible with breadcrumbs
- Resizable panels (20-40% sidebar, 60-80% content)

---

## Error Handling

**Routes handle:**
- Project not found (404)
- Unauthorized access
- Network errors
- Invalid data

**User sees:**
- Error card with message
- "Back to Projects" button
- Option to retry

---

## Data Flow

```
Dashboard
  ↓
  useAuth() → Get currentWorkspace
  useProjects() → Load projects from /api/workspaces/{id}/projects
  
Projects Page
  ↓
  Filter projects by workspace
  Show project cards
  
Project Detail
  ↓
  Find project by ID
  setCurrentProject(project)
  Filter sub-projects
  Show project details
  
Create Sub-project
  ↓
  CreateProjectDialog
  Pass parentId = currentProject.id
  On success: Refresh projects list
```

---

## Next Steps

### Features Coming Soon
1. Tasks within projects
2. Project templates
3. Project settings (archive, permissions)
4. Activity log per project
5. Project sharing via link
6. Project search with advanced filters
7. Bulk operations
8. Project favorites/pinning

### UI Enhancements
1. Drag & drop to reorder projects
2. Favorites/pinning in sidebar
3. Recently viewed projects
4. Project statistics dashboard
5. More color options
6. Custom project icons

### Backend Integration
1. Project milestones
2. Project timeline
3. Resource allocation
4. Budget tracking
5. Project templates

---

## Summary

The routing structure follows Next.js App Router conventions with:
- Dynamic segments for workspace and project IDs
- Proper nesting under `[domain]` for multi-tenant support
- Modal dialogs for CRUD operations
- Breadcrumb and sidebar navigation
- Responsive design for all screen sizes
