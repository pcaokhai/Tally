import { createCipheriv, createDecipheriv, createHash, randomBytes } from "node:crypto";

// ponytail: AES-256-GCM via node:crypto (stdlib) instead of a session-cookie
// library — one algorithm, no extra dependency. Upgrade to key rotation /
// a KMS-backed secret if multi-instance key distribution becomes a need.

export const SESSION_COOKIE = "tally_session";
const IDLE_TIMEOUT_MS = 30 * 60 * 1000;
const ABSOLUTE_TIMEOUT_MS = 12 * 60 * 60 * 1000;

export interface Membership {
  tenantId: string;
  tenantName: string;
}

export interface SessionPayload {
  userId: string;
  tenantId: string;
  memberships: Membership[];
  issuedAt: number;
  lastSeenAt: number;
}

function sessionSecret(): string {
  const secret = process.env.SESSION_SECRET;
  if (!secret) {
    throw new Error("SESSION_SECRET is not configured");
  }
  return secret;
}

function encryptionKey(): Buffer {
  return createHash("sha256").update(sessionSecret()).digest();
}

export function encryptSession(payload: SessionPayload): string {
  const iv = randomBytes(12);
  const cipher = createCipheriv("aes-256-gcm", encryptionKey(), iv);
  const plaintext = Buffer.from(JSON.stringify(payload), "utf8");
  const ciphertext = Buffer.concat([cipher.update(plaintext), cipher.final()]);
  const authTag = cipher.getAuthTag();
  return [iv, authTag, ciphertext].map((buf) => buf.toString("base64url")).join(".");
}

export function decryptSession(token: string): SessionPayload | null {
  const parts = token.split(".");
  if (parts.length !== 3) return null;
  const [ivPart, tagPart, dataPart] = parts;
  if (!ivPart || !tagPart || !dataPart) return null;
  try {
    const decipher = createDecipheriv("aes-256-gcm", encryptionKey(), Buffer.from(ivPart, "base64url"));
    decipher.setAuthTag(Buffer.from(tagPart, "base64url"));
    const plaintext = Buffer.concat([
      decipher.update(Buffer.from(dataPart, "base64url")),
      decipher.final(),
    ]);
    return JSON.parse(plaintext.toString("utf8")) as SessionPayload;
  } catch {
    return null;
  }
}

export type SessionState = "valid" | "idle-expired" | "absolute-expired" | "invalid";

export function sessionState(payload: SessionPayload | null, now: number = Date.now()): SessionState {
  if (!payload) return "invalid";
  if (now - payload.issuedAt > ABSOLUTE_TIMEOUT_MS) return "absolute-expired";
  if (now - payload.lastSeenAt > IDLE_TIMEOUT_MS) return "idle-expired";
  return "valid";
}

export function touchSession(payload: SessionPayload, now: number = Date.now()): SessionPayload {
  return { ...payload, lastSeenAt: now };
}
