"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { useAuth } from "@/lib/auth-context";
import { apiClient } from "@/lib/api";
import type { WorkspaceMember } from "@/lib/types";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import {
  Tabs,
  TabsContent,
  TabsList,
  TabsTrigger,
} from "@/components/ui/tabs";
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogHeader,
  AlertDialogTitle,
} from "@/components/ui/alert-dialog";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { toast } from "sonner";
import { AlertTriangle, Loader2, Mail, Users, Trash2, Shield } from "lucide-react";
import { PermissionGuard } from "@/components/rbac/permission-guard";
import { WorkspacePermission } from "@/lib/permissions";
import {
  useCanUpdateWorkspace,
  useCanDeleteWorkspace,
  useCanInviteMembers,
  useCanInviteAdmins,
  useCanRemoveMembers,
  useCanUpdateMemberRoles,
} from "@/lib/hooks/use-permissions";
import { RoleBadge } from "@/components/rbac/role-badge";

export default function WorkspaceSettingsPage() {
  const router = useRouter();
  const { currentWorkspace, refreshWorkspaces } = useAuth();
  
  // Permission checks using hooks
  const canUpdateWorkspace = useCanUpdateWorkspace();
  const canDeleteWorkspace = useCanDeleteWorkspace();
  const canInviteMembers = useCanInviteMembers();
  const canInviteAdmins = useCanInviteAdmins();
  const canRemoveMembers = useCanRemoveMembers();
  const canUpdateMemberRoles = useCanUpdateMemberRoles();
  const [workspaceName, setWorkspaceName] = useState("");
  const [workspaceSlug, setWorkspaceSlug] = useState("");
  const [isUpdating, setIsUpdating] = useState(false);
  const [isDeleting, setIsDeleting] = useState(false);
  const [showDeleteDialog, setShowDeleteDialog] = useState(false);
  const [inviteEmail, setInviteEmail] = useState("");
  const [inviteRole, setInviteRole] = useState("MEMBER");
  const [isInviting, setIsInviting] = useState(false);
  const [members, setMembers] = useState<WorkspaceMember[]>([]);
  const [isLoadingMembers, setIsLoadingMembers] = useState(false);

  // Initialize form with current workspace data
  useEffect(() => {
    if (currentWorkspace) {
      setWorkspaceName(currentWorkspace.name);
      setWorkspaceSlug(currentWorkspace.slug);
      loadMembers();
    }
  }, [currentWorkspace]);

  const loadMembers = async () => {
    if (!currentWorkspace) return;
    setIsLoadingMembers(true);
    try {
      const membersList = await apiClient.getWorkspaceMembers(currentWorkspace.id);
      setMembers(membersList);
    } catch (error) {
      console.error("Failed to load members:", error);
      toast.error("Failed to load workspace members");
    } finally {
      setIsLoadingMembers(false);
    }
  };

  const handleUpdateWorkspace = async (e: React.FormEvent) => {
    e.preventDefault();

    if (!canUpdateWorkspace) {
      toast.error("You don't have permission to update workspace settings");
      return;
    }

    if (!workspaceName.trim()) {
      toast.error("Workspace name is required");
      return;
    }

    if (!workspaceSlug.trim()) {
      toast.error("Workspace slug is required");
      return;
    }

    setIsUpdating(true);

    try {
      if (!currentWorkspace) {
        toast.error("No workspace selected");
        return;
      }

      // Check if slug has changed
      const slugChanged = workspaceSlug !== currentWorkspace.slug;

      await apiClient.updateWorkspace(
        currentWorkspace.id,
        workspaceName,
        workspaceSlug,
      );

      await refreshWorkspaces();
      toast.success("Workspace updated successfully!");

      // If slug changed, redirect to new subdomain
      if (slugChanged) {
        const rootDomain = process.env.NEXT_PUBLIC_ROOT_DOMAIN || "localhost:3000";
        const protocol = window.location.protocol;
        const targetUrl = `${protocol}//${workspaceSlug}.${rootDomain}/settings`;
        window.location.href = targetUrl;
      }
    } catch (error) {
      toast.error(
        error instanceof Error ? error.message : "Failed to update workspace",
      );
    } finally {
      setIsUpdating(false);
    }
  };

  const handleDeleteWorkspace = async () => {
    if (!canDeleteWorkspace) {
      toast.error("Only workspace owners can delete workspaces");
      return;
    }

    setIsDeleting(true);

    try {
      if (!currentWorkspace) {
        toast.error("No workspace selected");
        return;
      }

      await apiClient.deleteWorkspace(currentWorkspace.id);
      toast.success("Workspace deleted successfully!");

      // Refresh workspaces and redirect appropriately
      try {
        const workspaces = await refreshWorkspaces();
        const rootDomain = process.env.NEXT_PUBLIC_ROOT_DOMAIN || "localhost:3000";
        const protocol = window.location.protocol;
        
        if (workspaces.length === 0) {
          // No workspaces left, redirect to first-time workspace creation
          window.location.href = `${protocol}//${rootDomain}/welcome/new-workspace`;
        } else {
          // Workspaces still exist, redirect to select workspace
          window.location.href = `${protocol}//${rootDomain}/select-workspace`;
        }
      } catch (e) {
        // If fetching fails, default to select-workspace
        console.warn("Could not fetch workspaces, redirecting to select-workspace", e);
        const rootDomain = process.env.NEXT_PUBLIC_ROOT_DOMAIN || "localhost:3000";
        const protocol = window.location.protocol;
        window.location.href = `${protocol}//${rootDomain}/select-workspace`;
      }
    } catch (error) {
      toast.error(
        error instanceof Error ? error.message : "Failed to delete workspace",
      );
    } finally {
      setIsDeleting(false);
      setShowDeleteDialog(false);
    }
  };

  const handleInviteMember = async (e: React.FormEvent) => {
    e.preventDefault();

    if (!canInviteMembers) {
      toast.error("You don't have permission to invite members");
      return;
    }

    // Only OWNER can invite ADMIN
    if (inviteRole === "ADMIN" && !canInviteAdmins) {
      toast.error("Only workspace owners can invite admins");
      return;
    }

    if (!inviteEmail.trim()) {
      toast.error("Email is required");
      return;
    }

    setIsInviting(true);

    try {
      if (!currentWorkspace) {
        toast.error("No workspace selected");
        return;
      }

      await apiClient.inviteMember(currentWorkspace.id, inviteEmail, inviteRole);
      toast.success(`Invitation sent to ${inviteEmail}!`);
      setInviteEmail("");
      setInviteRole("MEMBER");
      await loadMembers();
    } catch (error) {
      toast.error(
        error instanceof Error ? error.message : "Failed to send invitation",
      );
    } finally {
      setIsInviting(false);
    }
  };

  const handleRemoveMember = async (userId: string, memberEmail: string) => {
    if (!currentWorkspace) return;

    if (!canRemoveMembers) {
      toast.error("Only workspace owners can remove members");
      return;
    }

    try {
      await apiClient.removeMember(currentWorkspace.id, userId);
      toast.success(`Removed ${memberEmail} from workspace`);
      await loadMembers();
    } catch (error) {
      toast.error(
        error instanceof Error ? error.message : "Failed to remove member",
      );
    }
  };

  const handleUpdateMemberRole = async (userId: string, newRole: string) => {
    if (!currentWorkspace) return;

    if (!canUpdateMemberRoles) {
      toast.error("Only workspace owners can update member roles");
      return;
    }

    try {
      await apiClient.updateMemberRole(currentWorkspace.id, userId, newRole);
      toast.success("Member role updated successfully");
      await loadMembers();
    } catch (error) {
      toast.error(
        error instanceof Error ? error.message : "Failed to update member role",
      );
    }
  };

  return (
    <PermissionGuard
      permission={WorkspacePermission.ACCESS_SETTINGS}
      redirectTo="/dashboard"
    >
      <div className="flex flex-col">
        {/* Header */}
        <div className="border-b bg-background/80 px-6 py-4">
          <h1 className="text-2xl font-bold tracking-tight">
            Workspace Settings
          </h1>
          <p className="text-sm text-muted-foreground">
            Manage your workspace details, members, and security settings
          </p>
        </div>

        {/* Content */}
        <div className="flex-1 overflow-auto">
          <div className="max-w-2xl mx-auto px-6 py-8">
            <Tabs defaultValue="general" className="w-full">
              <TabsList className="grid w-full grid-cols-2">
                <TabsTrigger value="general">General</TabsTrigger>
                <TabsTrigger value="members">Members</TabsTrigger>
              </TabsList>

              {/* General Tab */}
              <TabsContent value="general" className="space-y-6">
                {/* Basic Settings - Only visible to OWNER and ADMIN */}
                <PermissionGuard permission={WorkspacePermission.UPDATE_WORKSPACE}>
                  <Card>
                    <CardHeader>
                      <CardTitle>Workspace Information</CardTitle>
                      <CardDescription>
                        Update your workspace name and custom URL slug
                      </CardDescription>
                    </CardHeader>
                    <CardContent>
                      <form onSubmit={handleUpdateWorkspace} className="space-y-6">
                      <div className="space-y-2">
                        <Label htmlFor="workspace-name">Workspace Name</Label>
                        <Input
                          id="workspace-name"
                          placeholder="My Workspace"
                          value={workspaceName}
                          onChange={(e) => setWorkspaceName(e.target.value)}
                        />
                      </div>

                      <div className="space-y-2">
                        <Label htmlFor="workspace-slug">Workspace URL</Label>
                        <div className="flex items-center gap-2">
                          <Input
                            id="workspace-slug"
                            placeholder="my-workspace"
                            value={workspaceSlug}
                            onChange={(e) => setWorkspaceSlug(e.target.value)}
                          />
                          <span className="text-sm text-muted-foreground whitespace-nowrap">
                            .{process.env.NEXT_PUBLIC_ROOT_DOMAIN || "localhost:3000"}
                          </span>
                        </div>
                        <p className="text-xs text-muted-foreground">
                          This is your unique workspace URL. Changing it will redirect your workspace to a new subdomain.
                        </p>
                      </div>

                      <Button
                        type="submit"
                        disabled={isUpdating}
                      >
                        {isUpdating ? (
                          <>
                            <Loader2 className="h-4 w-4 animate-spin mr-2" />
                            Updating...
                          </>
                        ) : (
                          "Save Changes"
                        )}
                      </Button>
                    </form>
                  </CardContent>
                </Card>
                </PermissionGuard>

                {/* Danger Zone - Only visible to OWNER */}
                <PermissionGuard permission={WorkspacePermission.DELETE_WORKSPACE}>
                  <Card className="border-destructive/50 bg-destructive/5">
                    <CardHeader>
                      <CardTitle className="flex items-center gap-2 text-destructive">
                        <AlertTriangle className="h-5 w-5" />
                        Danger Zone
                      </CardTitle>
                      <CardDescription>
                        Irreversible and destructive actions
                      </CardDescription>
                    </CardHeader>
                    <CardContent>
                      <div className="space-y-4">
                        <p className="text-sm text-muted-foreground">
                          Deleting a workspace will permanently remove it along with all associated data. This action cannot be undone. Only workspace owners can delete workspaces.
                        </p>
                        <Button
                          variant="destructive"
                          onClick={() => setShowDeleteDialog(true)}
                          disabled={isDeleting}
                        >
                          Delete Workspace
                        </Button>
                      </div>
                    </CardContent>
                  </Card>
                </PermissionGuard>
              </TabsContent>

              {/* Members Tab */}
              <TabsContent value="members" className="space-y-6">
                {/* Invite Member - Only visible to OWNER and ADMIN */}
                <PermissionGuard permission={WorkspacePermission.INVITE_MEMBER}>
                  <Card>
                    <CardHeader>
                      <CardTitle className="flex items-center gap-2">
                        <Users className="h-5 w-5" />
                        Invite Member
                      </CardTitle>
                      <CardDescription>
                        Invite new members to your workspace
                      </CardDescription>
                    </CardHeader>
                  <CardContent>
                    <form onSubmit={handleInviteMember} className="space-y-6">
                      <div className="space-y-2">
                        <Label htmlFor="invite-email">Email Address</Label>
                        <div className="flex items-center gap-2">
                          <Input
                            id="invite-email"
                            type="email"
                            placeholder="member@example.com"
                            value={inviteEmail}
                            onChange={(e) => setInviteEmail(e.target.value)}
                          />
                        </div>
                      </div>

                      <div className="space-y-2">
                        <Label htmlFor="invite-role">Role</Label>
                        <Select value={inviteRole} onValueChange={setInviteRole}>
                          <SelectTrigger id="invite-role">
                            <SelectValue />
                          </SelectTrigger>
                          <SelectContent>
                            <SelectItem value="MEMBER">Member</SelectItem>
                            {canInviteAdmins && (
                              <SelectItem value="ADMIN">Admin</SelectItem>
                            )}
                          </SelectContent>
                        </Select>
                        {!canInviteAdmins && (
                          <p className="text-xs text-muted-foreground">
                            Admins can only invite members. Only owners can invite admins.
                          </p>
                        )}
                      </div>

                      <Button
                        type="submit"
                        disabled={isInviting}
                        className="w-full"
                      >
                        {isInviting ? (
                          <>
                            <Loader2 className="h-4 w-4 animate-spin mr-2" />
                            Sending Invitation...
                          </>
                        ) : (
                          <>
                            <Mail className="h-4 w-4 mr-2" />
                            Send Invitation
                          </>
                        )}
                      </Button>
                    </form>
                  </CardContent>
                </Card>
                </PermissionGuard>

                <Card>
                  <CardHeader>
                    <CardTitle className="flex items-center gap-2">
                      <Shield className="h-5 w-5" />
                      Workspace Members
                    </CardTitle>
                    <CardDescription>
                      Manage members and their roles
                    </CardDescription>
                  </CardHeader>
                  <CardContent>
                    {isLoadingMembers ? (
                      <div className="flex items-center justify-center py-8">
                        <Loader2 className="h-5 w-5 animate-spin text-muted-foreground" />
                      </div>
                    ) : members.length === 0 ? (
                      <p className="text-sm text-muted-foreground">
                        No members yet. Invite someone to get started!
                      </p>
                    ) : (
                      <div className="space-y-4">
                        {members.map((member) => (
                          <div
                            key={member.id}
                            className="flex items-center justify-between p-4 border rounded-lg"
                          >
                            <div className="flex flex-col gap-1">
                              <p className="font-medium">{member.fullName}</p>
                              <p className="text-sm text-muted-foreground">
                                {member.email}
                              </p>
                            </div>
                            <div className="flex items-center gap-3">
                              {member.role === "OWNER" && (
                                <RoleBadge role="OWNER" />
                              )}
                              {member.role !== "OWNER" && canUpdateMemberRoles && (
                                <>
                                  <Select
                                    value={member.role}
                                    onValueChange={(newRole) =>
                                      handleUpdateMemberRole(member.id, newRole)
                                    }
                                  >
                                    <SelectTrigger className="w-32">
                                      <SelectValue />
                                    </SelectTrigger>
                                    <SelectContent>
                                      <SelectItem value="MEMBER">Member</SelectItem>
                                      <SelectItem value="ADMIN">Admin</SelectItem>
                                    </SelectContent>
                                  </Select>
                                  {canRemoveMembers && (
                                    <Button
                                      variant="ghost"
                                      size="sm"
                                      onClick={() =>
                                        handleRemoveMember(member.id, member.email)
                                      }
                                      className="text-destructive hover:text-destructive"
                                    >
                                      <Trash2 className="h-4 w-4" />
                                    </Button>
                                  )}
                                </>
                              )}
                              {member.role !== "OWNER" && !canUpdateMemberRoles && (
                                <RoleBadge role={member.role} />
                              )}
                            </div>
                          </div>
                        ))}
                      </div>
                    )}
                  </CardContent>
                </Card>
              </TabsContent>
            </Tabs>
          </div>
        </div>

        {/* Delete Confirmation Dialog */}
        <AlertDialog open={showDeleteDialog} onOpenChange={setShowDeleteDialog}>
          <AlertDialogContent>
            <AlertDialogHeader>
              <AlertDialogTitle>Delete Workspace?</AlertDialogTitle>
              <AlertDialogDescription>
                This action cannot be undone. All data associated with this workspace will be permanently deleted.
              </AlertDialogDescription>
            </AlertDialogHeader>
            <div className="flex gap-3">
              <AlertDialogCancel>Cancel</AlertDialogCancel>
              <Button
                variant="destructive"
                onClick={handleDeleteWorkspace}
                disabled={isDeleting}
                className="text-white"
              >
                {isDeleting ? (
                  <>
                    <Loader2 className="h-4 w-4 animate-spin mr-2" />
                    Deleting...
                  </>
                ) : (
                  "Delete Workspace"
                )}
              </Button>
            </div>
          </AlertDialogContent>
        </AlertDialog>
      </div>
    </PermissionGuard>
  );
}
