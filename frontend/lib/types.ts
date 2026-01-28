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
  createdAt: string
}

export interface WorkspaceMember {
  id: string
  userId: string
  workspaceId: string
  role: "OWNER" | "ADMIN" | "MEMBER"
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
