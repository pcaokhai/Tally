import { NextRequest, NextResponse } from "next/server";
import {
  encodeOAuthFlowState,
  generateOAuthState,
  OAUTH_STATE_COOKIE,
  safeRedirectTarget,
} from "@/lib/authFlow";

// TLY-104-AC1: kicks off the Keycloak Authorization Code flow. In mock mode
// (no KEYCLOAK_ISSUER_URL configured) we skip straight to the callback with a
// fake code — but only outside production (security review HIGH: a missing
// env var in prod must fail closed, never silently log everyone in as
// user_1/tenant_a). A random `state` is stashed in a short-lived httpOnly
// cookie and checked back in /callback (security review CRITICAL: without
// this, an attacker's own auth code can be injected into a victim's session —
// OAuth login CSRF).
export function GET(request: NextRequest): NextResponse {
  const target = safeRedirectTarget(request.nextUrl.searchParams.get("target"));
  const issuer = process.env.KEYCLOAK_ISSUER_URL;

  if (!issuer) {
    if (process.env.NODE_ENV === "production") {
      return NextResponse.json(
        { status: 500, detail: "KEYCLOAK_ISSUER_URL is not configured" },
        { status: 500 },
      );
    }
    return startFlow(request, target, new URL("/callback", request.url), (url, state) => {
      url.searchParams.set("code", "mock-auth-code");
      url.searchParams.set("state", state);
    });
  }

  const authorizeUrl = new URL(`${issuer}/protocol/openid-connect/auth`);
  authorizeUrl.searchParams.set("client_id", process.env.KEYCLOAK_CLIENT_ID ?? "tally-web");
  authorizeUrl.searchParams.set("response_type", "code");
  authorizeUrl.searchParams.set("redirect_uri", new URL("/callback", request.url).toString());
  return startFlow(request, target, authorizeUrl, (url, state) => {
    url.searchParams.set("state", state);
  });
}

function startFlow(
  request: NextRequest,
  target: string,
  destination: URL,
  attachState: (url: URL, state: string) => void,
): NextResponse {
  const state = generateOAuthState();
  attachState(destination, state);

  const response = NextResponse.redirect(destination);
  response.cookies.set(OAUTH_STATE_COOKIE, encodeOAuthFlowState({ state, target }), {
    httpOnly: true,
    secure: request.nextUrl.protocol === "https:",
    sameSite: "lax",
    path: "/",
    maxAge: 300,
  });
  return response;
}
