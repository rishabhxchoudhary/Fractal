"use client"

import { useRouter } from "next/navigation"
import { useAuth } from "@/lib/auth-context"
import { Button } from "@/components/ui/button"
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import { Building2, ChevronRight, Plus } from "lucide-react"
import type { Workspace } from "@/lib/types"

export default function SelectWorkspacePage() {
  const router = useRouter()
  const { user, workspaces, setCurrentWorkspace, logout } = useAuth()

  const handleSelectWorkspace = (workspace: Workspace) => {
    setCurrentWorkspace(workspace)
    router.push("/dashboard")
  }

  const handleCreateNew = () => {
    router.push("/welcome/new-workspace")
  }

  const handleLogout = async () => {
    await logout()
    router.push("/login")
  }

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
          <div className="flex items-center gap-4">
            {user && (
              <span className="text-sm text-muted-foreground">{user.email}</span>
            )}
            <Button variant="ghost" size="sm" onClick={handleLogout}>
              Sign out
            </Button>
          </div>
        </div>
      </header>

      {/* Main content */}
      <main className="flex-1 flex items-center justify-center px-4 py-12">
        <div className="w-full max-w-lg space-y-6">
          {/* Header */}
          <div className="text-center space-y-2">
            <h1 className="text-3xl font-bold tracking-tight">
              Choose a workspace
            </h1>
            <p className="text-muted-foreground">
              Select a workspace to continue or create a new one
            </p>
          </div>

          {/* Workspace list */}
          <Card className="border-0 shadow-lg">
            <CardHeader>
              <CardTitle className="text-lg">Your workspaces</CardTitle>
              <CardDescription>
                You have access to {workspaces.length} workspace
                {workspaces.length !== 1 ? "s" : ""}
              </CardDescription>
            </CardHeader>
            <CardContent className="space-y-2">
              {workspaces.map((workspace) => (
                <button
                  key={workspace.id}
                  onClick={() => handleSelectWorkspace(workspace)}
                  className="w-full flex items-center justify-between p-3 rounded-lg border bg-background hover:bg-accent transition-colors text-left group"
                >
                  <div className="flex items-center gap-3">
                    <div className="h-10 w-10 rounded-lg bg-foreground/5 flex items-center justify-center">
                      <Building2 className="h-5 w-5 text-foreground" />
                    </div>
                    <div>
                      <div className="font-medium">{workspace.name}</div>
                      <div className="text-sm text-muted-foreground">
                        {workspace.slug}.taskflow.app
                      </div>
                    </div>
                  </div>
                  <ChevronRight className="h-5 w-5 text-muted-foreground group-hover:text-foreground transition-colors" />
                </button>
              ))}
            </CardContent>
          </Card>

          {/* Create new workspace */}
          <Button
            variant="outline"
            className="w-full h-12 gap-2 bg-transparent"
            onClick={handleCreateNew}
          >
            <Plus className="h-4 w-4" />
            Create new workspace
          </Button>
        </div>
      </main>
    </div>
  )
}
