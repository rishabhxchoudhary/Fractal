"use client"

import { useState, useEffect } from "react"
import { apiClient } from "@/lib/api"
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select"
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table"
import { Badge } from "@/components/ui/badge"
import { Avatar, AvatarFallback, AvatarImage } from "@/components/ui/avatar"
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu"
import { toast } from "sonner"
import { Loader2, MoreVertical, Trash2 } from "lucide-react"
import { hasProjectPermission, ProjectPermission } from "@/lib/permissions"
import type { Project, ProjectMember, ProjectRole } from "@/lib/types"

interface ProjectMembersDialogProps {
  project: Project
  open: boolean
  onOpenChange: (open: boolean) => void
}

export function ProjectMembersDialog({
  project,
  open,
  onOpenChange,
}: ProjectMembersDialogProps) {
  const [members, setMembers] = useState<ProjectMember[]>([])
  const [isLoading, setIsLoading] = useState(false)
  const [selectedUserId, setSelectedUserId] = useState("")
  const [selectedRole, setSelectedRole] = useState<ProjectRole>("EDITOR")
  const [addingMember, setAddingMember] = useState(false)
  const [transferringOwnerId, setTransferringOwnerId] = useState("")
  const [transferringOwner, setTransferringOwner] = useState(false)
  const [workspaceMembers, setWorkspaceMembers] = useState<any[]>([])
  const [loadingWorkspaceMembers, setLoadingWorkspaceMembers] = useState(false)

  useEffect(() => {
    if (open) {
      loadMembers()
      loadWorkspaceMembers()
    }
  }, [open, project.id])

  const loadMembers = async () => {
    try {
      setIsLoading(true)
      const data = await apiClient.getProjectMembers(project.id)
      setMembers(data || [])
    } catch (error) {
      toast.error("Failed to load members")
    } finally {
      setIsLoading(false)
    }
  }

  const loadWorkspaceMembers = async () => {
    try {
      setLoadingWorkspaceMembers(true)
      // Get workspace members to allow adding them to project
      // For now, show existing members from project as they're already invited to workspace
    } catch (error) {
      console.error("Failed to load workspace members")
    } finally {
      setLoadingWorkspaceMembers(false)
    }
  }

  const handleAddMember = async (e: React.FormEvent) => {
    e.preventDefault()

    if (!selectedUserId.trim()) {
      toast.error("Please select a member")
      return
    }

    // Check if already a member
    if (members.find(m => m.userId === selectedUserId)) {
      toast.error("This member is already part of the project")
      return
    }

    try {
      setAddingMember(true)
      await apiClient.addProjectMember(project.id, selectedUserId, selectedRole)
      toast.success("Member added successfully")
      setSelectedUserId("")
      setSelectedRole("EDITOR")
      await loadMembers()
    } catch (error) {
      toast.error(
        error instanceof Error ? error.message : "Failed to add member"
      )
    } finally {
      setAddingMember(false)
    }
  }

  const handleRemoveMember = async (userId: string) => {
    try {
      await apiClient.removeProjectMember(project.id, userId)
      toast.success("Member removed successfully")
      await loadMembers()
    } catch (error) {
      toast.error("Failed to remove member")
    }
  }

  const handleUpdateRole = async (userId: string, newRole: ProjectRole) => {
    const member = members.find(m => m.userId === userId)
    
    // Prevent owner from changing their own role
    if (member?.role === "OWNER") {
      toast.error("Cannot change the role of the project owner. Transfer ownership first.")
      return
    }

    try {
      await apiClient.updateProjectMemberRole(project.id, userId, newRole)
      toast.success("Role updated successfully")
      await loadMembers()
    } catch (error) {
      toast.error(
        error instanceof Error ? error.message : "Failed to update role"
      )
    }
  }

  const handleTransferOwnership = async (e: React.FormEvent) => {
    e.preventDefault()

    if (!transferringOwnerId.trim()) {
      toast.error("Please select a member to transfer ownership to")
      return
    }

    try {
      setTransferringOwner(true)
      await apiClient.transferProjectOwnership(project.id, transferringOwnerId)
      toast.success("Ownership transferred successfully")
      setTransferringOwnerId("")
      await loadMembers()
    } catch (error) {
      toast.error(
        error instanceof Error ? error.message : "Failed to transfer ownership"
      )
    } finally {
      setTransferringOwner(false)
    }
  }

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-[600px]">
        <DialogHeader>
          <DialogTitle>Project Members</DialogTitle>
          <DialogDescription>
            Manage members and their roles in {project.name}
          </DialogDescription>
        </DialogHeader>

        <div className="space-y-6">
          {/* Transfer Ownership Form */}
          {project.role === "OWNER" && (
            <form onSubmit={handleTransferOwnership} className="space-y-4 p-4 border rounded-lg bg-yellow-50/50 dark:bg-yellow-950/10 border-yellow-200/50 dark:border-yellow-900/50">
              <h4 className="font-medium text-yellow-900 dark:text-yellow-100">Transfer Ownership</h4>
              <p className="text-sm text-yellow-800 dark:text-yellow-200">Select a project member to make them the new owner. You will become an admin.</p>
              <div className="space-y-3">
                <div>
                  <Label className="text-sm">New Owner</Label>
                  <Select value={transferringOwnerId} onValueChange={setTransferringOwnerId}>
                    <SelectTrigger disabled={transferringOwner}>
                      <SelectValue placeholder="Select new owner..." />
                    </SelectTrigger>
                    <SelectContent>
                      {members
                        .filter(m => m.role !== "OWNER")
                        .map(member => (
                          <SelectItem key={member.userId} value={member.userId}>
                            {member.fullName} ({member.email})
                          </SelectItem>
                        ))}
                    </SelectContent>
                  </Select>
                </div>
                <Button
                  type="submit"
                  disabled={transferringOwner || !transferringOwnerId}
                  className="w-full gap-2"
                >
                  {transferringOwner && <Loader2 className="h-4 w-4 animate-spin" />}
                  {transferringOwner ? "Transferring..." : "Transfer Ownership"}
                </Button>
              </div>
            </form>
          )}

          {/* Add Member Form */}
          {hasProjectPermission(project.role, ProjectPermission.ADD_MEMBER) && (
            <form onSubmit={handleAddMember} className="space-y-4 p-4 border rounded-lg bg-muted/50">
              <h4 className="font-medium">Add New Member</h4>
              <p className="text-sm text-muted-foreground">Select a workspace member to add to this project</p>
              <div className="space-y-3">
                <div>
                  <Label className="text-sm">Member</Label>
                  <Select value={selectedUserId} onValueChange={setSelectedUserId}>
                    <SelectTrigger disabled={addingMember}>
                      <SelectValue placeholder="Select a member..." />
                    </SelectTrigger>
                    <SelectContent>
                      {members
                        .filter(m => m.role !== "OWNER") // Filter out owner
                        .map(member => (
                          <SelectItem key={member.userId} value={member.userId}>
                            {member.fullName} ({member.email})
                          </SelectItem>
                        ))}
                    </SelectContent>
                  </Select>
                </div>
                <div>
                  <Label className="text-sm">Role</Label>
                  <Select value={selectedRole} onValueChange={(value) => setSelectedRole(value as ProjectRole)}>
                    <SelectTrigger disabled={addingMember}>
                      <SelectValue />
                    </SelectTrigger>
                    <SelectContent>
                      <SelectItem value="VIEWER">Viewer</SelectItem>
                      <SelectItem value="EDITOR">Editor</SelectItem>
                      <SelectItem value="ADMIN">Admin</SelectItem>
                    </SelectContent>
                  </Select>
                </div>
              </div>
              <Button
                type="submit"
                disabled={addingMember || !selectedUserId}
                className="w-full gap-2"
              >
                {addingMember && <Loader2 className="h-4 w-4 animate-spin" />}
                {addingMember ? "Adding..." : "Add Member"}
              </Button>
            </form>
          )}

          {/* Members Table */}
          {isLoading ? (
            <div className="flex items-center justify-center py-8">
              <Loader2 className="h-6 w-6 animate-spin text-muted-foreground" />
            </div>
          ) : members.length === 0 ? (
            <div className="text-center py-8 text-muted-foreground">
              No members yet
            </div>
          ) : (
            <div className="border rounded-lg overflow-hidden">
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead>Member</TableHead>
                    <TableHead>Email</TableHead>
                    <TableHead>Role</TableHead>
                    <TableHead className="text-right">Action</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {members.map((member) => (
                    <TableRow key={member.userId}>
                      <TableCell>
                        <div className="flex items-center gap-2">
                          <Avatar className="h-8 w-8">
                            <AvatarImage src={member.avatarUrl} alt={member.fullName} />
                            <AvatarFallback>
                              {member.fullName.charAt(0).toUpperCase()}
                            </AvatarFallback>
                          </Avatar>
                          <div>
                            <p className="font-medium">{member.fullName}</p>
                          </div>
                        </div>
                      </TableCell>
                      <TableCell className="text-sm">{member.email}</TableCell>
                      <TableCell>
                        {hasProjectPermission(project.role, ProjectPermission.UPDATE_MEMBER_ROLE) && member.role !== "OWNER" ? (
                          <Select value={member.role} onValueChange={(role) => handleUpdateRole(member.userId, role as ProjectRole)}>
                            <SelectTrigger className="w-32">
                              <SelectValue />
                            </SelectTrigger>
                            <SelectContent>
                              <SelectItem value="VIEWER">Viewer</SelectItem>
                              <SelectItem value="EDITOR">Editor</SelectItem>
                              <SelectItem value="ADMIN">Admin</SelectItem>
                            </SelectContent>
                          </Select>
                        ) : (
                          <Badge variant={member.role === "OWNER" ? "default" : "secondary"}>{member.role}</Badge>
                        )}
                      </TableCell>
                      <TableCell className="text-right">
                        {hasProjectPermission(project.role, ProjectPermission.REMOVE_MEMBER) && member.role !== "OWNER" && (
                          <DropdownMenu>
                            <DropdownMenuTrigger asChild>
                              <Button variant="ghost" size="icon" className="h-8 w-8">
                                <MoreVertical className="h-4 w-4" />
                              </Button>
                            </DropdownMenuTrigger>
                            <DropdownMenuContent align="end">
                              <DropdownMenuItem
                                onClick={() => handleRemoveMember(member.userId)}
                                className="text-destructive focus:text-destructive"
                              >
                                <Trash2 className="h-4 w-4 mr-2" />
                                Remove
                              </DropdownMenuItem>
                            </DropdownMenuContent>
                          </DropdownMenu>
                        )}
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </div>
          )}
        </div>

        <div className="flex justify-end">
          <Button variant="outline" onClick={() => onOpenChange(false)}>
            Close
          </Button>
        </div>
      </DialogContent>
    </Dialog>
  )
}
