"use client";

import {
  createContext,
  useContext,
  useEffect,
  useState,
  useCallback,
  type ReactNode,
} from "react";
import { apiClient } from "./api";
import type { User, Workspace, AuthState } from "./types";
import { useRouter } from "next/navigation";

interface AuthContextType extends AuthState {
  login: () => void;
  logout: () => Promise<void>;
  setCurrentWorkspace: (workspace: Workspace) => void;
  refreshWorkspaces: () => Promise<Workspace[]>;
  handleAuthCallback: (code: string) => Promise<{ redirectUrl: string }>;
}

const AuthContext = createContext<AuthContextType | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const router = useRouter();
  const [state, setState] = useState<AuthState>({
    user: null,
    workspaces: [],
    currentWorkspace: null,
    currentRole: null,
    isLoading: true,
    isAuthenticated: false,
  });

  const refreshUser = useCallback(async () => {
    try {
      const [user, workspaces] = await Promise.all([
        apiClient.getCurrentUser(),
        apiClient.getUserWorkspaces(),
      ]);
      
      // Restore current workspace from localStorage if available
      let currentWorkspace = null;
      let currentRole = null;
      if (typeof window !== "undefined") {
        const savedWorkspaceId = localStorage.getItem("currentWorkspaceId");
        if (savedWorkspaceId) {
          currentWorkspace = workspaces.find((w) => w.id === savedWorkspaceId) || null;
          currentRole = currentWorkspace?.role || null;
        }
      }
      
      setState((prev) => ({
        ...prev,
        user,
        workspaces,
        currentWorkspace,
        currentRole,
        isAuthenticated: true,
        isLoading: false,
      }));
    } catch {
      setState((prev) => ({
        ...prev,
        user: null,
        workspaces: [],
        currentWorkspace: null,
        currentRole: null,
        isAuthenticated: false,
        isLoading: false,
      }));
    }
  }, []);

  useEffect(() => {
    if (apiClient.isAuthenticated()) {
      refreshUser();
    } else {
      setState((prev) => ({ ...prev, isLoading: false }));
    }
  }, [refreshUser]);

  const login = useCallback(() => {
    window.location.href = apiClient.getGoogleAuthUrl();
  }, []);

  const logout = useCallback(async () => {
    await apiClient.logout();
    setState({
      user: null,
      workspaces: [],
      currentWorkspace: null,
      currentRole: null,
      isLoading: false,
      isAuthenticated: false,
    });
    router.replace("/");
  }, []);

  const setCurrentWorkspace = useCallback((workspace: Workspace) => {
    setState((prev) => ({ 
      ...prev, 
      currentWorkspace: workspace,
      currentRole: workspace.role || null,
    }));
    if (typeof window !== "undefined") {
      localStorage.setItem("currentWorkspaceId", workspace.id);
    }
  }, []);

  const refreshWorkspaces = useCallback(async () => {
    const workspaces = await apiClient.getUserWorkspaces();
    
    // Update currentRole if currentWorkspace still exists
    setState((prev) => {
      const currentWorkspace = prev.currentWorkspace
        ? workspaces.find((w) => w.id === prev.currentWorkspace?.id) || null
        : null;
      
      return {
        ...prev,
        workspaces,
        currentWorkspace,
        currentRole: currentWorkspace?.role || null,
      };
    });
    
    return workspaces;
  }, []);

  const handleAuthCallback = useCallback(async (code: string) => {
    const response = await apiClient.handleOAuthCallback(code);
    
    // Set first workspace as current if available
    const firstWorkspace = response.workspaces[0] || null;
    const currentRole = firstWorkspace?.role || null;
    
    if (firstWorkspace && typeof window !== "undefined") {
      localStorage.setItem("currentWorkspaceId", firstWorkspace.id);
    }
    
    setState((prev) => ({
      ...prev,
      user: response.user,
      workspaces: response.workspaces,
      currentWorkspace: firstWorkspace,
      currentRole,
      isAuthenticated: true,
      isLoading: false,
    }));
    return { redirectUrl: response.redirectUrl };
  }, []);

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
  );
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error("useAuth must be used within an AuthProvider");
  }
  return context;
}
