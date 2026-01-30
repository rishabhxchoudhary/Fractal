/**
 * Workspace Role-Based Access Control (RBAC) System
 * 
 * This file defines all permissions and capabilities for workspace roles.
 * Following industry best practices for scalable RBAC implementation.
 */

export type WorkspaceRole = "OWNER" | "ADMIN" | "MEMBER";

/**
 * Permission/Capability types for workspace operations
 */
export enum WorkspacePermission {
  // Workspace Management
  UPDATE_WORKSPACE = "UPDATE_WORKSPACE",
  DELETE_WORKSPACE = "DELETE_WORKSPACE",
  
  // Member Management
  INVITE_MEMBER = "INVITE_MEMBER",
  INVITE_ADMIN = "INVITE_ADMIN",
  REMOVE_MEMBER = "REMOVE_MEMBER",
  UPDATE_MEMBER_ROLE = "UPDATE_MEMBER_ROLE",
  
  // Settings Access
  ACCESS_SETTINGS = "ACCESS_SETTINGS",
  
  // View Operations (for future use)
  VIEW_WORKSPACE = "VIEW_WORKSPACE",
  VIEW_MEMBERS = "VIEW_MEMBERS",
}

/**
 * Permission matrix: Maps roles to their allowed permissions
 * This is the single source of truth for all permissions
 */
export const ROLE_PERMISSIONS: Record<WorkspaceRole, WorkspacePermission[]> = {
  OWNER: [
    WorkspacePermission.UPDATE_WORKSPACE,
    WorkspacePermission.DELETE_WORKSPACE,
    WorkspacePermission.INVITE_MEMBER,
    WorkspacePermission.INVITE_ADMIN,
    WorkspacePermission.REMOVE_MEMBER,
    WorkspacePermission.UPDATE_MEMBER_ROLE,
    WorkspacePermission.ACCESS_SETTINGS,
    WorkspacePermission.VIEW_WORKSPACE,
    WorkspacePermission.VIEW_MEMBERS,
  ],
  ADMIN: [
    WorkspacePermission.UPDATE_WORKSPACE,
    WorkspacePermission.INVITE_MEMBER,
    WorkspacePermission.ACCESS_SETTINGS,
    WorkspacePermission.VIEW_WORKSPACE,
    WorkspacePermission.VIEW_MEMBERS,
  ],
  MEMBER: [
    WorkspacePermission.VIEW_WORKSPACE,
    WorkspacePermission.VIEW_MEMBERS,
  ],
};

/**
 * Check if a role has a specific permission
 * @param role - The user's role in the workspace
 * @param permission - The permission to check
 * @returns true if the role has the permission, false otherwise
 */
export function hasPermission(
  role: WorkspaceRole | null,
  permission: WorkspacePermission
): boolean {
  if (!role) return false;
  return ROLE_PERMISSIONS[role]?.includes(permission) ?? false;
}

/**
 * Check if a role has any of the specified permissions
 * @param role - The user's role in the workspace
 * @param permissions - Array of permissions to check
 * @returns true if the role has at least one of the permissions
 */
export function hasAnyPermission(
  role: WorkspaceRole | null,
  permissions: WorkspacePermission[]
): boolean {
  if (!role) return false;
  return permissions.some((permission) => hasPermission(role, permission));
}

/**
 * Check if a role has all of the specified permissions
 * @param role - The user's role in the workspace
 * @param permissions - Array of permissions to check
 * @returns true if the role has all of the permissions
 */
export function hasAllPermissions(
  role: WorkspaceRole | null,
  permissions: WorkspacePermission[]
): boolean {
  if (!role) return false;
  return permissions.every((permission) => hasPermission(role, permission));
}

/**
 * Get all permissions for a role
 * @param role - The user's role in the workspace
 * @returns Array of permissions for the role
 */
export function getRolePermissions(role: WorkspaceRole | null): WorkspacePermission[] {
  if (!role) return [];
  return ROLE_PERMISSIONS[role] ?? [];
}

/**
 * Check if a role is at least as powerful as another role
 * Role hierarchy: OWNER > ADMIN > MEMBER
 * @param userRole - The user's role
 * @param requiredRole - The minimum required role
 * @returns true if userRole is at least as powerful as requiredRole
 */
export function hasMinimumRole(
  userRole: WorkspaceRole | null,
  requiredRole: WorkspaceRole
): boolean {
  if (!userRole) return false;
  
  const roleHierarchy: Record<WorkspaceRole, number> = {
    OWNER: 3,
    ADMIN: 2,
    MEMBER: 1,
  };
  
  return roleHierarchy[userRole] >= roleHierarchy[requiredRole];
}

