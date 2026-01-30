"use client";

import React, { useEffect, useRef, Suspense } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import { apiClient } from "@/lib/api";
import { useAuth } from "@/lib/auth-context";
import { redirectToRoot } from "@/lib/utils";
import { Button } from "@/components/ui/button";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import { Alert, AlertDescription } from "@/components/ui/alert";
import { toast } from "sonner";
import {
  Loader2,
  CheckCircle2,
  AlertCircle,
  ArrowRight,
} from "lucide-react";

function InviteAcceptanceContent() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const { refreshWorkspaces } = useAuth();
  const processedRef = useRef(false);

  const token = searchParams.get("token");
  const [state, setState] = React.useState<
    "loading" | "success" | "error" | "invalid"
  >("loading");
  const [errorMessage, setErrorMessage] = React.useState("");

  useEffect(() => {
    const processInvite = async () => {
      // Prevent double execution in React Strict Mode
      if (processedRef.current) return;
      processedRef.current = true;

      // Check if token is present
      if (!token) {
        setState("invalid");
        setErrorMessage("No invitation token provided. Please check your email link.");
        return;
      }

      setState("loading");

      try {
        // Call the accept invite API
        await apiClient.acceptInvite(token);

        // Refresh workspaces to reflect the new membership
        await refreshWorkspaces();

        setState("success");
        toast.success("Invitation accepted! Redirecting...");

        // Redirect to workspace selection after 2 seconds
        setTimeout(() => {
          redirectToRoot("/select-workspace");
        }, 2000);
      } catch (error) {
        console.error("Failed to accept invite:", error);
        setState("error");
        setErrorMessage(
          error instanceof Error
            ? error.message
            : "Failed to accept invitation. The token may have expired or is invalid.",
        );
      }
    };

    processInvite();
  }, [token, refreshWorkspaces]);

  const handleRetry = () => {
    processedRef.current = false;
    setState("loading");
    setErrorMessage("");
    window.location.reload();
  };

  const handleBackToLogin = () => {
    redirectToRoot("/login");
  };

  return (
    <div className="min-h-screen flex items-center justify-center bg-muted/30 p-4">
      <div className="w-full max-w-md">
        {state === "loading" && (
          <Card className="border-0 shadow-lg">
            <CardHeader className="text-center">
              <CardTitle>Processing Your Invitation</CardTitle>
              <CardDescription>
                Please wait while we accept your workspace invitation...
              </CardDescription>
            </CardHeader>
            <CardContent className="flex flex-col items-center gap-4">
              <Loader2 className="h-8 w-8 animate-spin text-primary" />
              <p className="text-sm text-muted-foreground">
                This may take a moment
              </p>
            </CardContent>
          </Card>
        )}

        {state === "success" && (
          <Card className="border-0 shadow-lg border-green-200 bg-green-50">
            <CardHeader className="text-center">
              <div className="flex justify-center mb-2">
                <CheckCircle2 className="h-12 w-12 text-green-600" />
              </div>
              <CardTitle>Invitation Accepted!</CardTitle>
              <CardDescription>
                You have successfully joined the workspace
              </CardDescription>
            </CardHeader>
            <CardContent className="flex flex-col gap-4">
              <p className="text-sm text-muted-foreground text-center">
                Redirecting to your workspace selection...
              </p>
              <div className="flex justify-center">
                <Loader2 className="h-4 w-4 animate-spin text-muted-foreground" />
              </div>
            </CardContent>
          </Card>
        )}

        {state === "error" && (
          <Card className="border-0 shadow-lg border-red-200 bg-red-50">
            <CardHeader className="text-center">
              <div className="flex justify-center mb-2">
                <AlertCircle className="h-12 w-12 text-red-600" />
              </div>
              <CardTitle>Invitation Failed</CardTitle>
              <CardDescription>
                We couldn't accept your invitation
              </CardDescription>
            </CardHeader>
            <CardContent className="space-y-4">
              <Alert className="border-red-200 bg-red-100">
                <AlertDescription className="text-red-800">
                  {errorMessage}
                </AlertDescription>
              </Alert>
              <div className="space-y-2">
                <Button
                  onClick={handleRetry}
                  className="w-full"
                  variant="default"
                >
                  Try Again
                </Button>
                <Button
                  onClick={handleBackToLogin}
                  className="w-full"
                  variant="outline"
                >
                  Back to Login
                </Button>
              </div>
            </CardContent>
          </Card>
        )}

        {state === "invalid" && (
          <Card className="border-0 shadow-lg border-yellow-200 bg-yellow-50">
            <CardHeader className="text-center">
              <div className="flex justify-center mb-2">
                <AlertCircle className="h-12 w-12 text-yellow-600" />
              </div>
              <CardTitle>Invalid Invitation Link</CardTitle>
              <CardDescription>
                The invitation link is not valid
              </CardDescription>
            </CardHeader>
            <CardContent className="space-y-4">
              <Alert className="border-yellow-200 bg-yellow-100">
                <AlertDescription className="text-yellow-800">
                  {errorMessage}
                </AlertDescription>
              </Alert>
              <p className="text-sm text-muted-foreground">
                Please ask the workspace administrator to send you a new invitation link.
              </p>
              <Button
                onClick={handleBackToLogin}
                className="w-full"
                variant="default"
              >
                <ArrowRight className="h-4 w-4 mr-2" />
                Back to Login
              </Button>
            </CardContent>
          </Card>
        )}

        {/* Help text */}
        <div className="mt-8 text-center">
          <p className="text-xs text-muted-foreground">
            If you're having trouble, please contact your workspace administrator
          </p>
        </div>
      </div>
    </div>
  );
}

export default function InviteAcceptancePage() {
  return (
    <Suspense
      fallback={
        <div className="min-h-screen flex items-center justify-center bg-muted/30">
          <div className="flex flex-col items-center gap-2">
            <Loader2 className="h-8 w-8 animate-spin text-muted-foreground" />
            <p className="text-muted-foreground">Loading...</p>
          </div>
        </div>
      }
    >
      <InviteAcceptanceContent />
    </Suspense>
  );
}
