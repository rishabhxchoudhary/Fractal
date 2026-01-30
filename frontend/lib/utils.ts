import { clsx, type ClassValue } from "clsx";
import { twMerge } from "tailwind-merge";

export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs));
}

export const getRootDomain = () => {
  return process.env.NEXT_PUBLIC_ROOT_DOMAIN || "localhost:3000";
};

export const getSubdomain = (host: string) => {
  // 1. Get the root domain from env
  let rootDomain = getRootDomain(); // e.g., "lvh.me:3000" or "todo.com"

  // 2. Remove port from rootDomain if present
  if (rootDomain.includes(":")) {
    rootDomain = rootDomain.split(":")[0];
  }

  // 3. Remove port from the incoming host if present
  let hostname = host;
  if (hostname.includes(":")) {
    hostname = hostname.split(":")[0];
  }

  // 4. Handle localhost edge case
  if (hostname.includes("localhost")) {
    const parts = hostname.split(".");
    if (parts.length > 1 && !parts[0].includes("localhost")) {
      return parts[0];
    }
    return null;
  }

  // 5. Handle standard subdomains (e.g. rishabh.lvh.me)
  // Check if hostname ends with rootDomain (e.g. "rishabh.lvh.me" ends with "lvh.me")
  if (hostname.endsWith(rootDomain)) {
    // Remove the root domain part
    const subdomain = hostname.replace(`.${rootDomain}`, "");

    // Safety check: verify we actually stripped something and aren't left with the root
    if (subdomain !== hostname && subdomain !== "") {
      return subdomain;
    }
  }

  return null;
};

export const getCookieDomain = () => {
  const hostname = window.location.hostname;

  // Remove the specific localhost check, or ensure lvh.me falls through
  if (hostname.includes("localhost")) {
    return "localhost";
  }

  // Logic for lvh.me or production (app.todo.com)
  const root = getRootDomain().split(":")[0]; // "lvh.me"
  return `.${root}`; // Returns ".lvh.me" -> Shared across all subdomains
};

/**
 * Redirects to a path on the root domain (e.g., /login, /select-workspace)
 * Use this for cross-subdomain navigation where Next.js router won't work
 * 
 * @param path - The path to redirect to (e.g., "/login", "/select-workspace")
 * @param queryParams - Optional query parameters as an object (e.g., { error: "auth_failed" })
 * 
 * @example
 * redirectToRoot("/login")
 * redirectToRoot("/login", { error: "auth_failed" })
 */
export const redirectToRoot = (path: string, queryParams?: Record<string, string>) => {
  if (typeof window === "undefined") {
    console.warn("redirectToRoot called on server side");
    return;
  }

  const rootDomain = getRootDomain();
  const protocol = window.location.protocol;
  
  let url = `${protocol}//${rootDomain}${path}`;
  
  if (queryParams && Object.keys(queryParams).length > 0) {
    const params = new URLSearchParams(queryParams);
    url += `?${params.toString()}`;
  }
  
  window.location.href = url;
};

/**
 * Redirects to a path on a specific workspace subdomain
 * 
 * @param workspaceSlug - The workspace slug (subdomain)
 * @param path - The path to redirect to (e.g., "/dashboard", "/settings")
 * 
 * @example
 * redirectToWorkspace("my-workspace", "/dashboard")
 * redirectToWorkspace("my-workspace", "/settings")
 */
export const redirectToWorkspace = (workspaceSlug: string, path: string = "/dashboard") => {
  if (typeof window === "undefined") {
    console.warn("redirectToWorkspace called on server side");
    return;
  }

  const rootDomain = getRootDomain();
  const protocol = window.location.protocol;
  const url = `${protocol}//${workspaceSlug}.${rootDomain}${path}`;
  
  window.location.href = url;
};
