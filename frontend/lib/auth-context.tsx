"use client"

import {
  createContext,
  useContext,
  useEffect,
  useState,
  useCallback,
  type ReactNode,
} from "react"
import { apiClient } from "./api"
import type { User, Workspace, AuthState } from "./types"

interface AuthContextType extends AuthState {
  login: () => void
  logout: () => Promise<void>
  setCurrentWorkspace: (workspace: Workspace) => void
  refreshWorkspaces: () => Promise<void>
  handleAuthCallback: (code: string) => Promise<{ redirectUrl: string }>
}

const AuthContext = createContext<AuthContextType | null>(null)

export function AuthProvider({ children }: { children: ReactNode }) {
  const [state, setState] = useState<AuthState>({
    user: null,
    workspaces: [],
    currentWorkspace: null,
    isLoading: true,
    isAuthenticated: false,
  })

  const refreshUser = useCallback(async () => {
    try {
      const [user, workspaces] = await Promise.all([
        apiClient.getCurrentUser(),
        apiClient.getUserWorkspaces(),
      ])
      setState((prev) => ({
        ...prev,
        user,
        workspaces,
        isAuthenticated: true,
        isLoading: false,
      }))
    } catch {
      setState((prev) => ({
        ...prev,
        user: null,
        workspaces: [],
        isAuthenticated: false,
        isLoading: false,
      }))
    }
  }, [])

  useEffect(() => {
    if (apiClient.isAuthenticated()) {
      refreshUser()
    } else {
      setState((prev) => ({ ...prev, isLoading: false }))
    }
  }, [refreshUser])

  const login = useCallback(() => {
    window.location.href = apiClient.getGoogleAuthUrl()
  }, [])

  const logout = useCallback(async () => {
    await apiClient.logout()
    setState({
      user: null,
      workspaces: [],
      currentWorkspace: null,
      isLoading: false,
      isAuthenticated: false,
    })
  }, [])

  const setCurrentWorkspace = useCallback((workspace: Workspace) => {
    setState((prev) => ({ ...prev, currentWorkspace: workspace }))
    if (typeof window !== "undefined") {
      localStorage.setItem("currentWorkspaceId", workspace.id)
    }
  }, [])

  const refreshWorkspaces = useCallback(async () => {
    const workspaces = await apiClient.getUserWorkspaces()
    setState((prev) => ({ ...prev, workspaces }))
  }, [])

  const handleAuthCallback = useCallback(async (code: string) => {
    const response = await apiClient.handleOAuthCallback(code)
    setState((prev) => ({
      ...prev,
      user: response.user,
      workspaces: response.workspaces,
      isAuthenticated: true,
      isLoading: false,
    }))
    return { redirectUrl: response.redirectUrl }
  }, [])

  return (
    <AuthContext.Provider
      value={{
        ...state,
        login,
        logout,
        setCurrentWorkspace,
        refreshWorkspaces,
        handleAuthCallback,
      }}
    >
      {children}
    </AuthContext.Provider>
  )
}

export function useAuth() {
  const context = useContext(AuthContext)
  if (!context) {
    throw new Error("useAuth must be used within an AuthProvider")
  }
  return context
}
