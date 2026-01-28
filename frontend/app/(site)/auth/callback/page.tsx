"use client";

import { useEffect, useRef, Suspense } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import { apiClient } from "@/lib/api";
import { Loader2 } from "lucide-react";
import { useAuth } from "@/lib/auth-context";

function AuthCallbackContent() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const processedRef = useRef(false);
  const { refreshWorkspaces } = useAuth();

  useEffect(() => {
    const processLogin = async () => {
      // Prevent double execution in React Strict Mode
      if (processedRef.current) return;
      processedRef.current = true;

      // 1. Check for the token we sent from the backend
      const token = searchParams.get("token");

      if (!token) {
        // If we still have the old "error" param or no token
        const error = searchParams.get("error");
        console.error("Auth Error:", error || "No token found");
        router.push("/login?error=" + (error || "no_token"));
        return;
      }

      try {
        // 2. Set the token in our API client (saves to localStorage)
        apiClient.setAccessToken(token);

        // 3. Force a reload of the user state or just redirect
        // We will fetch the user data to decide routing
        // (Note: This call will fail until we do Step 2 below, but that's okay for now)
        try {
          const workspaces = await refreshWorkspaces();
          if (workspaces.length === 0) {
            router.replace("/welcome/new-workspace");
          } else {
            router.replace("/select-workspace");
          }
        } catch (e) {
          // If fetching fails, we default to dashboard or new-workspace
          // This allows us to proceed even if the /me endpoint isn't ready
          console.warn("Could not fetch workspaces, redirecting to default", e);
          router.replace("/select-workspace");
        }
      } catch (error) {
        console.error("Failed to process login", error);
        router.push("/login?error=auth_failed");
      }
    };

    processLogin();
  }, [router, searchParams]);

  return (
    <div className="flex h-screen w-full items-center justify-center bg-background">
      <div className="flex flex-col items-center gap-4">
        <Loader2 className="h-8 w-8 animate-spin text-primary" />
        <p className="text-muted-foreground">Authenticating...</p>
      </div>
    </div>
  );
}

export default function AuthCallbackPage() {
  return (
    <Suspense fallback={<div>Loading...</div>}>
      <AuthCallbackContent />
    </Suspense>
  );
}
