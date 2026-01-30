"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { useAuth } from "@/lib/auth-context";
import { getSubdomain } from "@/lib/utils";

export default function DomainLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  const router = useRouter();
  const {
    isAuthenticated,
    isLoading,
    workspaces,
    currentWorkspace,
    setCurrentWorkspace,
  } = useAuth();
  const [isWorkspaceResolved, setIsWorkspaceResolved] = useState(false);

  useEffect(() => {
    if (isLoading) return;

    if (!isAuthenticated) {
      const rootDomain =
        process.env.NEXT_PUBLIC_ROOT_DOMAIN || "localhost:3000";
      const protocol = window.location.protocol;
      window.location.href = `${protocol}//${rootDomain}/login`;
      return;
    }

    // Get the subdomain from the current hostname
    const hostname = window.location.hostname;
    const subdomain = getSubdomain(hostname);

    if (subdomain) {
      // Find the workspace that matches this subdomain
      const matchingWorkspace = workspaces.find((w) => w.slug === subdomain);

      if (matchingWorkspace) {
        // Set it as active if it's not already
        if (currentWorkspace?.id !== matchingWorkspace.id) {
          setCurrentWorkspace(matchingWorkspace);
        }
        setIsWorkspaceResolved(true);
      } else {
        // User does not have access to this specific subdomain workspace
        console.warn(`User does not have access to workspace: ${subdomain}`);
        const rootDomain =
          process.env.NEXT_PUBLIC_ROOT_DOMAIN || "localhost:3000";
        const protocol = window.location.protocol;
        window.location.href = `${protocol}//${rootDomain}/select-workspace`;
      }
    } else {
      // No subdomain found
      console.log("no subdomain found");
      const rootDomain =
        process.env.NEXT_PUBLIC_ROOT_DOMAIN || "localhost:3000";
      const protocol = window.location.protocol;
      window.location.href = `${protocol}//${rootDomain}/select-workspace`;
    }
  }, [
    isAuthenticated,
    isLoading,
    workspaces,
    currentWorkspace,
    setCurrentWorkspace,
    router,
  ]);

  if (!isAuthenticated) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-background">
        <div className="flex flex-col items-center gap-2">
          <div className="animate-pulse text-muted-foreground">
            Authenticating...
          </div>
        </div>
      </div>
    );
  }

  if (isLoading || !isWorkspaceResolved) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-background">
        <div className="flex flex-col items-center gap-2">
          <div className="animate-pulse text-muted-foreground">
            Loading Workspace...
          </div>
        </div>
      </div>
    );
  }

  return children;
}
