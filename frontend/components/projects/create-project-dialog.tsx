"use client"

import { useState } from "react"
import { useAuth } from "@/lib/auth-context"
import { useProjects } from "@/lib/project-context"
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
import { toast } from "sonner"
import { Loader2 } from "lucide-react"

const COLORS = [
  "#FF6B6B", // Red
  "#4ECDC4", // Teal
  "#45B7D1", // Blue
  "#FFA07A", // Salmon
  "#98D8C8", // Mint
  "#F7DC6F", // Yellow
  "#BB8FCE", // Purple
  "#85C1E2", // Sky Blue
]

interface CreateProjectDialogProps {
  open: boolean
  onOpenChange: (open: boolean) => void
  parentId?: string | null
}

export function CreateProjectDialog({
  open,
  onOpenChange,
  parentId = null,
}: CreateProjectDialogProps) {
  const { currentWorkspace } = useAuth()
  const { createProject, isLoading } = useProjects()
  const [name, setName] = useState("")
  const [selectedColor, setSelectedColor] = useState(COLORS[0])

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()

    if (!name.trim()) {
      toast.error("Project name is required")
      return
    }

    if (!currentWorkspace?.id) {
      toast.error("Workspace not found")
      return
    }

    try {
      await createProject(currentWorkspace.id, {
        name: name.trim(),
        color: selectedColor,
        parentId,
      })
      toast.success("Project created successfully")
      setName("")
      setSelectedColor(COLORS[0])
      onOpenChange(false)
    } catch (error) {
      toast.error(
        error instanceof Error ? error.message : "Failed to create project"
      )
    }
  }

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-[425px]">
        <DialogHeader>
          <DialogTitle>Create New Project</DialogTitle>
          <DialogDescription>
            Create a new project to organize your work
          </DialogDescription>
        </DialogHeader>

        <form onSubmit={handleSubmit} className="space-y-6">
          <div className="space-y-2">
            <Label htmlFor="name">Project Name</Label>
            <Input
              id="name"
              placeholder="Enter project name"
              value={name}
              onChange={(e) => setName(e.target.value)}
              disabled={isLoading}
            />
          </div>

          <div className="space-y-2">
            <Label>Color</Label>
            <div className="grid grid-cols-4 gap-2">
              {COLORS.map((color) => (
                <button
                  key={color}
                  type="button"
                  onClick={() => setSelectedColor(color)}
                  className="h-10 rounded-md border-2 transition-all"
                  style={{
                    backgroundColor: color,
                    borderColor:
                      selectedColor === color ? "black" : "transparent",
                  }}
                  title={color}
                />
              ))}
            </div>
          </div>

          <div className="flex gap-3 justify-end">
            <Button
              type="button"
              variant="outline"
              onClick={() => onOpenChange(false)}
              disabled={isLoading}
            >
              Cancel
            </Button>
            <Button type="submit" disabled={isLoading} className="gap-2">
              {isLoading && <Loader2 className="h-4 w-4 animate-spin" />}
              {isLoading ? "Creating..." : "Create Project"}
            </Button>
          </div>
        </form>
      </DialogContent>
    </Dialog>
  )
}
