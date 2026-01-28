import { NextRequest, NextResponse } from "next/server";
import { getSubdomain, getRootDomain } from "@/lib/utils";

export const config = {
  matcher: [
    /*
     * Match all paths except for:
     * 1. /api routes
     * 2. /_next (Next.js internals)
     * 3. /_static (inside /public)
     * 4. all root files inside /public (e.g. /favicon.ico)
     */
    "/((?!api/|_next/|_static/|[\\w-]+\\.\\w+).*)",
  ],
};

export default async function proxy(req: NextRequest) {
  const url = req.nextUrl;
  const hostname = req.headers.get("host") || "";
  
  // Get the subdomain (e.g., "my-workspace" from "my-workspace.localhost:3000")
  const subdomain = getSubdomain(hostname);
  const rootDomain = getRootDomain();

  const searchParams = req.nextUrl.searchParams.toString();
  // Get the path (e.g. "/dashboard")
  const path = `${url.pathname}${
    searchParams.length > 0 ? `?${searchParams}` : ""
  }`;

  // 1. Handle Subdomains (e.g. workspace.localhost:3000)
  if (subdomain) {
    // Rewrite to the dynamic route folder: /app/[domain]/path
    // We will create this folder structure in the next step
    return NextResponse.rewrite(
      new URL(`/${subdomain}${path === "/" ? "" : path}`, req.url)
    );
  }

  // 2. Handle Root Domain (localhost:3000)
  // If the user is on the root domain, we don't rewrite, 
  // or we rewrite to a specific marketing layout folder if desired.
  // For now, let's keep it simple: no rewrite means it renders `app/page.tsx` normally.
  
  // Optional: If you want to force all root logic into a (site) folder:
  // return NextResponse.rewrite(new URL(`/home${path}`, req.url));

  return NextResponse.next();
}