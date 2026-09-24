import { describe, expect, it } from "vitest";
import { decodeOAuthFlowState, encodeOAuthFlowState, safeRedirectTarget } from "./authFlow";

describe("safeRedirectTarget — TLY-104-AC1 (open redirect hardening)", () => {
  it("keeps a plain relative path", () => {
    expect(safeRedirectTarget("/invoices")).toBe("/invoices");
  });

  it("defaults to / for null or empty", () => {
    expect(safeRedirectTarget(null)).toBe("/");
    expect(safeRedirectTarget("")).toBe("/");
  });

  it("rejects an absolute URL to another origin", () => {
    expect(safeRedirectTarget("https://evil.com")).toBe("/");
  });

  it("rejects a protocol-relative URL", () => {
    expect(safeRedirectTarget("//evil.com")).toBe("/");
  });

  it("rejects a path that doesn't start with /", () => {
    expect(safeRedirectTarget("evil.com")).toBe("/");
  });
});

describe("OAuth flow state round-trip — TLY-104-AC1 (login CSRF / state validation)", () => {
  it("round-trips state + target through the cookie encoding", () => {
    const encoded = encodeOAuthFlowState({ state: "abc123", target: "/invoices" });
    expect(decodeOAuthFlowState(encoded)).toEqual({ state: "abc123", target: "/invoices" });
  });

  it("rejects a missing or malformed cookie value", () => {
    expect(decodeOAuthFlowState(undefined)).toBeNull();
    expect(decodeOAuthFlowState("not-base64url-json")).toBeNull();
  });
});
