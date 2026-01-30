"use client";

import { useRouter } from "next/navigation";
import { useAuth } from "@/lib/auth-context";
import { cn, redirectToRoot, redirectToWorkspace } from "@/lib/utils";
import { Button } from "@/components/ui/button";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { Building2, Check, ChevronsUpDown, Plus, Settings } from "lucide-react";
import type { Workspace } from "@/lib/types";
import { PermissionGuard } from "@/components/rbac/permission-guard";
import { WorkspacePermission } from "@/lib/permissions";

interface WorkspaceSelectorProps {
  className?: string;
}

export function WorkspaceSelector({ className }: WorkspaceSelectorProps) {
  const router = useRouter();
  const { workspaces, currentWorkspace } = useAuth();

  const handleSelectWorkspace = (workspace: Workspace) => {
    // If selecting the current one, do nothing
    if (workspace.id === currentWorkspace?.id) return;

    // Force full browser redirect to the workspace subdomain
    redirectToWorkspace(workspace.slug, "/dashboard");
  };

  const handleCreateNew = () => {
    // Redirect to create a new workspace
    redirectToRoot("/create-workspace");
  };

  const handleWorkspaceSettings = () => {
    if (!currentWorkspace) return;
    // Redirect to the workspace settings page
    redirectToWorkspace(currentWorkspace.slug, "/settings");
  };

  return (
    <DropdownMenu>
      <DropdownMenuTrigger asChild>
        <Button
          variant="ghost"
          className={cn(
            "justify-between gap-2 h-10 px-3 hover:bg-accent",
            className,
          )}
        >
          <div className="flex items-center gap-2">
            <div className="h-6 w-6 rounded bg-foreground/10 flex items-center justify-center shrink-0">
              <Building2 className="h-3.5 w-3.5 text-foreground" />
            </div>
            <span className="font-medium truncate max-w-[150px]">
              {currentWorkspace?.name || "Select workspace"}
            </span>
          </div>
          <ChevronsUpDown className="h-4 w-4 text-muted-foreground shrink-0" />
        </Button>
      </DropdownMenuTrigger>
      <DropdownMenuContent align="start" className="w-[240px]">
        <DropdownMenuLabel className="text-xs text-muted-foreground font-normal">
          Your workspaces
        </DropdownMenuLabel>
        {workspaces.map((workspace) => (
          <DropdownMenuItem
            key={workspace.id}
            onClick={() => handleSelectWorkspace(workspace)}
            className="flex items-center justify-between cursor-pointer"
          >
            <div className="flex items-center gap-2">
              <div className="h-6 w-6 rounded bg-foreground/10 flex items-center justify-center">
                <span className="text-xs font-medium">
                  {workspace.name.charAt(0).toUpperCase()}
                </span>
              </div>
              <span className="truncate">{workspace.name}</span>
            </div>
            {currentWorkspace?.id === workspace.id && (
              <Check className="h-4 w-4 text-foreground" />
            )}
          </DropdownMenuItem>
        ))}
        <DropdownMenuSeparator />
        <DropdownMenuItem onClick={handleCreateNew} className="cursor-pointer">
          <Plus className="h-4 w-4 mr-2" />
          Create new workspace
        </DropdownMenuItem>
        <PermissionGuard permission={WorkspacePermission.ACCESS_SETTINGS} showFallback={false}>
          <DropdownMenuItem onClick={handleWorkspaceSettings} className="cursor-pointer">
            <Settings className="h-4 w-4 mr-2" />
            Workspace settings
          </DropdownMenuItem>
        </PermissionGuard>
      </DropdownMenuContent>
    </DropdownMenu>
  );
}
