import { expect, test } from "@playwright/test";

test("the response serves a strict CSP with no third-party origins — TLY-007-AC3", async ({ request }) => {
  const response = await request.get("/");
  const csp = response.headers()["content-security-policy"];
  expect(csp).toBeDefined();
  expect(csp).toContain("frame-ancestors 'none'");
  expect(csp).toContain("default-src 'self'");
  expect(csp).toContain("object-src 'none'");
  expect(csp).toContain("base-uri 'self'");
  expect(csp).not.toMatch(/https?:\/\//);
  expect(csp).not.toContain("unsafe-eval");
});

test("the response carries the full security header set — TLY-007-AC3", async ({ request }) => {
  const response = await request.get("/");
  const headers = response.headers();
  expect(headers["x-frame-options"]).toBe("DENY");
  expect(headers["x-content-type-options"]).toBe("nosniff");
  expect(headers["referrer-policy"]).toBe("strict-origin-when-cross-origin");
  expect(headers["permissions-policy"]).toBe("camera=(), microphone=(), geolocation=()");
  expect(headers["strict-transport-security"]).toBe("max-age=31536000; includeSubDomains; preload");
});

test("the served page has no off-origin script sources — TLY-007-AC3", async ({ page, baseURL }) => {
  await page.goto("/");
  const scriptSrcs = await page.locator("script[src]").evaluateAll((nodes) =>
    nodes.map((n) => (n as HTMLScriptElement).src),
  );
  const origin = new URL(baseURL ?? "http://localhost:3100").origin;
  for (const src of scriptSrcs) {
    expect(src.startsWith(origin) || src.startsWith("/")).toBe(true);
  }
});
