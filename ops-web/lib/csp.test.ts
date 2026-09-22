import { describe, expect, test } from "vitest";
import { buildCsp, buildSecurityHeaders } from "./csp";

describe("buildCsp", () => {
  test("the production policy forbids framing — TLY-007-AC3", () => {
    const csp = buildCsp({ isProduction: true });
    expect(csp).toContain("frame-ancestors 'none'");
    expect(csp).toContain("default-src 'self'");
    expect(csp).toContain("object-src 'none'");
    expect(csp).toContain("base-uri 'self'");
  });

  test("the production policy allows no third-party origin — TLY-007-AC3", () => {
    const csp = buildCsp({ isProduction: true });
    expect(csp).not.toMatch(/https?:\/\//);
  });

  test("the production policy has no unsafe-eval — TLY-007-AC3", () => {
    const csp = buildCsp({ isProduction: true });
    expect(csp).not.toContain("unsafe-eval");
    expect(csp).not.toContain("unsafe-inline");
  });

  test("the development policy allows unsafe-eval and unsafe-inline for HMR — TLY-007-AC3", () => {
    const csp = buildCsp({ isProduction: false });
    expect(csp).toContain("unsafe-eval");
    expect(csp).toContain("unsafe-inline");
  });
});

describe("buildSecurityHeaders", () => {
  test("includes all required security headers — TLY-007-AC3", () => {
    const headers = buildSecurityHeaders({ isProduction: true });
    const keys = headers.map((h) => h.key);
    expect(keys).toEqual([
      "Content-Security-Policy",
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
