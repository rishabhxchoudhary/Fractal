"use client"

import { useEffect, useState } from "react"
import { useParams, useRouter } from "next/navigation"
import { useAuth } from "@/lib/auth-context"
import { useProjects } from "@/lib/project-context"
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import { Button } from "@/components/ui/button"
import { Badge } from "@/components/ui/badge"
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu"
import { Skeleton } from "@/components/ui/skeleton"
import {
  MoreVertical,
  Edit2,
  Trash2,
  Users,
  FolderPlus,
  ArrowLeft,
  AlertCircle,
} from "lucide-react"
import { CreateProjectDialog } from "@/components/projects/create-project-dialog"
import { EditProjectDialog } from "@/components/projects/edit-project-dialog"
import { DeleteProjectDialog } from "@/components/projects/delete-project-dialog"
import { ProjectMembersDialog } from "@/components/projects/project-members-dialog"
import { hasProjectPermission, ProjectPermission } from "@/lib/permissions"
import type { Project } from "@/lib/types"
import { toast } from "sonner"

export default function ProjectDetailPage() {
  const router = useRouter()
  const params = useParams()
  const projectId = params.projectId as string
  const { currentWorkspace } = useAuth()
  const { projects, setCurrentProject, refreshProjects } = useProjects()
  const [openCreateDialog, setOpenCreateDialog] = useState(false)
  const [editingProject, setEditingProject] = useState<Project | null>(null)
  const [deletingProject, setDeletingProject] = useState<Project | null>(null)
  const [membersProject, setMembersProject] = useState<Project | null>(null)

  const project = projects.find((p) => p.id === projectId)
  const subprojects = projects.filter((p) => p.parentId === projectId)

  useEffect(() => {
    if (currentWorkspace?.id && !project) {
      refreshProjects(currentWorkspace.id)
    }
  }, [projectId, currentWorkspace?.id, project, refreshProjects])

  useEffect(() => {
    if (project) {
      setCurrentProject(project)
    }
  }, [project, setCurrentProject])

  if (!project) {
    return (
      <div className="p-6 space-y-6">
        <div className="flex items-center gap-2">
          <Button
            variant="ghost"
            size="icon"
            onClick={() => router.back()}
          >
            <ArrowLeft className="h-4 w-4" />
          </Button>
        </div>
        <Card className="border-destructive/50">
          <CardContent className="pt-6">
            <div className="flex items-center gap-3">
              <AlertCircle className="h-5 w-5 text-destructive" />
              <div>
                <p className="font-medium">Project not found</p>
                <p className="text-sm text-muted-foreground">
                  The project you're looking for doesn't exist.
                </p>
              </div>
              <Button
                variant="outline"
                onClick={() => router.push("..")}
                className="ml-auto"
              >
                Back to Projects
              </Button>
            </div>
          </CardContent>
        </Card>
      </div>
    )
  }

  return (
    <div className="p-6 space-y-6">
      {/* Header */}
      <div className="flex items-start justify-between">
        <div className="flex items-start gap-4">
          <Button
            variant="ghost"
            size="icon"
            onClick={() => router.back()}
          >
            <ArrowLeft className="h-4 w-4" />
          </Button>
          <div>
            <div className="flex items-center gap-3">
              <div
                className="h-4 w-4 rounded-full"
                style={{ backgroundColor: project.color || "#808080" }}
              />
              <h1 className="text-3xl font-bold tracking-tight">{project.name}</h1>
              <Badge variant="secondary">{project.role}</Badge>
            </div>
            <p className="text-muted-foreground mt-2">
              Manage tasks and collaborate with your team
            </p>
          </div>
        </div>

        {hasProjectPermission(project.role, ProjectPermission.UPDATE_PROJECT) && (
          <DropdownMenu>
            <DropdownMenuTrigger asChild>
              <Button variant="outline" size="icon">
                <MoreVertical className="h-4 w-4" />
              </Button>
            </DropdownMenuTrigger>
            <DropdownMenuContent align="end">
              <DropdownMenuItem onClick={() => setEditingProject(project)}>
                <Edit2 className="h-4 w-4 mr-2" />
                Edit Project
              </DropdownMenuItem>
              {hasProjectPermission(project.role, ProjectPermission.VIEW_MEMBERS) && (
                <DropdownMenuItem onClick={() => setMembersProject(project)}>
                  <Users className="h-4 w-4 mr-2" />
                  Manage Members
                </DropdownMenuItem>
              )}
              {hasProjectPermission(project.role, ProjectPermission.DELETE_PROJECT) && (
                <DropdownMenuItem
                  onClick={() => setDeletingProject(project)}
                  className="text-destructive focus:text-destructive"
                >
                  <Trash2 className="h-4 w-4 mr-2" />
                  Delete Project
                </DropdownMenuItem>
              )}
            </DropdownMenuContent>
          </DropdownMenu>
        )}
      </div>

      {/* Stats */}
      <div className="grid gap-4 md:grid-cols-3">
        <Card>
          <CardHeader className="pb-2">
            <CardTitle className="text-sm font-medium text-muted-foreground">
              Sub-projects
            </CardTitle>
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold">{subprojects.length}</div>
          </CardContent>
        </Card>
        <Card>
          <CardHeader className="pb-2">
            <CardTitle className="text-sm font-medium text-muted-foreground">
              Role
            </CardTitle>
          </CardHeader>
          <CardContent>
            <Badge>{project.role}</Badge>
          </CardContent>
        </Card>
        <Card>
          <CardHeader className="pb-2">
            <CardTitle className="text-sm font-medium text-muted-foreground">
              Status
            </CardTitle>
          </CardHeader>
          <CardContent>
            <Badge variant={project.isArchived ? "secondary" : "outline"}>
              {project.isArchived ? "Archived" : "Active"}
            </Badge>
          </CardContent>
        </Card>
      </div>

      {/* Sub-projects Section */}
      <Card>
        <CardHeader>
          <div className="flex items-center justify-between">
            <div>
              <CardTitle>Sub-projects</CardTitle>
              <CardDescription>
                Create and manage sub-projects within this project
              </CardDescription>
            </div>
            {hasProjectPermission(project.role, ProjectPermission.CREATE_SUBPROJECT) && (
              <Button
                onClick={() => setOpenCreateDialog(true)}
                className="gap-2"
              >
                <FolderPlus className="h-4 w-4" />
                New Sub-project
              </Button>
            )}
          </div>
        </CardHeader>
        <CardContent>
          {subprojects.length === 0 ? (
            <div className="text-center py-8 text-muted-foreground">
              <FolderPlus className="h-12 w-12 mx-auto mb-3 opacity-50" />
              <p className="font-medium">No sub-projects yet</p>
              <p className="text-sm">
                Create a sub-project to break down your work
              </p>
            </div>
          ) : (
            <div className="space-y-3">
              {subprojects.map((subproject) => (
                <div
                  key={subproject.id}
                  onClick={() => router.push(`./${subproject.id}`)}
                  className="flex items-center justify-between p-4 rounded-lg border hover:bg-muted/50 cursor-pointer transition-colors"
                >
                  <div className="flex items-center gap-3 flex-1">
                    <div
                      className="h-3 w-3 rounded-full"
                      style={{ backgroundColor: subproject.color || "#808080" }}
                    />
                    <div className="flex-1">
                      <p className="font-medium">{subproject.name}</p>
                    </div>
                    <Badge variant="outline">{subproject.role}</Badge>
                  </div>
                </div>
              ))}
            </div>
          )}
        </CardContent>
      </Card>

      {/* Tasks Section */}
      <Card>
        <CardHeader>
          <CardTitle>Tasks</CardTitle>
          <CardDescription>
            Tasks will appear here (feature coming soon)
          </CardDescription>
        </CardHeader>
        <CardContent>
          <div className="text-center py-8 text-muted-foreground">
            <p className="font-medium">No tasks yet</p>
            <p className="text-sm">Start creating tasks to organize your work</p>
          </div>
        </CardContent>
      </Card>

      {/* Dialogs */}
      <CreateProjectDialog
        open={openCreateDialog}
        onOpenChange={setOpenCreateDialog}
        parentId={projectId}
      />

      {editingProject && (
        <EditProjectDialog
          project={editingProject}
          open={!!editingProject}
          onOpenChange={(open) => !open && setEditingProject(null)}
        />
      )}

      {deletingProject && (
        <DeleteProjectDialog
          project={deletingProject}
          open={!!deletingProject}
          onOpenChange={(open) => !open && setDeletingProject(null)}
        />
      )}

      {membersProject && (
        <ProjectMembersDialog
          project={membersProject}
          open={!!membersProject}
          onOpenChange={(open) => !open && setMembersProject(null)}
        />
      )}
    </div>
  )
}
