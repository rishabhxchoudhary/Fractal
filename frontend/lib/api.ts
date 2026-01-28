import type {
  LoginResponse,
  CreateWorkspaceRequest,
  CreateWorkspaceResponse,
  User,
  Workspace,
} from "./types";
import Cookies from "js-cookie";
import { getCookieDomain } from "./utils";

const API_BASE_URL = process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080";

class ApiClient {
  private accessToken: string | null = null;

  setAccessToken(token: string | null) {
    this.accessToken = token;
    if (token) {
      Cookies.set("accessToken", token, {
        expires: 7,
        domain: getCookieDomain(),
        sameSite: "Lax",
      });
    } else {
      this.logout();
    }
  }

  getAccessToken(): string | null {
    if (this.accessToken) return this.accessToken;
    const token = Cookies.get("accessToken");
    if (token) {
      this.accessToken = token;
      return token;
    }
    return null;
  }
  private async fetch<T>(
    endpoint: string,
    options: RequestInit = {},
  ): Promise<T> {
    const token = this.getAccessToken();
    const headers: HeadersInit = {
      "Content-Type": "application/json",
      ...options.headers,
    };

    if (token) {
      (headers as Record<string, string>)["Authorization"] = `Bearer ${token}`;
    }

    const response = await fetch(`${API_BASE_URL}${endpoint}`, {
      ...options,
      headers,
      credentials: "omit",
    });

    if (!response.ok) {
      const error = await response.json().catch(() => ({}));
      throw new Error(error.message || `API Error: ${response.status}`);
    }

    return response.json();
  }

  // Auth endpoints
  getGoogleAuthUrl(): string {
    const redirectUri =
      typeof window !== "undefined"
        ? `${window.location.origin}/auth/callback`
        : "";
    return `${API_BASE_URL}/oauth2/authorization/google?redirect_uri=${encodeURIComponent(redirectUri)}`;
  }

  async handleOAuthCallback(code: string): Promise<LoginResponse> {
    const response = await this.fetch<LoginResponse>(
      "/api/auth/oauth/callback",
      {
        method: "POST",
        body: JSON.stringify({ code }),
      },
    );
    this.setAccessToken(response.accessToken);
    return response;
  }

  async getCurrentUser(): Promise<User> {
    return this.fetch<User>("/api/auth/me");
  }

  async getUserWorkspaces(): Promise<Workspace[]> {
    return this.fetch<Workspace[]>("/api/workspaces");
  }

  async createWorkspace(
    data: CreateWorkspaceRequest,
  ): Promise<CreateWorkspaceResponse> {
    return this.fetch<CreateWorkspaceResponse>("/api/workspaces", {
      method: "POST",
      body: JSON.stringify(data),
    });
  }

  logout() {
    this.accessToken = null;
    Cookies.remove("accessToken", { domain: getCookieDomain() });
    Cookies.remove("accessToken"); // Fallback cleanup
  }

  // Check if user is authenticated
  isAuthenticated(): boolean {
    return !!this.getAccessToken();
  }
}

export const apiClient = new ApiClient();
