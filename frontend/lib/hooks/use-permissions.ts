/**
 * Custom hooks for permission checks
 * Provides easy-to-use hooks for components to check permissions
 */

import { useAuth } from "@/lib/auth-context";
import {
  WorkspacePermission,
  hasPermission,
  hasAnyPermission,
  hasAllPermissions,
  hasMinimumRole,
  type WorkspaceRole,
} from "@/lib/permissions";

/**
 * Hook to check if the current user has a specific permission
 * @param permission - The permission to check
 * @returns true if the user has the permission
 */
export function useHasPermission(permission: WorkspacePermission): boolean {
  const { currentRole } = useAuth();
  return hasPermission(currentRole, permission);
}

/**
 * Hook to check if the current user has any of the specified permissions
 * @param permissions - Array of permissions to check
 * @returns true if the user has at least one of the permissions
 */
export function useHasAnyPermission(
  permissions: WorkspacePermission[]
): boolean {
  const { currentRole } = useAuth();
  return hasAnyPermission(currentRole, permissions);
}

/**
 * Hook to check if the current user has all of the specified permissions
 * @param permissions - Array of permissions to check
 * @returns true if the user has all of the permissions
 */
export function useHasAllPermissions(
  permissions: WorkspacePermission[]
): boolean {
  const { currentRole } = useAuth();
  return hasAllPermissions(currentRole, permissions);
}

/**
 * Hook to check if the current user has at least the minimum required role
 * @param requiredRole - The minimum required role
 * @returns true if the user has at least the required role
 */
export function useHasMinimumRole(requiredRole: WorkspaceRole): boolean {
  const { currentRole } = useAuth();
  return hasMinimumRole(currentRole, requiredRole);
}

/**
 * Hook to get the current user's role
 * @returns The current user's role or null
 */
export function useCurrentRole(): WorkspaceRole | null {
  const { currentRole } = useAuth();
  return currentRole;
}

/**
 * Hook for checking if user can delete workspace (OWNER only)
 */
export function useCanDeleteWorkspace(): boolean {
  return useHasPermission(WorkspacePermission.DELETE_WORKSPACE);
}

/**
 * Hook for checking if user can update workspace (OWNER or ADMIN)
 */
export function useCanUpdateWorkspace(): boolean {
  return useHasPermission(WorkspacePermission.UPDATE_WORKSPACE);
}

/**
 * Hook for checking if user can access settings (OWNER or ADMIN)
 */
export function useCanAccessSettings(): boolean {
  return useHasPermission(WorkspacePermission.ACCESS_SETTINGS);
}

/**
 * Hook for checking if user can invite members (OWNER or ADMIN)
 */
export function useCanInviteMembers(): boolean {
  return useHasPermission(WorkspacePermission.INVITE_MEMBER);
}

/**
 * Hook for checking if user can invite admins (OWNER only)
 */
export function useCanInviteAdmins(): boolean {
  return useHasPermission(WorkspacePermission.INVITE_ADMIN);
}

/**
 * Hook for checking if user can remove members (OWNER only)
 */
export function useCanRemoveMembers(): boolean {
  return useHasPermission(WorkspacePermission.REMOVE_MEMBER);
}

/**
 * Hook for checking if user can update member roles (OWNER only)
 */
export function useCanUpdateMemberRoles(): boolean {
  return useHasPermission(WorkspacePermission.UPDATE_MEMBER_ROLE);
}

