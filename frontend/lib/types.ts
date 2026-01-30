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
  role: "OWNER" | "ADMIN" | "MEMBER" | "VIEWER"
  createdAt?: string
}

export interface WorkspaceMember {
  id: string
  email: string
  fullName: string
  avatarUrl?: string
  role: "OWNER" | "ADMIN" | "MEMBER" | "VIEWER"
  joinedAt: string
}

export interface AuthState {
  user: User | null
  workspaces: Workspace[]
  currentWorkspace: Workspace | null
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
