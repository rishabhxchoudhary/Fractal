# Project Management System - Flow Diagrams

## 1. Complete User Journey Flow

```
START: User visits dashboard
│
├─→ [Dashboard Page]
│   ├─ Active Projects: 0
│   ├─ Recent Projects: (empty)
│   └─ 3 Quick Action Buttons
│       ├─ New Task
│       ├─ [View Projects] ←─ USER CLICKS HERE
│       └─ Schedule
│
├─→ [Projects Overview Page]
│   ├─ Search bar
│   ├─ "New Project" button
│   ├─ Empty state (OR project cards)
│   │
│   └─ USER ACTIONS:
│       ├─ Click "New Project" → Create Dialog
│       │   └─ Fill form → Create Project
│       │       └─ [Back to Projects List]
│       │
│       ├─ Click project card → Project Detail Page
│       │   ├─ Project Header
│       │   ├─ Stats Cards
│       │   ├─ Sub-projects List
│       │   ├─ "New Sub-project" button
│       │   └─ Menu (Edit, Members, Delete)
│       │
│       └─ Click menu → Dialog
│           ├─ Edit Dialog
│           ├─ Members Dialog
│           └─ Delete Dialog
│
├─→ [Project Detail Page]
│   ├─ Back button → Back to Projects
│   ├─ Project name + color + role
│   ├─ Stats: sub-projects, role, status
│   ├─ Sub-projects List
│   │   └─ Click sub-project → Project Detail (nested)
│   ├─ Tasks section (placeholder)
│   └─ Menu (Edit, Members, Delete)
│
└─→ Hierarchy Navigation
    Project
    ├─ Sub-project 1
    │  ├─ Sub-sub-project 1.1
    │  └─ Sub-sub-project 1.2
    └─ Sub-project 2
       └─ Sub-sub-project 2.1
```

---

## 2. State Management Flow

```
App Layout
│
└─→ ProjectProvider (Global Context)
    │
    ├─ State:
    │  ├─ projects: Project[]
    │  ├─ currentProject: Project | null
    │  ├─ isLoading: boolean
    │  └─ error: string | null
    │
    ├─ Actions:
    │  ├─ refreshProjects(workspaceId)
    │  │  └─ GET /api/workspaces/{id}/projects
    │  │
    │  ├─ createProject(workspaceId, data)
    │  │  └─ POST /api/workspaces/{id}/projects
    │  │
    │  ├─ updateProject(projectId, data)
    │  │  └─ PUT /api/projects/{id}
    │  │
    │  ├─ deleteProject(projectId)
    │  │  └─ DELETE /api/projects/{id}
    │  │
    │  └─ setCurrentProject(project)
    │
    └─→ Components using context:
        ├─ DashboardContent
        ├─ ProjectsPage
        ├─ ProjectDetailPage
        ├─ CreateProjectDialog
        ├─ EditProjectDialog
        ├─ DeleteProjectDialog
        └─ ProjectMembersDialog
```

---

## 3. API Integration Flow

```
Frontend Actions
│
├─ Create Project
│  └─ POST /api/workspaces/{id}/projects
│     ├─ Body: { name, color, parentId? }
│     └─ Response: Project object
│         ├─ Added to projects state
│         ├─ Dialog closes
│         ├─ Success toast shows
│         └─ List re-renders
│
├─ Get Projects
│  └─ GET /api/workspaces/{id}/projects
│     ├─ Called on mount
│     ├─ Called on workspace change
│     └─ Response: Project[] array
│         └─ Stored in projects state
│
├─ Update Project
│  └─ PUT /api/projects/{id}
│     ├─ Body: { name?, color? }
│     └─ Response: Updated project
│         ├─ Updated in state
│         ├─ currentProject updated
│         └─ List re-renders
│
├─ Delete Project
│  └─ DELETE /api/projects/{id}
│     ├─ Requires confirmation
│     └─ Response: 200 OK
│         ├─ Removed from projects array
│         ├─ Sub-projects also removed
│         └─ currentProject cleared
│
├─ Get Members
│  └─ GET /api/projects/{id}/members
│     └─ Response: ProjectMember[] array
│         └─ Displayed in Members Dialog
│
├─ Add Member
│  └─ POST /api/projects/{id}/members
│     ├─ Body: { userId, role }
│     └─ Response: 200 OK
│         ├─ Member added to list
│         └─ Form reset
│
├─ Update Member Role
│  └─ PUT /api/projects/{id}/members/{userId}
│     ├─ Body: { role }
│     └─ Response: 200 OK
│         └─ Member list refreshed
│
└─ Remove Member
   └─ DELETE /api/projects/{id}/members/{userId}
      └─ Response: 200 OK
          └─ Member removed from list
```

---

## 4. Component Tree

```
App [Layout]
│
├─ ProjectProvider [Context]
│  │
│  └─ [domain]/layout [Layout]
│     │
│     ├─ DashboardLayout [Layout]
│     │  ├─ Sidebar
│     │  │  ├─ WorkspaceSelector
│     │  │  ├─ Navigation Items
│     │  │  │  ├─ Home
│     │  │  │  ├─ [Projects] ← NEW
│     │  │  │  ├─ Inbox
│     │  │  │  └─ Tasks
│     │  │  └─ User Menu
│     │  │
│     │  ├─ Header
│     │  ├─ ProjectBreadcrumbs [Conditional]
│     │  ├─ ProjectSidebar [Conditional]
│     │  └─ Main Content
│     │
│     └─ Page Content
│        ├─ DashboardContent
│        │  ├─ Stats Cards
│        │  ├─ Quick Actions
│        │  ├─ Recent Projects ← NEW
│        │  └─ Getting Started
│        │
│        ├─ ProjectsPage ← NEW
│        │  ├─ Header
│        │  ├─ Search Bar
│        │  ├─ Project Grid
│        │  │  └─ Project Cards
│        │  ├─ CreateProjectDialog
│        │  ├─ EditProjectDialog
│        │  ├─ DeleteProjectDialog
│        │  └─ ProjectMembersDialog
│        │
│        └─ ProjectDetailPage ← NEW
│           ├─ Header
│           ├─ Stats Cards
│           ├─ Sub-projects Section
│           ├─ Tasks Section
│           ├─ CreateProjectDialog (Sub-project)
│           ├─ EditProjectDialog
│           ├─ DeleteProjectDialog
│           └─ ProjectMembersDialog
```

---

## 5. RBAC (Role-Based Access Control) Flow

```
User Opens Project
│
├─ Get user's role: OWNER | ADMIN | EDITOR | VIEWER
│
├─ Determine permissions:
│  │
│  ├─ OWNER
│  │  ├─ UPDATE_PROJECT ✓
│  │  ├─ DELETE_PROJECT ✓
│  │  ├─ CREATE_SUBPROJECT ✓
│  │  ├─ ADD_MEMBER ✓
│  │  ├─ REMOVE_MEMBER ✓
│  │  ├─ UPDATE_MEMBER_ROLE ✓
│  │  ├─ VIEW_PROJECT ✓
│  │  └─ VIEW_MEMBERS ✓
│  │
│  ├─ ADMIN
│  │  ├─ UPDATE_PROJECT ✓
│  │  ├─ DELETE_PROJECT ✗
│  │  ├─ CREATE_SUBPROJECT ✓
│  │  ├─ ADD_MEMBER ✓
│  │  ├─ REMOVE_MEMBER ✓
│  │  ├─ UPDATE_MEMBER_ROLE ✓
│  │  ├─ VIEW_PROJECT ✓
│  │  └─ VIEW_MEMBERS ✓
│  │
│  ├─ EDITOR
│  │  ├─ UPDATE_PROJECT ✗
│  │  ├─ DELETE_PROJECT ✗
│  │  ├─ CREATE_SUBPROJECT ✓
│  │  ├─ ADD_MEMBER ✗
│  │  ├─ REMOVE_MEMBER ✗
│  │  ├─ UPDATE_MEMBER_ROLE ✗
│  │  ├─ VIEW_PROJECT ✓
│  │  └─ VIEW_MEMBERS ✓
│  │
│  └─ VIEWER
│     ├─ UPDATE_PROJECT ✗
│     ├─ DELETE_PROJECT ✗
│     ├─ CREATE_SUBPROJECT ✗
│     ├─ ADD_MEMBER ✗
│     ├─ REMOVE_MEMBER ✗
│     ├─ UPDATE_MEMBER_ROLE ✗
│     ├─ VIEW_PROJECT ✓
│     └─ VIEW_MEMBERS ✓
│
└─ Show/Hide UI Elements
   ├─ Edit button: show if OWNER or ADMIN
   ├─ Delete button: show if OWNER only
   ├─ Members button: show if OWNER or ADMIN
   ├─ New Sub-project: show if OWNER, ADMIN, or EDITOR
   └─ All other actions: disabled based on role
```

---

## 6. Dialog Flow

### Create Project Dialog
```
User clicks "New Project"
│
└─ CreateProjectDialog opens
   ├─ Form Fields:
   │  ├─ Project Name (required)
   │  └─ Color Selector (8 colors)
   │
   ├─ User Action:
   │  ├─ Enters name
   │  ├─ Selects color
   │  └─ Clicks "Create Project"
   │
   ├─ Validation:
   │  ├─ Name required? → Error toast
   │  └─ Name empty? → Error toast
   │
   ├─ API Call:
   │  └─ POST /api/workspaces/{id}/projects
   │     ├─ Loading state: buttons disabled
   │     └─ Response:
   │        ├─ Success → Add to projects array
   │        ├─ Success → Close dialog
   │        ├─ Success → Show success toast
   │        └─ Error → Show error toast
   │
   └─ Dialog Closes
      └─ User sees new project in list
```

### Edit Project Dialog
```
User clicks "Edit" or "Edit Project"
│
└─ EditProjectDialog opens
   ├─ Pre-fills:
   │  ├─ Project Name
   │  └─ Current Color
   │
   ├─ User Action:
   │  ├─ Changes name (optional)
   │  ├─ Changes color (optional)
   │  └─ Clicks "Save Changes"
   │
   ├─ API Call:
   │  └─ PUT /api/projects/{id}
   │     ├─ Loading state: buttons disabled
   │     └─ Response:
   │        ├─ Success → Update projects array
   │        ├─ Success → Update currentProject
   │        ├─ Success → Close dialog
   │        ├─ Success → Show success toast
   │        └─ Error → Show error toast
   │
   └─ Dialog Closes
      └─ User sees updated project
```

### Delete Project Dialog
```
User clicks "Delete"
│
└─ DeleteProjectDialog opens
   ├─ Shows:
   │  ├─ Project name
   │  ├─ Warning message
   │  ├─ "Type project name to confirm"
   │  └─ Confirmation input
   │
   ├─ User Action:
   │  ├─ Types project name
   │  └─ Clicks "Delete Project"
   │
   ├─ Validation:
   │  ├─ Name matches? → Enable button
   │  └─ Name doesn't match? → Disable button
   │
   ├─ API Call:
   │  └─ DELETE /api/projects/{id}
   │     ├─ Loading state: buttons disabled
   │     └─ Response:
   │        ├─ Success → Remove from projects array
   │        ├─ Success → Remove sub-projects
   │        ├─ Success → Clear currentProject
   │        ├─ Success → Close dialog
   │        ├─ Success → Show success toast
   │        └─ Error → Show error toast
   │
   └─ Dialog Closes
      └─ User sees updated project list
```

### Members Dialog
```
User clicks "Members" or "Manage Members"
│
└─ ProjectMembersDialog opens
   ├─ Add Member Form (if OWNER or ADMIN):
   │  ├─ Email input
   │  ├─ Role selector
   │  └─ "Add Member" button
   │
   ├─ Members Table:
   │  ├─ Member avatar, name, email
   │  ├─ Role selector (if OWNER or ADMIN)
   │  ├─ Delete button (if OWNER)
   │  └─ Refresh on action
   │
   ├─ User Actions:
   │  ├─ Add member:
   │  │  ├─ Enter email
   │  │  ├─ Select role
   │  │  └─ Click "Add Member"
   │  │     └─ POST /api/projects/{id}/members
   │  │
   │  ├─ Update role:
   │  │  ├─ Click role dropdown
   │  │  └─ Select new role
   │  │     └─ PUT /api/projects/{id}/members/{userId}
   │  │
   │  └─ Remove member:
   │     ├─ Click delete button
   │     └─ DELETE /api/projects/{id}/members/{userId}
   │
   └─ Dialog Closes
      └─ User sees updated members list
```

---

## 7. Data Flow Example: Create Project

```
[Projects Page Component]
                │
                │ useProjects() hook
                ↓
        [ProjectContext]
                │
                │ createProject(workspaceId, data)
                ↓
        [API Client]
                │
                │ POST /api/workspaces/{id}/projects
                ↓
        [Backend Server]
                │
                │ Create project in DB
                │ Set user as OWNER
                │ Return Project object
                ↓
        [API Client - Success]
                │
                │ setProjects([...prev, newProject])
                │ toast.success("Project created")
                ↓
        [Create Dialog]
                │
                │ Close dialog
                │ Reset form
                ↓
        [Projects Page]
                │
                │ Render new project in grid
                ↓
        [User Sees New Project]
```

---

## 8. Routing Structure

```
App
│
└─ [domain] - Multi-tenant wrapper
   │
   ├─ / - Redirect to dashboard
   │
   ├─ /dashboard - Main dashboard
   │  ├─ /inbox
   │  ├─ /tasks
   │  └─ /calendar
   │
   ├─ /projects ← NEW
   │  ├─ [projectId] ← NEW
   │  │  └─ Shows project details
   │  │
   │  └─ Create/Edit/Delete via dialogs
   │
   ├─ /settings
   │
   └─ Other routes...
```

---

## 9. Search & Filter Flow

```
User types in search box
│
└─ onSearchChange() fires
   │
   ├─ Update searchQuery state
   │
   ├─ Filter projects:
   │  └─ projects.filter(p =>
   │      p.name.toLowerCase()
   │       .includes(query.toLowerCase())
   │     )
   │
   ├─ Update displayedProjects
   │
   └─ Components re-render
      └─ Show filtered projects only
```

---

## 10. Mobile Responsive Flow

```
Viewport Width Check
│
├─ < 768px (Mobile)
│  ├─ Grid: 1 column
│  ├─ Sidebar: hidden (toggle available)
│  ├─ Modals: full width
│  └─ Buttons: large tap targets
│
├─ 768px - 1024px (Tablet)
│  ├─ Grid: 2 columns
│  ├─ Sidebar: collapsible
│  ├─ Modals: max-width
│  └─ Buttons: standard size
│
└─ > 1024px (Desktop)
   ├─ Grid: 3 columns
   ├─ Sidebar: always visible
   ├─ Panels: resizable
   └─ Buttons: standard size
```

---

## Summary

**The complete project management system flow:**
1. User navigates to Projects from dashboard
2. Sees list of projects they have access to
3. Can create, edit, delete projects
4. Can navigate project hierarchies
5. Can manage team members
6. All actions use real backend APIs
7. Proper error handling at each step
8. Role-based permissions control actions
9. Responsive on all devices
10. State persists across navigation

**All flows follow REST conventions and proper error handling patterns.**
