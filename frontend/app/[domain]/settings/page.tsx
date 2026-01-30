"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { useAuth } from "@/lib/auth-context";
import { apiClient } from "@/lib/api";
import { DashboardLayout } from "@/components/dashboard/dashboard-layout";
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
import { AlertTriangle, Loader2, Mail, Users } from "lucide-react";
import { getSubdomain } from "@/lib/utils";

export default function WorkspaceSettingsPage() {
  const router = useRouter();
  const { currentWorkspace, refreshWorkspaces } = useAuth();
  const [workspaceName, setWorkspaceName] = useState("");
  const [workspaceSlug, setWorkspaceSlug] = useState("");
  const [isUpdating, setIsUpdating] = useState(false);
  const [isDeleting, setIsDeleting] = useState(false);
  const [showDeleteDialog, setShowDeleteDialog] = useState(false);
  const [inviteEmail, setInviteEmail] = useState("");
  const [inviteRole, setInviteRole] = useState("MEMBER");
  const [isInviting, setIsInviting] = useState(false);
  const [isWorkspaceResolved, setIsWorkspaceResolved] = useState(false);

  // Initialize form with current workspace data
  useEffect(() => {
    if (currentWorkspace) {
      setWorkspaceName(currentWorkspace.name);
      setWorkspaceSlug(currentWorkspace.slug);
      setIsWorkspaceResolved(true);
    }
  }, [currentWorkspace]);

  const handleUpdateWorkspace = async (e: React.FormEvent) => {
    e.preventDefault();

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
    setIsDeleting(true);

    try {
      if (!currentWorkspace) {
        toast.error("No workspace selected");
        return;
      }

      await apiClient.deleteWorkspace(currentWorkspace.id);
      await refreshWorkspaces();
      toast.success("Workspace deleted successfully!");

      // Redirect to workspace selection
      const rootDomain = process.env.NEXT_PUBLIC_ROOT_DOMAIN || "localhost:3000";
      const protocol = window.location.protocol;
      window.location.href = `${protocol}//${rootDomain}/select-workspace`;
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

    if (!inviteEmail.trim()) {
      toast.error("Email is required");
      return;
    }

    // Basic email validation
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    if (!emailRegex.test(inviteEmail)) {
      toast.error("Please enter a valid email address");
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
    } catch (error) {
      toast.error(
        error instanceof Error ? error.message : "Failed to send invitation",
      );
    } finally {
      setIsInviting(false);
    }
  };

  if (!isWorkspaceResolved) {
    return (
      <DashboardLayout>
        <div className="flex items-center justify-center min-h-screen">
          <div className="flex flex-col items-center gap-2">
            <Loader2 className="h-8 w-8 animate-spin text-muted-foreground" />
            <p className="text-muted-foreground">Loading settings...</p>
          </div>
        </div>
      </DashboardLayout>
    );
  }

  return (
    <DashboardLayout>
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
                {/* Basic Settings */}
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

                {/* Danger Zone */}
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
                        Deleting a workspace will permanently remove it along with all associated data. This action cannot be undone.
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
              </TabsContent>

              {/* Members Tab */}
              <TabsContent value="members" className="space-y-6">
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
                            <SelectItem value="ADMIN">Admin</SelectItem>
                          </SelectContent>
                        </Select>
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

                <Card>
                  <CardHeader>
                    <CardTitle>Member List</CardTitle>
                    <CardDescription>
                      API to list members coming soon
                    </CardDescription>
                  </CardHeader>
                  <CardContent>
                    <p className="text-sm text-muted-foreground">
                      Member management UI will be available once the backend API for listing members is implemented.
                    </p>
                  </CardContent>
                </Card>
              </TabsContent>
            </Tabs>
          </div>
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
            <AlertDialogAction
              onClick={handleDeleteWorkspace}
              className="bg-destructive text-destructive-foreground hover:bg-destructive/90"
              disabled={isDeleting}
            >
              {isDeleting ? (
                <>
                  <Loader2 className="h-4 w-4 animate-spin mr-2" />
                  Deleting...
                </>
              ) : (
                "Delete Workspace"
              )}
            </AlertDialogAction>
          </div>
        </AlertDialogContent>
      </AlertDialog>
    </DashboardLayout>
  );
}
