"use client"

import { useEffect, useState } from "react"
import { useRouter, useSearchParams } from "next/navigation"
import { useAuth } from "@/lib/auth-context"
import { Loader2 } from "lucide-react"

export default function AuthCallbackPage() {
  const router = useRouter()
  const searchParams = useSearchParams()
  const { handleAuthCallback } = useAuth()
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    const processCallback = async () => {
      const code = searchParams.get("code")
      const errorParam = searchParams.get("error")

      if (errorParam) {
        setError(errorParam)
        return
      }

      if (!code) {
        setError("No authorization code received")
        return
      }

      try {
        const { redirectUrl } = await handleAuthCallback(code)
        router.push(redirectUrl || "/dashboard")
      } catch (err) {
        setError(err instanceof Error ? err.message : "Authentication failed")
      }
    }

    processCallback()
  }, [searchParams, handleAuthCallback, router])

  if (error) {
    return (
      <div className="min-h-screen flex flex-col items-center justify-center bg-background px-4">
        <div className="text-center space-y-4 max-w-md">
          <div className="h-12 w-12 rounded-full bg-destructive/10 flex items-center justify-center mx-auto">
            <span className="text-destructive text-xl">!</span>
          </div>
          <h1 className="text-2xl font-semibold">Authentication Failed</h1>
          <p className="text-muted-foreground">{error}</p>
          <button
            onClick={() => router.push("/login")}
            className="text-sm text-foreground underline underline-offset-4 hover:text-muted-foreground"
          >
            Return to login
          </button>
        </div>
      </div>
    )
  }

  return (
    <div className="min-h-screen flex flex-col items-center justify-center bg-background">
      <div className="text-center space-y-4">
        <Loader2 className="h-8 w-8 animate-spin mx-auto text-muted-foreground" />
        <p className="text-muted-foreground">Completing sign in...</p>
      </div>
    </div>
  )
}
