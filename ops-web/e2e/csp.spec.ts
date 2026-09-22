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

test("the inline bootstrap scripts carry the CSP nonce — TLY-007-AC3", async ({ request }) => {
  const response = await request.get("/");
  const csp = response.headers()["content-security-policy"] ?? "";
  const nonceMatch = csp.match(/'nonce-([^']+)'/);
  expect(nonceMatch).not.toBeNull();
  const nonce = nonceMatch?.[1];

  const html = await response.text();
  const inlineScripts = [...html.matchAll(/<script(?![^>]*\bsrc=)([^>]*)>/g)];
  expect(inlineScripts.length).toBeGreaterThan(0);
  for (const [, attrs] of inlineScripts) {
    expect(attrs).toContain(`nonce="${nonce}"`);
  }
});

test("the console hydrates under the strict CSP — TLY-007-AC2", async ({ page }) => {
  const consoleMessages: string[] = [];
  const pageErrors: string[] = [];
  page.on("console", (msg) => consoleMessages.push(msg.text()));
  page.on("pageerror", (error) => pageErrors.push(error.message));

  await page.goto("/");

  const timer = page.getByText(/^\d{2}:\d{2}$/);
  const firstReading = await timer.textContent();
  await page.waitForTimeout(1500);
  const secondReading = await timer.textContent();

  expect(pageErrors).toEqual([]);
  const cspViolations = consoleMessages.filter((m) => /content security policy|refused to/i.test(m));
  expect(cspViolations).toEqual([]);
  expect(secondReading).not.toBe(firstReading);
});
