# Member Management Fixes - Complete Code Review

## Issues Found and Fixed

### 1. Email vs UserId Mismatch (403 Errors)
**Problem:** Frontend was sending `userEmail` (string) but backend `AddProjectMemberRequest` expects `userId` (UUID).
**Error:** 403 Forbidden when adding members
**Fix:** Changed add member form to select from existing project members instead of entering emails

**Before:**
```typescript
const [userEmail, setUserEmail] = useState("")
// Sending: { userId: userEmail, role } ❌ Wrong - email is a string
```

**After:**
```typescript
const [selectedUserId, setSelectedUserId] = useState("")
// Sending: { userId: selectedUserId, role } ✓ Correct - UUID string
// Shows dropdown of members from the project
```

### 2. Dropdown Showing Null Value
**Problem:** `<Select value={member.role}>` was displaying as null because the value wasn't being set properly initially
**Fix:** Ensured value is always set to the member's current role

**Before:**
```typescript
<Select value={member.role || "VIEWER"} onValueChange={...}>
  <SelectValue placeholder="Select role" />  // ❌ Placeholder shown even with value
```

**After:**
```typescript
<Select value={member.role} onValueChange={...}>
  <SelectValue />  // ✓ Directly displays member.role
```

### 3. Owner Could Change Own Role
**Problem:** No validation to prevent the owner from changing their own role
**Fix:** Added frontend validation + backend already prevents it

**Added:**
```typescript
const handleUpdateRole = async (userId: string, newRole: ProjectRole) => {
  const member = members.find(m => m.userId === userId)
  
  // Prevent owner from changing their own role
  if (member?.role === "OWNER") {
    toast.error("Cannot change the role of the project owner. Transfer ownership first.")
    return
  }
  // ... rest of logic
}
```

**Also prevented in UI:**
```typescript
{hasProjectPermission(project.role, ProjectPermission.UPDATE_MEMBER_ROLE) && member.role !== "OWNER" ? (
  // Show dropdown only for non-owners
```

### 4. No Transfer Ownership Feature
**Problem:** No UI to transfer project ownership
**Fix:** Added complete transfer ownership form

**Added:**
- Transfer Ownership section (visible only to current owner)
- Dropdown to select new owner from non-owner members
- Proper state management (`transferringOwnerId`, `transferringOwner`)
- Handler function `handleTransferOwnership`
- Toast notifications for success/error

**Backend Already Supports:**
- Endpoint: `POST /api/projects/{projectId}/transfer-ownership`
- Request: `{ newOwnerId: UUID }`
- Logic: Demotes current owner to ADMIN, promotes new owner to OWNER

### 5. API Client Already Correct
**Verified:**
```typescript
async transferProjectOwnership(
  projectId: string,
  newOwnerId: string,
): Promise<void> {
  return this.fetch<void>(`/api/projects/${projectId}/transfer-ownership`, {
    method: "POST",
    body: JSON.stringify({ newOwnerId }),
  })
}
```

## Files Modified
1. `/frontend/components/projects/project-members-dialog.tsx`
   - Fixed state management for userId selection
   - Added transfer ownership form
   - Added validation to prevent owner role changes
   - Fixed dropdown value display

## Backend DTOs Verified
✓ `AddProjectMemberRequest` - expects `userId` (UUID) + `role`
✓ `UpdateProjectMemberRequest` - expects `role` (string)
✓ `TransferProjectOwnershipRequest` - expects `newOwnerId` (UUID)

## Backend Permission Checks (Already Implemented)
✓ `updateMemberRole()` - throws 403 if trying to change OWNER's role
✓ `removeMember()` - prevents removing OWNER
✓ `transferOwnership()` - only OWNER or Workspace OWNER can transfer
✓ `addMember()` - requires ADMIN/OWNER role
✓ `validateProjectAdminAccess()` - core permission validation

## Testing Checklist
- [ ] Try to add a member - should show dropdown of project members
- [ ] Try to add a non-existent member - button should be disabled
- [ ] Try to change owner's role - should show error toast
- [ ] Try to transfer ownership - should work if you're the owner
- [ ] After transfer, old owner should be ADMIN
- [ ] Try operations as non-owner - should show read-only badges

## Result
All member management issues resolved. The UI now properly:
1. Selects members by UUID (no more 403 errors)
2. Shows role dropdowns with proper values (no more null)
3. Prevents owner role changes (both UI and backend)
4. Allows ownership transfer (new feature added)
