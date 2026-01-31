"use client";

import { useState, useEffect } from "react";
import { useProjects } from "@/lib/project-context";
import { useAuth } from "@/lib/auth-context";
import { Button } from "@/components/ui/button";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from "@/components/ui/dialog";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import {
  ChevronDown,
  ChevronRight,
  FolderPlus,
  MoreHorizontal,
  Trash2,
  Edit2,
  FolderOpen,
} from "lucide-react";
import type { Project, CreateProjectRequest } from "@/lib/types";
import { hasProjectPermission, ProjectPermission } from "@/lib/permissions";

interface ProjectSidebarProps {
  onProjectSelect?: (project: Project) => void;
}

export function ProjectSidebar({ onProjectSelect }: ProjectSidebarProps) {
  const { projects, currentProject, refreshProjects, createProject, deleteProject } = useProjects();
  const { currentWorkspace } = useAuth();
  const [expandedProjects, setExpandedProjects] = useState<Set<string>>(new Set());
  const [isCreateDialogOpen, setIsCreateDialogOpen] = useState(false);
  const [newProjectName, setNewProjectName] = useState("");
  const [selectedParentId, setSelectedParentId] = useState<string | null>(null);

  useEffect(() => {
    if (currentWorkspace?.id) {
      refreshProjects(currentWorkspace.id);
    }
  }, [currentWorkspace?.id, refreshProjects]);

  const toggleProjectExpanded = (projectId: string) => {
    setExpandedProjects((prev) => {
      const newSet = new Set(prev);
      if (newSet.has(projectId)) {
        newSet.delete(projectId);
      } else {
        newSet.add(projectId);
      }
      return newSet;
    });
  };

  const handleCreateProject = async () => {
    if (!currentWorkspace?.id || !newProjectName.trim()) return;

    try {
      const newProject = await createProject(currentWorkspace.id, {
        name: newProjectName,
        color: "#3b82f6",
        parentId: selectedParentId,
      });

      setNewProjectName("");
      setSelectedParentId(null);
      setIsCreateDialogOpen(false);

      if (onProjectSelect) {
        onProjectSelect(newProject);
      }
    } catch (error) {
      console.error("Failed to create project:", error);
    }
  };

  const handleDeleteProject = async (projectId: string) => {
    if (!confirm("Are you sure you want to delete this project and all its sub-projects?")) {
      return;
    }

    try {
      await deleteProject(projectId);
    } catch (error) {
      console.error("Failed to delete project:", error);
    }
  };

  const rootProjects = projects.filter((p) => !p.parentId);
  const getChildProjects = (parentId: string) =>
    projects.filter((p) => p.parentId === parentId);

  const renderProjectTree = (parentId: string | null = null, level = 0) => {
    const items = parentId ? getChildProjects(parentId) : rootProjects;

    return items.map((project) => {
      const children = getChildProjects(project.id);
      const hasChildren = children.length > 0;
      const isExpanded = expandedProjects.has(project.id);

      return (
        <div key={project.id}>
          <div
            className="flex items-center gap-1 px-2 py-1.5 hover:bg-accent rounded group"
            style={{ marginLeft: `${level * 16}px` }}
          >
            {hasChildren && (
              <Button
                variant="ghost"
                size="icon"
                className="h-6 w-6 p-0"
                onClick={() => toggleProjectExpanded(project.id)}
              >
                {isExpanded ? (
                  <ChevronDown className="h-4 w-4" />
                ) : (
                  <ChevronRight className="h-4 w-4" />
                )}
              </Button>
            )}
            {!hasChildren && <div className="w-6" />}

            <Button
              variant={currentProject?.id === project.id ? "secondary" : "ghost"}
              className="flex-1 justify-start gap-2 h-7"
              onClick={() => onProjectSelect?.(project)}
            >
              <FolderOpen className="h-4 w-4 flex-shrink-0" />
              <span className="truncate">{project.name}</span>
            </Button>

            {hasProjectPermission(project.role, ProjectPermission.UPDATE_PROJECT) && (
              <DropdownMenu>
                <DropdownMenuTrigger asChild>
                  <Button
                    variant="ghost"
                    size="icon"
                    className="h-6 w-6 p-0 opacity-0 group-hover:opacity-100"
                  >
                    <MoreHorizontal className="h-4 w-4" />
                  </Button>
                </DropdownMenuTrigger>
                <DropdownMenuContent align="end">
                  <DropdownMenuLabel className="text-xs">Project</DropdownMenuLabel>
                  {hasProjectPermission(project.role, ProjectPermission.CREATE_SUBPROJECT) && (
                    <DropdownMenuItem
                      onClick={() => {
                        setSelectedParentId(project.id);
                        setIsCreateDialogOpen(true);
                      }}
                    >
                      <FolderPlus className="h-4 w-4 mr-2" />
                      Create Sub-project
                    </DropdownMenuItem>
                  )}
                  <DropdownMenuSeparator />
                  {hasProjectPermission(project.role, ProjectPermission.DELETE_PROJECT) && (
                    <DropdownMenuItem
                      onClick={() => handleDeleteProject(project.id)}
                      className="text-destructive focus:text-destructive"
                    >
                      <Trash2 className="h-4 w-4 mr-2" />
                      Delete
                    </DropdownMenuItem>
                  )}
                </DropdownMenuContent>
              </DropdownMenu>
            )}
          </div>

          {hasChildren && isExpanded && renderProjectTree(project.id, level + 1)}
        </div>
      );
    });
  };

  return (
    <div className="flex flex-col h-full">
      <div className="p-3 border-b">
        <Dialog open={isCreateDialogOpen} onOpenChange={setIsCreateDialogOpen}>
          <DialogTrigger asChild>
            <Button variant="outline" className="w-full gap-2" size="sm">
              <FolderPlus className="h-4 w-4" />
              New Project
            </Button>
          </DialogTrigger>
          <DialogContent>
            <DialogHeader>
              <DialogTitle>
                {selectedParentId ? "Create Sub-project" : "Create Project"}
              </DialogTitle>
              <DialogDescription>
                Add a new {selectedParentId ? "sub-" : ""}project to your workspace
              </DialogDescription>
            </DialogHeader>
            <div className="space-y-4">
              <div>
                <Label htmlFor="project-name">Name</Label>
                <Input
                  id="project-name"
                  placeholder="Project name"
                  value={newProjectName}
                  onChange={(e) => setNewProjectName(e.target.value)}
                  onKeyPress={(e) => e.key === "Enter" && handleCreateProject()}
                  autoFocus
                />
              </div>
              <div className="flex gap-2">
                <Button
                  variant="outline"
                  onClick={() => {
                    setIsCreateDialogOpen(false);
                    setSelectedParentId(null);
                    setNewProjectName("");
                  }}
                >
                  Cancel
                </Button>
                <Button onClick={handleCreateProject} disabled={!newProjectName.trim()}>
                  Create
                </Button>
              </div>
            </div>
          </DialogContent>
        </Dialog>
      </div>

      <div className="flex-1 overflow-y-auto p-2">
        {projects.length === 0 ? (
          <div className="text-center py-8 text-muted-foreground">
            <p className="text-sm">No projects yet</p>
            <p className="text-xs mt-1">Create your first project to get started</p>
          </div>
        ) : (
          <div className="space-y-0.5">{renderProjectTree()}</div>
        )}
      </div>
    </div>
  );
}
