export interface User {
  id: string
  email: string
  name: string
  profilePicture?: string
}

export interface Workspace {
  id: string
  name: string
  slug: string
  role: "OWNER" | "ADMIN" | "MEMBER"
  createdAt?: string
}

export interface WorkspaceMember {
  id: string
  email: string
  fullName: string
  avatarUrl?: string
  role: "OWNER" | "ADMIN" | "MEMBER"
  joinedAt: string
}

export interface AuthState {
  user: User | null
  workspaces: Workspace[]
  currentWorkspace: Workspace | null
  currentRole: "OWNER" | "ADMIN" | "MEMBER" | null
  isLoading: boolean
  isAuthenticated: boolean
}

export interface LoginResponse {
  accessToken: string
  refreshToken: string
  user: User
  workspaces: Workspace[]
  redirectUrl: string
}

export interface CreateWorkspaceRequest {
  name: string
}

export interface CreateWorkspaceResponse {
  workspace: Workspace
  redirectUrl: string
}

// Project Types
export type ProjectRole = "OWNER" | "ADMIN" | "EDITOR" | "VIEWER";

export interface Project {
  id: string
  name: string
  color?: string
  parentId?: string | null
  role: ProjectRole
  isArchived?: boolean
}

export interface ProjectMember {
  id: string
  email: string
  fullName: string
  avatarUrl?: string
  role: ProjectRole
  joinedAt: string
}

export interface CreateProjectRequest {
  name: string
  color?: string
  parentId?: string | null
}

export interface ProjectResponse {
  id: string
  name: string
  color?: string
  parentId?: string | null
  role: ProjectRole
  isArchived?: boolean
}
