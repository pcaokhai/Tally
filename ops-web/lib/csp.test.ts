import { describe, expect, test } from "vitest";
import { buildCsp, buildSecurityHeaders } from "./csp";

describe("buildCsp", () => {
  test("the production policy forbids framing — TLY-007-AC3", () => {
    const csp = buildCsp({ isProduction: true, nonce: "abc123" });
    expect(csp).toContain("frame-ancestors 'none'");
    expect(csp).toContain("default-src 'self'");
    expect(csp).toContain("object-src 'none'");
    expect(csp).toContain("base-uri 'self'");
  });

  test("the production policy allows no third-party origin — TLY-007-AC3", () => {
    const csp = buildCsp({ isProduction: true, nonce: "abc123" });
    expect(csp).not.toMatch(/https?:\/\//);
  });

  test("the production policy carries the request nonce and no unsafe-eval — TLY-007-AC3", () => {
    const csp = buildCsp({ isProduction: true, nonce: "abc123" });
    expect(csp).toContain("'nonce-abc123'");
    expect(csp).toContain("'strict-dynamic'");
    expect(csp).not.toContain("unsafe-eval");
    expect(csp).not.toContain("unsafe-inline");
  });

  test("the development policy allows unsafe-eval and unsafe-inline for HMR — TLY-007-AC3", () => {
    const csp = buildCsp({ isProduction: false, nonce: "abc123" });
    expect(csp).toContain("unsafe-eval");
    expect(csp).toContain("unsafe-inline");
  });

  test("an unknown NODE_ENV fails safe to the strict production policy — TLY-007-AC3", () => {
    const nodeEnv: string = "typo-env";
    const strict = buildCsp({ isProduction: true, nonce: "abc123" });
    const unknownEnv = buildCsp({ isProduction: nodeEnv !== "development", nonce: "abc123" });
    expect(unknownEnv).toBe(strict);
    expect(unknownEnv).not.toContain("unsafe-eval");
  });
});

describe("buildSecurityHeaders", () => {
  test("includes the non-CSP security headers — TLY-007-AC3", () => {
    const headers = buildSecurityHeaders();
    const keys = headers.map((h) => h.key);
    expect(keys).toEqual([
      "X-Frame-Options",
      "X-Content-Type-Options",
      "Referrer-Policy",
      "Permissions-Policy",
      "Strict-Transport-Security",
      "Cross-Origin-Opener-Policy",
    ]);
    expect(headers.find((h) => h.key === "X-Frame-Options")?.value).toBe("DENY");
  });
});
