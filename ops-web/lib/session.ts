export const IDLE_LIMIT_MS = 15 * 60 * 1000;
export const ABSOLUTE_LIMIT_MS = 8 * 60 * 60 * 1000;

export interface SessionClock {
  readonly now: number;
  readonly startedAt: number;
  readonly lastActivityAt: number;
}

export interface SessionRemaining {
  readonly idleMsLeft: number;
  readonly absoluteMsLeft: number;
  readonly msLeft: number;
  readonly expired: boolean;
}

function clamp(value: number, limit: number): number {
  return Math.min(limit, Math.max(0, value));
}

export function remainingSession({
  now,
  startedAt,
  lastActivityAt,
}: SessionClock): SessionRemaining {
  // Clamped both ways: a clock that jumps backwards must never show more time
  // left than the NFR-ADM-SEC-01 bounds allow (docs/02, R2 review MEDIUM #3).
  const idleMsLeft = clamp(IDLE_LIMIT_MS - (now - lastActivityAt), IDLE_LIMIT_MS);
  const absoluteMsLeft = clamp(ABSOLUTE_LIMIT_MS - (now - startedAt), ABSOLUTE_LIMIT_MS);
  const msLeft = Math.min(idleMsLeft, absoluteMsLeft);
  return {
    idleMsLeft,
    absoluteMsLeft,
    msLeft,
    expired: msLeft <= 0,
  };
}
