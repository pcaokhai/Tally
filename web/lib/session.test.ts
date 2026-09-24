import { beforeEach, describe, expect, it } from "vitest";
import {
  decryptSession,
  encryptSession,
  sessionState,
  touchSession,
  type SessionPayload,
} from "./session";

const basePayload: SessionPayload = {
  userId: "user_1",
  tenantId: "tenant_a",
  memberships: [
    { tenantId: "tenant_a", tenantName: "Tenant A" },
    { tenantId: "tenant_b", tenantName: "Tenant B" },
  ],
  issuedAt: 1_000_000,
  lastSeenAt: 1_000_000,
};

beforeEach(() => {
  process.env.SESSION_SECRET = "test-secret-do-not-use-in-prod";
});

describe("session cookie encryption — TLY-104-AC1", () => {
  it("round-trips the payload through encrypt/decrypt and hides it as opaque ciphertext", () => {
    const token = encryptSession(basePayload);

    // No JS-readable claim: raw token contains no plaintext user/tenant ids.
    expect(token).not.toContain(basePayload.userId);
    expect(token).not.toContain(basePayload.tenantId);

    expect(decryptSession(token)).toEqual(basePayload);
  });

  it("rejects a tampered token instead of returning forged data", () => {
    const token = encryptSession(basePayload);
    const tampered = token.slice(0, -2) + "aa";
    expect(decryptSession(tampered)).toBeNull();
  });
});

describe("session timeout policy — TLY-104-AC4", () => {
  it("is valid within the 30 min idle window and 12h absolute window", () => {
    const now = basePayload.issuedAt + 5 * 60 * 1000;
    expect(sessionState(basePayload, now)).toBe("valid");
  });

  it("expires after 30 minutes of inactivity", () => {
    const now = basePayload.lastSeenAt + 31 * 60 * 1000;
    expect(sessionState(basePayload, now)).toBe("idle-expired");
  });

  it("expires after 12 hours regardless of recent activity", () => {
    const recentlyActive = touchSession(basePayload, basePayload.issuedAt + 12 * 60 * 60 * 1000 - 1000);
    const now = basePayload.issuedAt + 12 * 60 * 60 * 1000 + 1000;
    expect(sessionState(recentlyActive, now)).toBe("absolute-expired");
  });

  it("treats a missing session as invalid", () => {
    expect(sessionState(null)).toBe("invalid");
  });
});
