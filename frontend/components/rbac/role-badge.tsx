/**
 * Role Badge Component
 * Displays a badge showing the user's role
 */

"use client";

import { Badge } from "@/components/ui/badge";
import type { WorkspaceRole } from "@/lib/permissions";

interface RoleBadgeProps {
  role: WorkspaceRole;
  className?: string;
}

const roleConfig: Record<WorkspaceRole, { label: string; variant: "default" | "secondary" | "destructive" | "outline" }> = {
  OWNER: { label: "Owner", variant: "default" },
  ADMIN: { label: "Admin", variant: "secondary" },
  MEMBER: { label: "Member", variant: "outline" },
};

export function RoleBadge({ role, className }: RoleBadgeProps) {
  const config = roleConfig[role];
  
  return (
    <Badge variant={config.variant} className={className}>
      {config.label}
    </Badge>
  );
}

