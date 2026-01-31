"use client"

import { useState } from "react"
import { useProjects } from "@/lib/project-context"
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogHeader,
  AlertDialogTitle,
} from "@/components/ui/alert-dialog"
import { toast } from "sonner"
import type { Project } from "@/lib/types"

interface DeleteProjectDialogProps {
  project: Project
  open: boolean
  onOpenChange: (open: boolean) => void
}

export function DeleteProjectDialog({
  project,
  open,
  onOpenChange,
}: DeleteProjectDialogProps) {
  const { deleteProject, isLoading } = useProjects()
  const [confirmText, setConfirmText] = useState("")

  const handleDelete = async () => {
    try {
      await deleteProject(project.id)
      toast.success("Project deleted successfully")
      setConfirmText("")
      onOpenChange(false)
    } catch (error) {
      toast.error(
        error instanceof Error ? error.message : "Failed to delete project"
      )
    }
  }

  const canDelete = confirmText === project.name

  return (
    <AlertDialog open={open} onOpenChange={onOpenChange}>
      <AlertDialogContent>
        <AlertDialogHeader>
          <AlertDialogTitle>Delete Project</AlertDialogTitle>
          <AlertDialogDescription className="space-y-4">
            <p>
              Are you sure you want to delete <strong>{project.name}</strong>?
            </p>
            <p className="text-destructive">
              This action cannot be undone. All sub-projects and associated data will be permanently deleted.
            </p>
            <div>
              <label className="text-sm font-medium">
                Type the project name to confirm:
              </label>
              <input
                type="text"
                placeholder={project.name}
                value={confirmText}
                onChange={(e) => setConfirmText(e.target.value)}
                className="mt-2 w-full px-3 py-2 border rounded-md text-sm"
              />
            </div>
          </AlertDialogDescription>
        </AlertDialogHeader>
        <div className="flex gap-3 justify-end">
          <AlertDialogCancel disabled={isLoading}>
            Cancel
          </AlertDialogCancel>
          <AlertDialogAction
            onClick={handleDelete}
            disabled={!canDelete || isLoading}
            className="bg-destructive text-destructive-foreground hover:bg-destructive/90"
          >
            {isLoading ? "Deleting..." : "Delete Project"}
          </AlertDialogAction>
        </div>
      </AlertDialogContent>
    </AlertDialog>
  )
}
