import { test, expect } from "@playwright/test";

// Regression net for the class of bug in ruling R7: Tailwind v4 bracket syntax on a
// bare custom property (`h-[--layout-topbar-height]`) compiles to a literal, invalid
// CSS value (`height: --layout-topbar-height`) instead of `var(...)`, so the browser
// silently falls back to `initial` — no error, no visual signal in a quick look.
// Only the parenthesis form (`h-(--layout-topbar-height)`) auto-wraps in `var(...)`.
// This asserts the shell's computed styles are real resolved values, not `initial`.

test("resolves every design token to a real computed value — TLY-006-AC2", async ({ page }, testInfo) => {
  // Run once per distinct browser engine, not gated to a specific project name (F6) —
  // a renamed project would otherwise silently drop this coverage.
  test.skip(testInfo.project.use.reducedMotion === "reduce", "one non-reduced-motion project is enough");

  await page.goto("/");

  const sidebar = page.locator("aside");
  const sidebarBg = await sidebar.evaluate((el) => getComputedStyle(el).backgroundColor);
  expect(sidebarBg).not.toBe("rgba(0, 0, 0, 0)");
  expect(sidebarBg).not.toBe("");

  const activeNavItem = page.locator('a[aria-current="page"]');
  const activeBg = await activeNavItem.evaluate((el) => getComputedStyle(el).backgroundColor);
  expect(activeBg).not.toBe("rgba(0, 0, 0, 0)");
  expect(activeBg).not.toBe("");

  const topbar = page.locator("header");
  const topbarHeight = await topbar.evaluate((el) => getComputedStyle(el).height);
  expect(Number.parseFloat(topbarHeight)).toBeGreaterThan(0);

  const knob = page.locator('button[role="switch"] > span[aria-hidden="true"]');
  const knobDuration = await knob.evaluate((el) => getComputedStyle(el).transitionDuration);
  expect(Number.parseFloat(knobDuration)).toBeGreaterThan(0);

  const wordmark = page.getByText("Tally", { exact: true });
  const wordmarkFont = await wordmark.evaluate((el) => getComputedStyle(el).fontSize);
  expect(Number.parseFloat(wordmarkFont)).toBeGreaterThan(0);
});
