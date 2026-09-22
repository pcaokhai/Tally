import { test, expect } from "@playwright/test";

// Clicking the test-mode switch slides its knob via `transform: translateX(...)`
// and toggles a CSS `transition-transform`. Under `prefers-reduced-motion: reduce`
// the global rule in app/globals.css forces every transition/animation duration
// to 0.01ms, so the same interaction must produce no meaningfully-running
// transform animation. The companion control test (run in the default-motion
// project) proves the interaction DOES animate normally — without it, the
// reduced-motion assertion could pass simply because nothing animates at all.

test("runs no transform animations under prefers-reduced-motion — TLY-006-AC4", async ({
  page,
  browserName: _browserName,
}, testInfo) => {
  test.skip(testInfo.project.name !== "reduced-motion", "only runs under the reduced-motion project");

  await page.goto("/");
  const knob = page.locator('button[role="switch"] > span[aria-hidden="true"]');
  await knob.waitFor();

  await page.locator('button[role="switch"]').click();

  const transformAnimations = await page.evaluate(async () => {
    const animations = document
      .getAnimations()
      .filter((animation) =>
        (animation.effect as KeyframeEffect | null)
          ?.getKeyframes()
          .some((frame) => "transform" in frame),
      );
    await Promise.all(animations.map((animation) => animation.finished.catch(() => undefined)));
    return animations.map((animation) => {
      const timing = animation.effect?.getComputedTiming();
      return {
        playState: animation.playState,
        duration: typeof timing?.duration === "number" ? timing.duration : 0,
      };
    });
  });

  for (const animation of transformAnimations) {
    expect(animation.duration).toBeLessThanOrEqual(1);
    expect(animation.playState).not.toBe("running");
  }

  const knobTransitionDuration = await knob.evaluate(
    (el) => getComputedStyle(el).transitionDuration,
  );
  const seconds = Number.parseFloat(knobTransitionDuration);
  expect(seconds).toBeLessThanOrEqual(0.001);
});

test("runs transform animations when motion is allowed — TLY-006-AC4", async ({
  page,
}, testInfo) => {
  test.skip(testInfo.project.name !== "chromium", "control test — default motion project only");

  await page.goto("/");
  const knob = page.locator('button[role="switch"] > span[aria-hidden="true"]');
  await knob.waitFor();

  await page.locator('button[role="switch"]').click();

  const knobTransitionDuration = await knob.evaluate(
    (el) => getComputedStyle(el).transitionDuration,
  );
  const seconds = Number.parseFloat(knobTransitionDuration);
  expect(seconds).toBeGreaterThan(0.001);
});
