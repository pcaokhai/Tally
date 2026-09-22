import { expect, test } from "@playwright/test";

/**
 * AC4's node-side tests prove the handler set is correct; they cannot prove the browser
 * path. This drives a real tab: the MSW service worker must register under the strict
 * CSP (`worker-src 'self'`) and intercept a client-side /ops/v1 fetch.
 */
test("the msw worker intercepts an ops request in the browser — TLY-007-AC4", async ({
  page,
}) => {
  await page.goto("/");

  // MockProvider starts the worker in an effect, so poll rather than race it.
  await expect
    .poll(
      async () =>
        page.evaluate(async () => {
          const response = await fetch("/ops/v1/overview");
          return response.status;
        }),
      { timeout: 15_000 },
    )
    .toBe(200);

  const body = await page.evaluate(async () => {
    const response = await fetch("/ops/v1/overview");
    return response.json();
  });
  expect(body).toBeTruthy();
});

test("registering the mock worker raises no CSP violation — TLY-007-AC4", async ({
  page,
}) => {
  const messages: string[] = [];
  page.on("console", (msg) => messages.push(msg.text()));

  await page.goto("/");
  await page.waitForFunction(() => navigator.serviceWorker.controller !== null, {
    timeout: 15_000,
  });

  const violations = messages.filter((m) =>
    /content security policy|refused to/i.test(m),
  );
  expect(violations).toEqual([]);
});
