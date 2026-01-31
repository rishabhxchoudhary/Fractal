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
  const [userEmail, setUserEmail] = useState("")
  const [selectedRole, setSelectedRole] = useState<ProjectRole>("EDITOR")
  const [addingMember, setAddingMember] = useState(false)

  useEffect(() => {
    if (open) {
      loadMembers()
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

  const handleAddMember = async (e: React.FormEvent) => {
    e.preventDefault()

    if (!userEmail.trim()) {
      toast.error("Email is required")
      return
    }

    try {
      setAddingMember(true)
      // In a real app, you'd fetch the user ID by email first
      // For now, using email as placeholder
      await apiClient.addProjectMember(project.id, userEmail, selectedRole)
      toast.success("Member added successfully")
      setUserEmail("")
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
    try {
      await apiClient.updateProjectMemberRole(project.id, userId, newRole)
      toast.success("Role updated successfully")
      await loadMembers()
    } catch (error) {
      toast.error("Failed to update role")
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
          {/* Add Member Form */}
          {project.role === "OWNER" || project.role === "ADMIN" ? (
            <form onSubmit={handleAddMember} className="space-y-4 p-4 border rounded-lg bg-muted/50">
              <h4 className="font-medium">Add New Member</h4>
              <div className="grid grid-cols-3 gap-2">
                <div className="col-span-2">
                  <Input
                    type="email"
                    placeholder="Enter member email"
                    value={userEmail}
                    onChange={(e) => setUserEmail(e.target.value)}
                    disabled={addingMember}
                  />
                </div>
                <Select value={selectedRole} onValueChange={(value) => setSelectedRole(value as ProjectRole)}>
                  <SelectTrigger disabled={addingMember}>
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="EDITOR">Editor</SelectItem>
                    <SelectItem value="VIEWER">Viewer</SelectItem>
                    <SelectItem value="ADMIN">Admin</SelectItem>
                  </SelectContent>
                </Select>
              </div>
              <Button
                type="submit"
                disabled={addingMember}
                className="w-full gap-2"
              >
                {addingMember && <Loader2 className="h-4 w-4 animate-spin" />}
                {addingMember ? "Adding..." : "Add Member"}
              </Button>
            </form>
          ) : null}

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
                    <TableRow key={member.id}>
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
                        {project.role === "OWNER" || project.role === "ADMIN" ? (
                          <Select value={member.role} onValueChange={(role) => handleUpdateRole(member.id, role as ProjectRole)}>
                            <SelectTrigger className="w-32">
                              <SelectValue />
                            </SelectTrigger>
                            <SelectContent>
                              <SelectItem value="EDITOR">Editor</SelectItem>
                              <SelectItem value="VIEWER">Viewer</SelectItem>
                              <SelectItem value="ADMIN">Admin</SelectItem>
                            </SelectContent>
                          </Select>
                        ) : (
                          <Badge>{member.role}</Badge>
                        )}
                      </TableCell>
                      <TableCell className="text-right">
                        {project.role === "OWNER" && member.role !== "OWNER" ? (
                          <DropdownMenu>
                            <DropdownMenuTrigger asChild>
                              <Button variant="ghost" size="icon" className="h-8 w-8">
                                <MoreVertical className="h-4 w-4" />
                              </Button>
                            </DropdownMenuTrigger>
                            <DropdownMenuContent align="end">
                              <DropdownMenuItem
                                onClick={() => handleRemoveMember(member.id)}
                                className="text-destructive focus:text-destructive"
                              >
                                <Trash2 className="h-4 w-4 mr-2" />
                                Remove
                              </DropdownMenuItem>
                            </DropdownMenuContent>
                          </DropdownMenu>
                        ) : null}
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
