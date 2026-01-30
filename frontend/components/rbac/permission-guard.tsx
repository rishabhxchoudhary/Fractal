/**
 * Permission Guard Component
 * Protects routes and UI elements based on permissions
 */

"use client";

import { ReactNode } from "react";
import { useRouter } from "next/navigation";
import { useAuth } from "@/lib/auth-context";
import {
  WorkspacePermission,
  hasPermission,
  hasAnyPermission,
  hasAllPermissions,
  hasMinimumRole,
  type WorkspaceRole,
} from "@/lib/permissions";

interface PermissionGuardProps {
  children: ReactNode;
  permission?: WorkspacePermission;
  permissions?: WorkspacePermission[];
  requireAll?: boolean;
  minimumRole?: WorkspaceRole;
  fallback?: ReactNode;
  redirectTo?: string;
  showFallback?: boolean;
}

/**
 * Permission Guard Component
 * 
 * Usage examples:
 * - <PermissionGuard permission={WorkspacePermission.DELETE_WORKSPACE}>...</PermissionGuard>
 * - <PermissionGuard permissions={[WorkspacePermission.UPDATE_WORKSPACE]} requireAll>...</PermissionGuard>
 * - <PermissionGuard minimumRole="ADMIN">...</PermissionGuard>
 */
export function PermissionGuard({
  children,
  permission,
  permissions,
  requireAll = false,
  minimumRole,
  fallback = null,
  redirectTo,
  showFallback = true,
}: PermissionGuardProps) {
  const { currentRole, currentWorkspace } = useAuth();
  const router = useRouter();

  let hasAccess = false;

  // Check by minimum role
  if (minimumRole) {
    hasAccess = hasMinimumRole(currentRole, minimumRole);
  }
  // Check by single permission
  else if (permission) {
    hasAccess = hasPermission(currentRole, permission);
  }
  // Check by multiple permissions
  else if (permissions && permissions.length > 0) {
    hasAccess = requireAll
      ? hasAllPermissions(currentRole, permissions)
      : hasAnyPermission(currentRole, permissions);
  } else {
    // No permission check specified, allow access
    hasAccess = true;
  }

  // Handle redirect if access denied
  if (!hasAccess && redirectTo) {
    const rootDomain = process.env.NEXT_PUBLIC_ROOT_DOMAIN || "localhost:3000";
    const protocol = typeof window !== "undefined" ? window.location.protocol : "http:";
    const targetUrl = currentWorkspace?.slug
      ? `${protocol}//${currentWorkspace.slug}.${rootDomain}${redirectTo}`
      : `${protocol}//${rootDomain}${redirectTo}`;
    
    if (typeof window !== "undefined") {
      window.location.href = targetUrl;
    }
    return null;
  }

  // Show fallback or nothing if access denied
  if (!hasAccess) {
    return showFallback ? <>{fallback}</> : null;
  }

  return <>{children}</>;
}

