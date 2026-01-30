"use client"

import { useEffect } from "react"
import { useRouter } from "next/navigation"
import { useAuth } from "@/lib/auth-context"
import { Loader2 } from "lucide-react"
import { redirectToRoot } from "@/lib/utils"

export default function HomePage() {
  const router = useRouter()
  const { isAuthenticated, isLoading, workspaces } = useAuth()

  useEffect(() => {
    if (!isLoading) {
      if (!isAuthenticated) {
        redirectToRoot("/login");
      } else if (workspaces.length === 0) {
        router.push("/welcome/new-workspace")
      } else if (workspaces.length === 1) {
        router.push("/dashboard")
      } else {
        router.push("/select-workspace")
      }
    }
  }, [isAuthenticated, isLoading, workspaces, router])

  return (
    <div className="min-h-screen flex items-center justify-center bg-background">
      <div className="text-center space-y-4">
        <Loader2 className="h-8 w-8 animate-spin mx-auto text-muted-foreground" />
        <p className="text-muted-foreground">Loading...</p>
      </div>
    </div>
  )
}
