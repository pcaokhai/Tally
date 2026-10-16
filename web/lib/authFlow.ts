import { randomBytes } from "node:crypto";

export const OAUTH_STATE_COOKIE = "tally_oauth_state";

/**
 * Only ever redirect to a path on our own origin. Rejects absolute URLs
 * (`https://evil.com`) and protocol-relative ones (`//evil.com`), which
 * `new URL(target, base)` would otherwise happily resolve off-origin —
 * TLY-104 security review CRITICAL: open redirect via `target`.
 */
export function safeRedirectTarget(rawTarget: string | null): string {
  if (!rawTarget) return "/";
  if (!rawTarget.startsWith("/") || rawTarget.startsWith("//")) return "/";
  return rawTarget;
}

export interface OAuthFlowState {
  state: string;
  target: string;
}

export function generateOAuthState(): string {
  return randomBytes(16).toString("base64url");
}

/** Serialized into a short-lived httpOnly cookie set by /login and read back
 * by /callback, so the callback can verify `state` came from a login we
 * actually started (TLY-104 security review CRITICAL: OAuth login CSRF /
 * code injection) and recover the original `target` without trusting the
 * query string a second time. */
export function encodeOAuthFlowState(flow: OAuthFlowState): string {
  return Buffer.from(JSON.stringify(flow), "utf8").toString("base64url");
}

export function decodeOAuthFlowState(raw: string | undefined): OAuthFlowState | null {
  if (!raw) return null;
  try {
    const parsed: unknown = JSON.parse(Buffer.from(raw, "base64url").toString("utf8"));
    if (
      typeof parsed === "object" &&
      parsed !== null &&
      "state" in parsed &&
      "target" in parsed &&
      typeof (parsed as { state: unknown }).state === "string" &&
      typeof (parsed as { target: unknown }).target === "string"
    ) {
      return parsed as OAuthFlowState;
    }
    return null;
  } catch {
    return null;
  }
}
