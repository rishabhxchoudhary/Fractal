# Project Management User Journey

## Complete End-to-End Flow

This document outlines the complete user journey for project management in Fractal.

---

## 1. Dashboard Landing (`/dashboard`)

**What the user sees:**
- Welcome message with greeting
- 3 stats cards showing:
  - Tasks Due Today (0)
  - Completed This Week (0)
  - Active Projects (auto-populated)
- 3 Quick Action buttons:
  - New Task → Opens task creation
  - View Projects → Navigates to `/projects`
  - Schedule → Opens calendar view
- Recent Projects Section (if projects exist):
  - Shows up to 5 recent projects
  - Click any project to open it
  - "View All Projects" button to see full list

**User Actions:**
```
✓ Click "View Projects" button → Navigate to /projects
✓ Click project card → Navigate to /projects/[projectId]
✓ Click "New Task" → Start task creation
```

---

## 2. Projects Overview Page (`/projects`)

**What the user sees:**
- Page title and description
- "New Project" button (top right)
- Search bar to filter projects
- Grid of project cards showing:
  - Project name with color indicator
  - User's role in project (OWNER/ADMIN/EDITOR/VIEWER)
  - 3-dot menu with actions

**Empty State** (when no projects exist):
- Large icon and message
- "Create Project" button

**Project Card Actions** (based on role):
```
All Roles:
  - Click card or "Open" → Go to project detail

OWNER/ADMIN:
  - Edit → Open edit dialog
  - Members → Open members management dialog
  
OWNER only:
  - Delete → Open delete confirmation dialog
```

**User Actions:**
```
✓ Type in search box → Filter projects by name
✓ Click "New Project" → Open create project dialog
✓ Click project card → Navigate to project detail page
✓ Click menu → Select edit, members, or delete
```

---

## 3. Create Project Dialog

**Triggered by:**
- "New Project" button on projects page
- "New Sub-project" button on project detail page

**Form Fields:**
- Project Name (required)
- Color Selector (8 color options)

**User Actions:**
```
✓ Enter project name
✓ Select color
✓ Click "Create Project"
  → Dialog closes
  → Project appears in list
  → Success toast notification
```

**Validation:**
- Project name required
- Workspace must be selected
- Shows error toast on failure

---

## 4. Edit Project Dialog

**Triggered by:**
- "Edit" action from project card menu
- "Edit Project" from project detail page menu

**Form Fields:**
- Project Name (pre-filled)
- Color Selector (pre-selected)

**User Actions:**
```
✓ Update project name
✓ Change color
✓ Click "Save Changes"
  → Dialog closes
  → Project updated
  → Success toast notification
```

---

## 5. Delete Project Dialog

**Triggered by:**
- "Delete" action from project card menu
- "Delete Project" from project detail page menu

**Confirmation:**
- Shows project name
- Warning about cascading deletion
- Requires typing project name to confirm
- Only OWNER can delete

**User Actions:**
```
✓ Read warning message
✓ Type project name to confirm
✓ Click "Delete Project"
  → Dialog closes
  → Project removed from list
  → Success toast notification
```

---

## 6. Project Members Dialog

**Triggered by:**
- "Members" action from project card menu
- "Manage Members" from project detail page menu
- Only accessible to OWNER/ADMIN

**Sections:**

### Add Member Form (OWNER/ADMIN only):
- Email input field
- Role selector (EDITOR, VIEWER, ADMIN)
- "Add Member" button

**Member List Table:**
- Member avatar + name
- Email
- Role selector (dropdown for OWNER/ADMIN)
- Delete button (OWNER only)

**User Actions:**
```
✓ Enter member email
✓ Select role
✓ Click "Add Member"
  → Member added to project
  → Success toast

✓ Click role dropdown → Change member role
✓ Click delete → Remove member
```

---

## 7. Project Detail Page (`/projects/[projectId]`)

**What the user sees:**
- Back button to return to projects list
- Project name with color indicator and role badge
- 3 stat cards:
  - Sub-projects count
  - Project role
  - Project status (Active/Archived)
- Sub-projects section:
  - "New Sub-project" button (for OWNER/ADMIN/EDITOR)
  - List of sub-projects with color indicators
  - Click sub-project to navigate deeper
- Tasks section (coming soon)

**User Actions:**
```
✓ Click back button → Return to /projects
✓ Click "..." menu → Edit, Members, or Delete
✓ Click "New Sub-project" → Open create dialog with parentId set
✓ Click sub-project → Navigate to /projects/[subprojectId]
✓ Click "Edit Project" → Open edit dialog
✓ Click "Manage Members" → Open members dialog
✓ Click "Delete Project" → Open delete dialog (OWNER only)
```

---

## 8. Sub-project Creation Flow

**Same as project creation, with one difference:**

When creating from project detail page:
- `parentId` is automatically set to current project ID
- Sub-project appears in parent's sub-projects list
- Full hierarchy preserved

**User Actions:**
```
✓ On project detail page, click "New Sub-project"
✓ Fill form (same as project creation)
✓ Click "Create Project"
  → Sub-project created
  → Appears in current project's list
  → Can navigate deeper by clicking it
```

---

## 9. Project Hierarchy Navigation

**Example Flow:**
```
Dashboard
  ↓ (click View Projects)
/projects (projects list)
  ↓ (click on project)
/projects/proj-1 (project detail - shows sub-projects)
  ↓ (click on sub-project)
/projects/proj-1-sub-1 (nested project detail)
  ↓ (click "New Sub-project")
/projects/proj-1-sub-1-sub-1 (deeper nesting)
  ↓ (click back button)
/projects/proj-1-sub-1
```

**Breadcrumb Navigation** (when project sidebar is visible):
- Shows current project hierarchy
- Click breadcrumb to jump to any level
- Click X to go back to projects list

---

## 10. Complete API Flow

### Create Project:
```
POST /api/workspaces/{workspaceId}/projects
{
  "name": "My Project",
  "color": "#FF6B6B",
  "parentId": null  // or sub-project ID
}
→ Returns: Project object
```

### List Projects:
```
GET /api/workspaces/{workspaceId}/projects
→ Returns: Array of Project objects
```

### Update Project:
```
PUT /api/projects/{projectId}
{
  "name": "Updated Name",
  "color": "#4ECDC4"
}
→ Returns: Updated Project object
```

### Delete Project:
```
DELETE /api/projects/{projectId}
→ Returns: 200 OK
→ Cascades to sub-projects
```

### Get Members:
```
GET /api/projects/{projectId}/members
→ Returns: Array of ProjectMember objects
```

### Add Member:
```
POST /api/projects/{projectId}/members
{
  "userId": "user-123",
  "role": "EDITOR"  // VIEWER, EDITOR, ADMIN
}
```

### Update Member Role:
```
PUT /api/projects/{projectId}/members/{userId}
{
  "role": "ADMIN"
}
```

### Remove Member:
```
DELETE /api/projects/{projectId}/members/{userId}
```

---

## 11. Role-Based Access Control

**OWNER:**
- ✓ Create projects and sub-projects
- ✓ Edit project details
- ✓ Add/remove members
- ✓ Update member roles
- ✓ Transfer ownership
- ✓ Delete project (cascades to children)

**ADMIN:**
- ✓ Create sub-projects
- ✓ Edit project details
- ✓ Add/remove members
- ✓ Update member roles (except OWNER)
- ✗ Cannot delete project
- ✗ Cannot transfer ownership

**EDITOR:**
- ✓ Create sub-projects
- ✓ View members
- ✗ Cannot edit project details
- ✗ Cannot manage members
- ✗ Cannot delete

**VIEWER:**
- ✓ View project and members
- ✗ Cannot create
- ✗ Cannot edit
- ✗ Cannot manage members

---

## 12. Error Handling

**Common Errors & Responses:**
- Invalid project name → "Project name is required"
- Workspace not found → "Workspace not found"
- Unauthorized action → "You don't have permission"
- Member already exists → "Member already in project"
- Network error → Generic error toast

**User Experience:**
- Error toasts appear at top of screen
- All forms remain open for retry
- Loading states prevent duplicate submissions

---

## 13. State Management

**Project Context provides:**
```typescript
{
  projects: Project[]           // All projects in workspace
  currentProject: Project | null // Currently selected project
  isLoading: boolean            // Loading state
  error: string | null          // Last error message
  
  // Actions
  refreshProjects(workspaceId)
  createProject(workspaceId, data)
  updateProject(projectId, data)
  deleteProject(projectId)
  setCurrentProject(project)
}
```

**Auto-loading:**
- Projects auto-load when dashboard content mounts
- Triggered by `refreshProjects()` with current workspace ID

---

## 14. Responsive Behavior

**Mobile:**
- Single column layout on projects page
- Full-width cards
- Touch-optimized buttons
- Dropdowns instead of hover menus

**Tablet:**
- 2-column grid on projects page
- Resizable project sidebar

**Desktop:**
- 3-column grid on projects page
- Resizable project sidebar with breadcrumbs
- Full context menu hover states

---

## Testing Checklist

**Create Flow:**
- [ ] Can create project with name and color
- [ ] Can create sub-project from parent
- [ ] Form validates required fields
- [ ] Success toast shows
- [ ] Project appears in list

**Edit Flow:**
- [ ] Can edit name and color
- [ ] Changes persist after refresh
- [ ] Only OWNER/ADMIN see edit option

**Delete Flow:**
- [ ] Requires name confirmation
- [ ] Deletes project and sub-projects
- [ ] Only OWNER can delete
- [ ] Success toast shows

**Members Flow:**
- [ ] Can add member with email and role
- [ ] Can change member role
- [ ] Can remove member (OWNER only)
- [ ] List updates in real-time

**Navigation Flow:**
- [ ] Can navigate to project from dashboard
- [ ] Can navigate to sub-project
- [ ] Breadcrumbs work correctly
- [ ] Back button navigates properly

---

## Summary

The project management system provides:
- **Create**: Projects with colors and optional parent ID
- **Read**: List projects, view details, see members
- **Update**: Edit name/color, update member roles
- **Delete**: Remove projects (cascades to children)
- **Navigate**: Breadcrumbs and hierarchical navigation
- **Collaborate**: Invite members with role-based access

All operations are real-time, with error handling and user feedback at every step.
