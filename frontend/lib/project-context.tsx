"use client";

import {
  createContext,
  useContext,
  useCallback,
  type ReactNode,
  useState,
} from "react";
import { apiClient } from "./api";
import type { Project, CreateProjectRequest } from "./types";

interface ProjectContextType {
  projects: Project[];
  currentProject: Project | null;
  isLoading: boolean;
  error: string | null;
  
  // Actions
  setCurrentProject: (project: Project | null) => void;
  refreshProjects: (workspaceId: string) => Promise<void>;
  createProject: (workspaceId: string, data: CreateProjectRequest) => Promise<Project>;
  updateProject: (projectId: string, data: Partial<CreateProjectRequest>) => Promise<Project>;
  deleteProject: (projectId: string) => Promise<void>;
}

const ProjectContext = createContext<ProjectContextType | null>(null);

export function ProjectProvider({ children }: { children: ReactNode }) {
  const [projects, setProjects] = useState<Project[]>([]);
  const [currentProject, setCurrentProject] = useState<Project | null>(null);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const refreshProjects = useCallback(async (workspaceId: string) => {
    setIsLoading(true);
    setError(null);
    try {
      const data = await apiClient.getProjects(workspaceId);
      setProjects(data || []);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to load projects");
      setProjects([]);
    } finally {
      setIsLoading(false);
    }
  }, []);

  const createProjectAction = useCallback(
    async (workspaceId: string, data: CreateProjectRequest): Promise<Project> => {
      setIsLoading(true);
      setError(null);
      try {
        const newProject = await apiClient.createProject(workspaceId, data);
        setProjects((prev) => [...prev, newProject]);
        return newProject;
      } catch (err) {
        const message = err instanceof Error ? err.message : "Failed to create project";
        setError(message);
        throw err;
      } finally {
        setIsLoading(false);
      }
    },
    []
  );

  const updateProjectAction = useCallback(
    async (projectId: string, data: Partial<CreateProjectRequest>): Promise<Project> => {
      setIsLoading(true);
      setError(null);
      try {
        const updated = await apiClient.updateProject(projectId, data as any);
        setProjects((prev) =>
          prev.map((p) => (p.id === projectId ? { ...p, ...updated } : p))
        );
        if (currentProject?.id === projectId) {
          setCurrentProject({ ...currentProject, ...updated });
        }
        return updated;
      } catch (err) {
        const message = err instanceof Error ? err.message : "Failed to update project";
        setError(message);
        throw err;
      } finally {
        setIsLoading(false);
      }
    },
    [currentProject]
  );

  const deleteProjectAction = useCallback(
    async (projectId: string) => {
      setIsLoading(true);
      setError(null);
      try {
        await apiClient.deleteProject(projectId);
        setProjects((prev) => prev.filter((p) => p.id !== projectId));
        if (currentProject?.id === projectId) {
          setCurrentProject(null);
        }
      } catch (err) {
        const message = err instanceof Error ? err.message : "Failed to delete project";
        setError(message);
        throw err;
      } finally {
        setIsLoading(false);
      }
    },
    [currentProject]
  );

  const value: ProjectContextType = {
    projects,
    currentProject,
    isLoading,
    error,
    setCurrentProject,
    refreshProjects,
    createProject: createProjectAction,
    updateProject: updateProjectAction,
    deleteProject: deleteProjectAction,
  };

  return (
    <ProjectContext.Provider value={value}>
      {children}
    </ProjectContext.Provider>
  );
}

export function useProjects() {
  const context = useContext(ProjectContext);
  if (!context) {
    throw new Error("useProjects must be used within a ProjectProvider");
  }
  return context;
}
