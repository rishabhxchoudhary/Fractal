"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { useAuth } from "@/lib/auth-context";
import { DashboardLayout } from "@/components/dashboard/dashboard-layout";
import { DashboardContent } from "@/components/dashboard/dashboard-content";
import { getSubdomain } from "@/lib/utils"; // Make sure you import this

export default function DashboardPage() {
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

    // --- NEW LOGIC START ---

    // 1. Get the subdomain from the current hostname
    // We can rely on window.location.hostname in the client
    const hostname = window.location.hostname;
    console.log("hostname", hostname);
    const subdomain = getSubdomain(hostname);
    console.log("subdomain", subdomain);
    if (subdomain) {
      console.log(subdomain);
      // 2. Find the workspace that matches this subdomain
      const matchingWorkspace = workspaces.find((w) => w.slug === subdomain);

      if (matchingWorkspace) {
        // 3. Set it as active if it's not already
        if (currentWorkspace?.id !== matchingWorkspace.id) {
          setCurrentWorkspace(matchingWorkspace);
        }
        setIsWorkspaceResolved(true);
      } else {
        // 4. If user has no access to this specific subdomain workspace -> 404 or redirect
        // For now, let's send them back to selection
        console.warn(`User does not have access to workspace: ${subdomain}`);
        return;
        router.push("/select-workspace"); // This will get rewritten to the root domain by middleware if configured, or just go to root
      }
    } else {
      // If we are somehow on the dashboard without a subdomain (should be impossible via middleware),
      console.log("no subdomain found");
      return;
      // send to select-workspace

      router.push("/select-workspace");
    }
    // --- NEW LOGIC END ---
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

  return (
    <DashboardLayout>
      <DashboardContent />
    </DashboardLayout>
  );
}
