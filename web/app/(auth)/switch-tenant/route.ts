import { NextRequest, NextResponse } from "next/server";
import { decryptSession, encryptSession, SESSION_COOKIE } from "@/lib/session";

// TLY-104-AC2: exchanges the current session for one scoped to a different
// tenant the user is a member of, then reissues the session cookie. The
// browser reloads afterward so first render of the new tenant starts clean
// (cache clearing itself is client-side — see components/shell/TenantSwitcher).
export async function POST(request: NextRequest): Promise<NextResponse> {
  const cookie = request.cookies.get(SESSION_COOKIE)?.value;
  const session = cookie ? decryptSession(cookie) : null;
  if (!session) {
    return NextResponse.json({ error: "unauthenticated" }, { status: 401 });
  }

  const body = (await request.json().catch(() => null)) as { tenantId?: string } | null;
  const tenantId = body?.tenantId;
  const membership = session.memberships.find((m) => m.tenantId === tenantId);
  if (!tenantId || !membership) {
    return NextResponse.json({ error: "not a member of that tenant" }, { status: 403 });
  }

  const now = Date.now();
  const token = encryptSession({ ...session, tenantId, lastSeenAt: now });

  const response = NextResponse.json({ tenantId: membership.tenantId, tenantName: membership.tenantName });
  response.cookies.set(SESSION_COOKIE, token, {
    httpOnly: true,
    secure: request.nextUrl.protocol === "https:",
    sameSite: "lax",
    path: "/",
  });
  return response;
}
