"use client"

import React from "react"
import { useState } from "react"
import { useRouter } from "next/navigation"
import { useAuth } from "@/lib/auth-context"
import { apiClient } from "@/lib/api"
import { redirectToRoot } from "@/lib/utils"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card"
import { toast } from "sonner"
import { ArrowRight, Building2, Loader2, Sparkles } from "lucide-react"

export default function CreateWorkspacePage() {
  const router = useRouter()
  const { user, refreshWorkspaces, setCurrentWorkspace } = useAuth()
  const [workspaceName, setWorkspaceName] = useState("")
  const [isCreating, setIsCreating] = useState(false)

  const generateSlug = (name: string) => {
    return name
      .toLowerCase()
      .replace(/[^a-z0-9]+/g, "-")
      .replace(/^-|-$/g, "")
  }

  const handleCreateWorkspace = async (e: React.FormEvent) => {
    e.preventDefault()

    if (!workspaceName.trim()) {
      toast.error("Please enter a workspace name")
      return
    }

    setIsCreating(true)

    try {
      const response = await apiClient.createWorkspace({ name: workspaceName })
      await refreshWorkspaces()
      if (response?.workspace) {
        setCurrentWorkspace(response.workspace)
      }
      toast.success("Workspace created successfully!")
      redirectToRoot("/select-workspace");
    } catch (error) {
      toast.error(
        error instanceof Error ? error.message : "Failed to create workspace"
      )
    } finally {
      setIsCreating(false)
    }
  }

  const slug = generateSlug(workspaceName)

  return (
    <div className="min-h-screen flex flex-col bg-muted/30">
      {/* Header */}
      <header className="border-b bg-background/80 backdrop-blur-sm">
        <div className="container mx-auto px-4 h-16 flex items-center justify-between">
          <div className="flex items-center gap-2">
            <div className="h-8 w-8 rounded-lg bg-foreground flex items-center justify-center">
              <span className="text-background font-bold">T</span>
            </div>
            <span className="font-semibold">TaskFlow</span>
          </div>
          {user && (
            <div className="flex items-center gap-2 text-sm text-muted-foreground">
              <span>{user.email}</span>
            </div>
          )}
        </div>
      </header>

      {/* Main content */}
      <main className="flex-1 flex items-center justify-center px-4 py-12">
        <div className="w-full max-w-lg space-y-8">
          {/* Welcome message */}
          <div className="text-center space-y-2">
            <div className="flex items-center justify-center mb-4">
              <div className="h-16 w-16 rounded-2xl bg-foreground/5 flex items-center justify-center">
                <Building2 className="h-8 w-8 text-foreground" />
              </div>
            </div>
            <h1 className="text-3xl font-bold tracking-tight">
              Create a new workspace
            </h1>
            <p className="text-muted-foreground max-w-md mx-auto">
              Create a new workspace to organize different projects and teams.
            </p>
          </div>

          {/* Workspace form */}
          <Card className="border-0 shadow-lg">
            <CardHeader>
              <CardTitle className="flex items-center gap-2">
                <Building2 className="h-5 w-5" />
                Workspace details
              </CardTitle>
              <CardDescription>
                Choose a name for your new workspace. You can always change this
                later.
              </CardDescription>
            </CardHeader>
            <CardContent>
              <form onSubmit={handleCreateWorkspace} className="space-y-6">
                <div className="space-y-2">
                  <Label htmlFor="workspace-name">Workspace name</Label>
                  <Input
                    id="workspace-name"
                    placeholder="Acme Inc."
                    value={workspaceName}
                    onChange={(e) => setWorkspaceName(e.target.value)}
                    className="h-12 text-base"
                    autoFocus
                  />
                </div>

                {slug && (
                  <div className="space-y-2">
                    <Label className="text-muted-foreground">
                      Workspace URL
                    </Label>
                    <div className="flex items-center gap-0 rounded-md border bg-muted/50 px-3 py-2">
                      <span className="text-sm font-medium">{slug}</span>
                      <span className="text-sm text-muted-foreground">
                        .{window.location.host}
                      </span>
                    </div>
                  </div>
                )}

                <Button
                  type="submit"
                  className="w-full h-12 text-base"
                  disabled={!workspaceName.trim() || isCreating}
                >
                  {isCreating ? (
                    <>
                      <Loader2 className="h-4 w-4 animate-spin mr-2" />
                      Creating workspace...
                    </>
                  ) : (
                    <>
                      Create workspace
                      <ArrowRight className="h-4 w-4 ml-2" />
                    </>
                  )}
                </Button>
              </form>
            </CardContent>
          </Card>
        </div>
      </main>
    </div>
  )
}
