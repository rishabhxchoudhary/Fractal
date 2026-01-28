"use client"

import { useEffect } from "react"
import { useRouter } from "next/navigation"
import { useAuth } from "@/lib/auth-context"
import { DashboardLayout } from "@/components/dashboard/dashboard-layout"
import { DashboardContent } from "@/components/dashboard/dashboard-content"

export default function DashboardPage() {
  const router = useRouter()
  const { isAuthenticated, isLoading, workspaces, currentWorkspace, setCurrentWorkspace } = useAuth()

  useEffect(() => {
    if (!isLoading) {
      if (!isAuthenticated) {
        router.push("/login")
        return
      }
      
      if (workspaces.length === 0) {
        router.push("/welcome/new-workspace")
        return
      }

      // If no workspace is selected, select the first one or redirect to selector
      if (!currentWorkspace) {
        if (workspaces.length === 1) {
          setCurrentWorkspace(workspaces[0])
        } else {
          router.push("/select-workspace")
        }
      }
    }
  }, [isAuthenticated, isLoading, workspaces, currentWorkspace, setCurrentWorkspace, router])

  if (isLoading) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-background">
        <div className="animate-pulse text-muted-foreground">Loading...</div>
      </div>
    )
  }

  if (!isAuthenticated || !currentWorkspace) {
    return null
  }

  return (
    <DashboardLayout>
      <DashboardContent />
    </DashboardLayout>
  )
}
