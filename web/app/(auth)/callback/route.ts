import { NextRequest, NextResponse } from "next/server";
import { decodeOAuthFlowState, OAUTH_STATE_COOKIE } from "@/lib/authFlow";
import { encryptSession, SESSION_COOKIE, type Membership } from "@/lib/session";

// TLY-104-AC1: exchanges the Keycloak auth code for tokens, then sets an
// httpOnly, Secure, SameSite=Lax encrypted session cookie. The access/refresh
// tokens never reach the browser — only the opaque encrypted cookie does.
//
// Security review CRITICAL fix: `state` must match the value /login stashed
// in OAUTH_STATE_COOKIE for *this* browser, or the request is rejected — this
// is what stops an attacker's own `code` being injected into a victim's
// session (OAuth login CSRF). `target` is read back from that same
// server-set cookie rather than re-trusted from the query string.
export async function GET(request: NextRequest): Promise<NextResponse> {
  const code = request.nextUrl.searchParams.get("code");
  const suppliedState = request.nextUrl.searchParams.get("state");
  const flow = decodeOAuthFlowState(request.cookies.get(OAUTH_STATE_COOKIE)?.value);

  if (!code || !suppliedState || !flow || flow.state !== suppliedState) {
    return NextResponse.redirect(new URL("/login", request.url));
  }

  const { userId, memberships, tenantId } = await exchangeCodeForIdentity(code);
  const now = Date.now();
  const token = encryptSession({
    userId,
    tenantId,
    memberships,
    issuedAt: now,
    lastSeenAt: now,
  });

  const response = NextResponse.redirect(new URL(flow.target, request.url));
  response.cookies.delete(OAUTH_STATE_COOKIE);
  response.cookies.set(SESSION_COOKIE, token, {
    httpOnly: true,
    secure: request.nextUrl.protocol === "https:",
    sameSite: "lax",
    path: "/",
  });
  return response;
}

interface Identity {
  userId: string;
  tenantId: string;
  memberships: Membership[];
}

// ponytail: mock-mode identity resolution stands in for the real Keycloak
// token exchange + /v1/me call until TLY-102/103 land the live IdP and core
// endpoint; swap the body of this function, the shape (Identity) doesn't change.
async function exchangeCodeForIdentity(_code: string): Promise<Identity> {
  return {
    userId: "user_1",
    tenantId: "tenant_a",
    memberships: [
      { tenantId: "tenant_a", tenantName: "Acme Inc" },
      { tenantId: "tenant_b", tenantName: "Globex Corp" },
    ],
  };
}
