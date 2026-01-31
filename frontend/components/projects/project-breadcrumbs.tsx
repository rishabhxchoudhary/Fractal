"use client";

import { useProjects } from "@/lib/project-context";
import { useAuth } from "@/lib/auth-context";
import { Button } from "@/components/ui/button";
import {
  Breadcrumb,
  BreadcrumbItem,
  BreadcrumbLink,
  BreadcrumbList,
  BreadcrumbPage,
  BreadcrumbSeparator,
} from "@/components/ui/breadcrumb";
import { ChevronRight, Home } from "lucide-react";

interface ProjectBreadcrumbsProps {
  onNavigate?: (projectId: string | null) => void;
}

export function ProjectBreadcrumbs({ onNavigate }: ProjectBreadcrumbsProps) {
  const { currentProject, projects } = useProjects();
  const { currentWorkspace } = useAuth();

  // Build breadcrumb path by finding ancestors
  const getBreadcrumbPath = () => {
    if (!currentProject) return [];

    const path: Array<{ id: string; name: string }> = [];
    let current = currentProject;

    // Add current project
    path.unshift({ id: current.id, name: current.name });

    // Find ancestors
    while (current.parentId) {
      const parent = projects.find((p) => p.id === current.parentId);
      if (!parent) break;
      path.unshift({ id: parent.id, name: parent.name });
      current = parent;
    }

    return path;
  };

  const breadcrumbPath = getBreadcrumbPath();

  if (!currentProject) {
    return null;
  }

  return (
    <div className="flex items-center gap-2 px-4 py-3 border-b bg-muted/30">
      <Button
        variant="ghost"
        size="sm"
        className="gap-2 h-8"
        onClick={() => onNavigate?.(null)}
      >
        <Home className="h-4 w-4" />
        <span className="text-sm">Dashboard</span>
      </Button>

      {breadcrumbPath.length > 0 && <ChevronRight className="h-4 w-4 text-muted-foreground" />}

      <Breadcrumb>
        <BreadcrumbList>
          {breadcrumbPath.map((item, index) => (
            <BreadcrumbItem key={item.id}>
              {index === breadcrumbPath.length - 1 ? (
                <BreadcrumbPage className="text-sm font-medium">
                  {item.name}
                </BreadcrumbPage>
              ) : (
                <>
                  <BreadcrumbLink asChild>
                    <Button
                      variant="ghost"
                      size="sm"
                      className="h-6 px-1 text-sm"
                      onClick={() => onNavigate?.(item.id)}
                    >
                      {item.name}
                    </Button>
                  </BreadcrumbLink>
                  <BreadcrumbSeparator className="mx-1" />
                </>
              )}
            </BreadcrumbItem>
          ))}
        </BreadcrumbList>
      </Breadcrumb>
    </div>
  );
}
