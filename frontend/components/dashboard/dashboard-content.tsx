"use client"

import { useEffect } from "react"
import { useRouter } from "next/navigation"
import { useAuth } from "@/lib/auth-context"
import { useProjects } from "@/lib/project-context"
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import { Button } from "@/components/ui/button"
import { Badge } from "@/components/ui/badge"
import { Calendar, CheckCircle2, Clock, Plus, Target, FolderOpen, ArrowRight } from "lucide-react"

export function DashboardContent() {
  const router = useRouter()
  const { user, currentWorkspace } = useAuth()
  const { currentProject, projects, refreshProjects } = useProjects()

  useEffect(() => {
    if (currentWorkspace?.id) {
      refreshProjects(currentWorkspace.id)
    }
  }, [currentWorkspace?.id, refreshProjects])

  const quickActions = [
    { 
      icon: Plus, 
      label: "New Task", 
      description: "Create a new task",
      action: () => router.push("./tasks?new=true")
    },
    { 
      icon: Target, 
      label: "View Projects", 
      description: "Manage your projects",
      action: () => router.push("../projects")
    },
    { 
      icon: Calendar, 
      label: "Schedule", 
      description: "View your calendar",
      action: () => router.push("./calendar")
    },
  ]

  return (
    <div className="p-6 space-y-6">
      {/* Welcome section */}
      <div className="space-y-1">
        <h1 className="text-2xl font-bold tracking-tight">
          Good {getGreeting()}, {user?.name?.split(" ")[0] || "there"}
        </h1>
        {currentProject ? (
          <p className="text-muted-foreground">
            Working on <span className="font-medium text-foreground">{currentProject.name}</span> in {currentWorkspace?.name}
          </p>
        ) : (
          <p className="text-muted-foreground">
            Here&apos;s what&apos;s happening in {currentWorkspace?.name}
          </p>
        )}
      </div>

      {/* Stats */}
      <div className="grid gap-4 md:grid-cols-3">
        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">Tasks Due Today</CardTitle>
            <Clock className="h-4 w-4 text-muted-foreground" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold">0</div>
            <p className="text-xs text-muted-foreground">
              No tasks due today
            </p>
          </CardContent>
        </Card>
        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">Completed This Week</CardTitle>
            <CheckCircle2 className="h-4 w-4 text-muted-foreground" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold">0</div>
            <p className="text-xs text-muted-foreground">
              Start completing tasks to see progress
            </p>
          </CardContent>
        </Card>
        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">Active Projects</CardTitle>
            <Target className="h-4 w-4 text-muted-foreground" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold">{projects.length}</div>
            <p className="text-xs text-muted-foreground">
              {projects.length === 0 ? "Create your first project" : "Manage your projects"}
            </p>
          </CardContent>
        </Card>
      </div>

      {/* Quick actions */}
      <Card>
        <CardHeader>
          <CardTitle>Quick Actions</CardTitle>
          <CardDescription>
            Get started with these common actions
          </CardDescription>
        </CardHeader>
        <CardContent>
          <div className="grid gap-4 md:grid-cols-3">
            {quickActions.map((action) => (
              <Button
                key={action.label}
                variant="outline"
                className="h-auto flex-col items-start gap-2 p-4 bg-transparent hover:bg-muted/50"
                onClick={action.action}
              >
                <div className="flex items-center gap-2">
                  <div className="h-8 w-8 rounded-lg bg-foreground/5 flex items-center justify-center">
                    <action.icon className="h-4 w-4" />
                  </div>
                  <span className="font-medium">{action.label}</span>
                </div>
                <span className="text-xs text-muted-foreground text-left">
                  {action.description}
                </span>
              </Button>
            ))}
          </div>
        </CardContent>
      </Card>

      {/* Recent Projects */}
      {projects.length > 0 && (
        <Card>
          <CardHeader>
            <div className="flex items-center justify-between">
              <div>
                <CardTitle>Your Projects</CardTitle>
                <CardDescription>
                  Quick access to your recent projects
                </CardDescription>
              </div>
              <Button
                variant="ghost"
                size="sm"
                onClick={() => router.push("../projects")}
                className="gap-1"
              >
                View All <ArrowRight className="h-4 w-4" />
              </Button>
            </div>
          </CardHeader>
          <CardContent>
            <div className="space-y-3">
              {projects.slice(0, 5).map((project) => (
                <div
                  key={project.id}
                  onClick={() => router.push(`../projects/${project.id}`)}
                  className="flex items-center justify-between p-3 rounded-lg border hover:bg-muted/50 cursor-pointer transition-colors"
                >
                  <div className="flex items-center gap-3 flex-1 min-w-0">
                    <div
                      className="h-3 w-3 rounded-full flex-shrink-0"
                      style={{ backgroundColor: project.color || "#808080" }}
                    />
                    <div className="min-w-0 flex-1">
                      <p className="font-medium truncate">{project.name}</p>
                    </div>
                  </div>
                  <Badge variant="outline" className="flex-shrink-0">
                    {project.role}
                  </Badge>
                </div>
              ))}
              {projects.length > 5 && (
                <Button
                  variant="outline"
                  className="w-full"
                  onClick={() => router.push("../projects")}
                >
                  View all {projects.length} projects
                </Button>
              )}
            </div>
          </CardContent>
        </Card>
      )}

      {/* Getting started */}
      <Card>
        <CardHeader>
          <CardTitle>Getting Started</CardTitle>
          <CardDescription>
            Complete these steps to set up your workspace
          </CardDescription>
        </CardHeader>
        <CardContent>
          <div className="space-y-4">
            {[
              { title: "Create your first task", completed: false },
              { title: "Invite team members", completed: false },
              { title: "Set up your first project", completed: false },
              { title: "Customize your workspace", completed: false },
            ].map((step, index) => (
              <div
                key={step.title}
                className="flex items-center gap-3 p-3 rounded-lg border bg-muted/30"
              >
                <div className="h-6 w-6 rounded-full border-2 flex items-center justify-center text-xs font-medium">
                  {index + 1}
                </div>
                <span className="flex-1">{step.title}</span>
                <Button size="sm" variant="ghost">
                  Start
                </Button>
              </div>
            ))}
          </div>
        </CardContent>
      </Card>
    </div>
  )
}

function getGreeting() {
  const hour = new Date().getHours()
  if (hour < 12) return "morning"
  if (hour < 18) return "afternoon"
  return "evening"
}
