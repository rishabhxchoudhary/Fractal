"use client"

import type { ReactNode } from "react"
import { useRouter } from "next/navigation"
import { useAuth } from "@/lib/auth-context"
import { redirectToRoot } from "@/lib/utils"
import { WorkspaceSelector } from "@/components/workspace/workspace-selector"
import { Button } from "@/components/ui/button"
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu"
import { Avatar, AvatarFallback, AvatarImage } from "@/components/ui/avatar"
import {
  Bell,
  HelpCircle,
  Home,
  Inbox,
  ListTodo,
  LogOut,
  Search,
  Settings,
  User,
} from "lucide-react"
import { PermissionGuard } from "@/components/rbac/permission-guard"
import { WorkspacePermission } from "@/lib/permissions"

interface DashboardLayoutProps {
  children: ReactNode
}

export function DashboardLayout({ children }: DashboardLayoutProps) {
  const router = useRouter()
  const { user, logout } = useAuth()

  const handleLogout = async () => {
    await logout()
    redirectToRoot("/login")
  }

  const navItems = [
    { icon: Home, label: "Home", href: "/dashboard" },
    { icon: Inbox, label: "Inbox", href: "/dashboard/inbox" },
    { icon: ListTodo, label: "My Tasks", href: "/dashboard/tasks" },
  ]

  const handleSettingsClick = () => {
    router.push("/settings")
  }

  return (
    <div className="min-h-screen flex bg-background">
      {/* Sidebar */}
      <aside className="w-64 border-r bg-muted/30 flex flex-col">
        {/* Workspace selector */}
        <div className="p-3 border-b">
          <WorkspaceSelector className="w-full" />
        </div>

        {/* Search */}
        <div className="p-3">
          <Button
            variant="ghost"
            className="w-full justify-start gap-2 text-muted-foreground h-9"
          >
            <Search className="h-4 w-4" />
            <span>Search</span>
            <kbd className="ml-auto pointer-events-none inline-flex h-5 select-none items-center gap-1 rounded border bg-muted px-1.5 font-mono text-[10px] font-medium text-muted-foreground">
              <span className="text-xs">Cmd</span>K
            </kbd>
          </Button>
        </div>

        {/* Navigation */}
        <nav className="flex-1 p-3 space-y-1">
          {navItems.map((item) => (
            <Button
              key={item.label}
              variant="ghost"
              className="w-full justify-start gap-2 h-9"
              onClick={() => router.push(item.href)}
            >
              <item.icon className="h-4 w-4" />
              <span>{item.label}</span>
            </Button>
          ))}
        </nav>

        {/* Bottom section */}
        <div className="p-3 border-t space-y-1">
          <Button
            variant="ghost"
            className="w-full justify-start gap-2 text-muted-foreground h-9"
          >
            <HelpCircle className="h-4 w-4" />
            <span>Help & Support</span>
          </Button>
          <PermissionGuard permission={WorkspacePermission.ACCESS_SETTINGS} showFallback={false}>
            <Button
              variant="ghost"
              className="w-full justify-start gap-2 text-muted-foreground h-9"
              onClick={handleSettingsClick}
            >
              <Settings className="h-4 w-4" />
              <span>Settings</span>
            </Button>
          </PermissionGuard>
        </div>
      </aside>

      {/* Main content */}
      <div className="flex-1 flex flex-col">
        {/* Header */}
        <header className="h-14 border-b flex items-center justify-between px-4">
          <div>{/* Breadcrumbs or page title can go here */}</div>

          <div className="flex items-center gap-2">
            <Button variant="ghost" size="icon" className="h-8 w-8">
              <Bell className="h-4 w-4" />
            </Button>

            <DropdownMenu>
              <DropdownMenuTrigger asChild>
                <Button
                  variant="ghost"
                  className="h-8 w-8 rounded-full p-0"
                >
                  <Avatar className="h-8 w-8">
                    <AvatarImage
                      src={user?.profilePicture || "/placeholder.svg"}
                      alt={user?.name || "User"}
                    />
                    <AvatarFallback>
                      {user?.name?.charAt(0).toUpperCase() || "U"}
                    </AvatarFallback>
                  </Avatar>
                </Button>
              </DropdownMenuTrigger>
              <DropdownMenuContent align="end" className="w-56">
                <DropdownMenuLabel>
                  <div className="flex flex-col space-y-1">
                    <p className="text-sm font-medium">{user?.name}</p>
                    <p className="text-xs text-muted-foreground">
                      {user?.email}
                    </p>
                  </div>
                </DropdownMenuLabel>
                <DropdownMenuSeparator />
                <DropdownMenuItem>
                  <User className="h-4 w-4 mr-2" />
                  Profile
                </DropdownMenuItem>
                <PermissionGuard permission={WorkspacePermission.ACCESS_SETTINGS} showFallback={false}>
                  <DropdownMenuItem onClick={handleSettingsClick}>
                    <Settings className="h-4 w-4 mr-2" />
                    Settings
                  </DropdownMenuItem>
                </PermissionGuard>
                <DropdownMenuSeparator />
                <DropdownMenuItem
                  onClick={handleLogout}
                  className="text-destructive focus:text-destructive"
                >
                  <LogOut className="h-4 w-4 mr-2" />
                  Sign out
                </DropdownMenuItem>
              </DropdownMenuContent>
            </DropdownMenu>
          </div>
        </header>

        {/* Page content */}
        <main className="flex-1 overflow-auto">{children}</main>
      </div>
    </div>
  )
}
