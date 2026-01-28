"use client"

import { useEffect } from "react"
import { useRouter } from "next/navigation"
import { useAuth } from "@/lib/auth-context"
import { LoginForm } from "@/components/auth/login-form"

export default function LoginPage() {
  const { isAuthenticated, isLoading, workspaces } = useAuth()
  const router = useRouter()

  useEffect(() => {
    if (!isLoading && isAuthenticated) {
      if (workspaces.length === 0) {
        router.push("/welcome/new-workspace")
      } else if (workspaces.length === 1) {
        router.push(`/dashboard`)
      } else {
        router.push("/select-workspace")
      }
    }
  }, [isAuthenticated, isLoading, workspaces, router])

  if (isLoading) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-background">
        <div className="animate-pulse text-muted-foreground">Loading...</div>
      </div>
    )
  }

  return <LoginForm />
}
