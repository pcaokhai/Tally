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

export function remainingSession({
  now,
  startedAt,
  lastActivityAt,
}: SessionClock): SessionRemaining {
  const idleMsLeft = Math.max(0, IDLE_LIMIT_MS - (now - lastActivityAt));
  const absoluteMsLeft = Math.max(0, ABSOLUTE_LIMIT_MS - (now - startedAt));
  const msLeft = Math.min(idleMsLeft, absoluteMsLeft);
  return {
    idleMsLeft,
    absoluteMsLeft,
    msLeft,
    expired: msLeft <= 0,
  };
}
