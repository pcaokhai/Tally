import { describe, expect, it } from "vitest";
import { ABSOLUTE_LIMIT_MS, IDLE_LIMIT_MS, remainingSession } from "./session";

describe("remainingSession", () => {
  it("expires at the 15 minute idle bound — TLY-007-AC2", () => {
    const startedAt = 0;
    const lastActivityAt = 0;
    const before = remainingSession({
      now: IDLE_LIMIT_MS - 1,
      startedAt,
      lastActivityAt,
    });
    expect(before.expired).toBe(false);

    const after = remainingSession({
      now: IDLE_LIMIT_MS,
      startedAt,
      lastActivityAt,
    });
    expect(after.expired).toBe(true);
    expect(after.idleMsLeft).toBe(0);
  });

  it("expires at the 8 hour absolute bound — TLY-007-AC2", () => {
    const startedAt = 0;
    const before = remainingSession({
      now: ABSOLUTE_LIMIT_MS - 1,
      startedAt,
      lastActivityAt: ABSOLUTE_LIMIT_MS - 1,
    });
    expect(before.expired).toBe(false);

    const after = remainingSession({
      now: ABSOLUTE_LIMIT_MS,
      startedAt,
      lastActivityAt: ABSOLUTE_LIMIT_MS,
    });
    expect(after.expired).toBe(true);
    expect(after.absoluteMsLeft).toBe(0);
  });

  it("caps msLeft at the sooner of idle and absolute — TLY-007-AC2", () => {
    const result = remainingSession({
      now: ABSOLUTE_LIMIT_MS - 60_000,
      startedAt: 0,
      lastActivityAt: ABSOLUTE_LIMIT_MS - 60_000,
    });
    expect(result.msLeft).toBe(result.absoluteMsLeft);
  });
});
