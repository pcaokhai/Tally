import { NextRequest, NextResponse } from "next/server";
import { decryptSession, encryptSession, sessionState, SESSION_COOKIE, touchSession } from "@/lib/session";

const PUBLIC_PATHS = ["/login", "/callback"];

// TLY-104-AC4: idle (30 min) / absolute (12h) session expiry redirects to
// login, preserving the page the user was headed to via ?target=.
export function proxy(request: NextRequest): NextResponse {
  const { pathname } = request.nextUrl;
  if (PUBLIC_PATHS.some((path) => pathname.startsWith(path))) {
    return NextResponse.next();
  }

  const cookie = request.cookies.get(SESSION_COOKIE)?.value;
  const session = cookie ? decryptSession(cookie) : null;
  const state = sessionState(session);

  if (state !== "valid") {
    const loginUrl = new URL("/login", request.url);
    loginUrl.searchParams.set("target", pathname + request.nextUrl.search);
    const response = NextResponse.redirect(loginUrl);
    response.cookies.delete(SESSION_COOKIE);
    return response;
  }

  // ponytail: re-encrypt + re-set the cookie on every authenticated request
  // so the idle timer actually extends with activity (security review LOW —
  // previously lastSeenAt was frozen at login, force-logging active users
  // out after 30 min regardless of use). Upgrade to a cheaper "touch at most
  // once a minute" if per-request re-encryption ever shows up in profiling.
  const response = NextResponse.next();
  if (session) {
    response.cookies.set(SESSION_COOKIE, encryptSession(touchSession(session)), {
      httpOnly: true,
      secure: request.nextUrl.protocol === "https:",
      sameSite: "lax",
      path: "/",
    });
  }
  return response;
}

export const config = {
  matcher: ["/((?!_next|api/bff|favicon.ico|mockServiceWorker.js).*)"],
};
