"use client"

import { useEffect, useState } from "react"
import { useAuth } from "@/lib/auth-context"
import { useProjects } from "@/lib/project-context"
import { hasProjectPermission, ProjectPermission } from "@/lib/permissions"
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import { Button } from "@/components/ui/button"
import { Badge } from "@/components/ui/badge"
import { Input } from "@/components/ui/input"
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu"
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog"
import { Skeleton } from "@/components/ui/skeleton"
import {
  FolderPlus,
  MoreVertical,
  Edit2,
  Trash2,
  Users,
  ChevronRight,
  Search,
} from "lucide-react"
import { CreateProjectDialog } from "@/components/projects/create-project-dialog"
import { EditProjectDialog } from "@/components/projects/edit-project-dialog"
import { DeleteProjectDialog } from "@/components/projects/delete-project-dialog"
import { ProjectMembersDialog } from "@/components/projects/project-members-dialog"
import type { Project } from "@/lib/types"
import { useRouter } from "next/navigation"

export default function ProjectsPage() {
  const router = useRouter()
  const { currentWorkspace } = useAuth()
  const { projects, isLoading, refreshProjects, setCurrentProject } = useProjects()
  const [searchQuery, setSearchQuery] = useState("")
  const [openCreateDialog, setOpenCreateDialog] = useState(false)
  const [editingProject, setEditingProject] = useState<Project | null>(null)
  const [deletingProject, setDeletingProject] = useState<Project | null>(null)
  const [membersProject, setMembersProject] = useState<Project | null>(null)

  useEffect(() => {
    if (currentWorkspace?.id) {
      refreshProjects(currentWorkspace.id)
    }
  }, [currentWorkspace?.id, refreshProjects])

  const filteredProjects = projects.filter((p) =>
    p.name.toLowerCase().includes(searchQuery.toLowerCase())
  )

  const rootProjects = filteredProjects.filter((p) => !p.parentId)

  const handleProjectClick = (project: Project) => {
    setCurrentProject(project)
    router.push(`./${project.id}`)
  }

  return (
    <div className="p-6 space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold tracking-tight">Projects</h1>
          <p className="text-muted-foreground mt-1">
            Manage and organize your projects in {currentWorkspace?.name}
          </p>
        </div>
        <Button
          onClick={() => setOpenCreateDialog(true)}
          className="gap-2"
        >
          <FolderPlus className="h-4 w-4" />
          New Project
        </Button>
      </div>

      {/* Search */}
      <div className="relative">
        <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
        <Input
          placeholder="Search projects..."
          value={searchQuery}
          onChange={(e) => setSearchQuery(e.target.value)}
          className="pl-10"
        />
      </div>

      {/* Projects Grid */}
      {isLoading ? (
        <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-3">
          {[...Array(3)].map((_, i) => (
            <Card key={i}>
              <CardHeader>
                <Skeleton className="h-6 w-24" />
              </CardHeader>
              <CardContent>
                <Skeleton className="h-4 w-32" />
              </CardContent>
            </Card>
          ))}
        </div>
      ) : rootProjects.length === 0 ? (
        <Card className="border-dashed">
          <CardContent className="flex flex-col items-center justify-center py-12">
            <FolderPlus className="h-12 w-12 text-muted-foreground/50 mb-4" />
            <h3 className="text-lg font-semibold">No projects yet</h3>
            <p className="text-muted-foreground text-sm mb-4">
              Create your first project to get started
            </p>
            <Button onClick={() => setOpenCreateDialog(true)}>
              Create Project
            </Button>
          </CardContent>
        </Card>
      ) : (
        <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-3">
          {rootProjects.map((project) => (
            <Card
              key={project.id}
              className="hover:shadow-md transition-shadow cursor-pointer group"
            >
              <CardHeader className="pb-3">
                <div className="flex items-start justify-between">
                  <div className="flex-1">
                    <div className="flex items-center gap-2 mb-1">
                      <div
                        className="h-3 w-3 rounded-full"
                        style={{ backgroundColor: project.color || "#808080" }}
                      />
                      <CardTitle className="text-lg line-clamp-1">
                        {project.name}
                      </CardTitle>
                    </div>
                    <Badge variant="secondary" className="text-xs">
                      {project.role}
                    </Badge>
                  </div>
                  <DropdownMenu>
                    <DropdownMenuTrigger asChild>
                      <Button
                        variant="ghost"
                        size="icon"
                        className="h-8 w-8 opacity-0 group-hover:opacity-100"
                      >
                        <MoreVertical className="h-4 w-4" />
                      </Button>
                    </DropdownMenuTrigger>
                    <DropdownMenuContent align="end">
                      <DropdownMenuItem onClick={() => handleProjectClick(project)}>
                        <ChevronRight className="h-4 w-4 mr-2" />
                        Open
                      </DropdownMenuItem>
                      {hasProjectPermission(project.role, ProjectPermission.UPDATE_PROJECT) && (
                        <>
                          <DropdownMenuItem onClick={() => setEditingProject(project)}>
                            <Edit2 className="h-4 w-4 mr-2" />
                            Edit
                          </DropdownMenuItem>
                          <DropdownMenuItem onClick={() => setMembersProject(project)}>
                            <Users className="h-4 w-4 mr-2" />
                            Members
                          </DropdownMenuItem>
                          {hasProjectPermission(project.role, ProjectPermission.DELETE_PROJECT) && (
                            <DropdownMenuItem
                              onClick={() => setDeletingProject(project)}
                              className="text-destructive focus:text-destructive"
                            >
                              <Trash2 className="h-4 w-4 mr-2" />
                              Delete
                            </DropdownMenuItem>
                          )}
                        </>
                      )}
                      {hasProjectPermission(project.role, ProjectPermission.VIEW_MEMBERS) && (
                        <DropdownMenuItem onClick={() => setMembersProject(project)}>
                          <Users className="h-4 w-4 mr-2" />
                          View Members
                        </DropdownMenuItem>
                      )}
                    </DropdownMenuContent>
                  </DropdownMenu>
                </div>
              </CardHeader>
              <CardContent>
                <p className="text-sm text-muted-foreground line-clamp-2">
                  Click to open project and view tasks
                </p>
              </CardContent>
            </Card>
          ))}
        </div>
      )}

      {/* Dialogs */}
      <CreateProjectDialog
        open={openCreateDialog}
        onOpenChange={setOpenCreateDialog}
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
