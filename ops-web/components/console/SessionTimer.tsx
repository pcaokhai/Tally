"use client";

import { useEffect, useState } from "react";
import { remainingSession } from "@/lib/session";

const WARN_THRESHOLD_MS = 2 * 60 * 1000;
const TICK_MS = 1000;

function formatClock(msLeft: number): string {
  const totalSeconds = Math.max(0, Math.ceil(msLeft / 1000));
  const minutes = Math.floor(totalSeconds / 60);
  const seconds = totalSeconds % 60;
  return `${String(minutes).padStart(2, "0")}:${String(seconds).padStart(2, "0")}`;
}

export function SessionTimer() {
  // TODO(TLY-802): seed startedAt/lastActivityAt from the real operator session
  const [startedAt] = useState(() => Date.now());
  const [lastActivityAt, setLastActivityAt] = useState(startedAt);
  const [now, setNow] = useState(startedAt);

  useEffect(() => {
    const interval = window.setInterval(() => setNow(Date.now()), TICK_MS);
    return () => window.clearInterval(interval);
  }, []);

  useEffect(() => {
    const onActivity = () => setLastActivityAt(Date.now());
    window.addEventListener("pointerdown", onActivity);
    window.addEventListener("keydown", onActivity);
    return () => {
      window.removeEventListener("pointerdown", onActivity);
      window.removeEventListener("keydown", onActivity);
    };
  }, []);

  const { msLeft } = remainingSession({ now, startedAt, lastActivityAt });
  const isWarning = msLeft <= WARN_THRESHOLD_MS;

  return (
    <div className="flex items-center gap-2">
      <span className="font-mono-ops text-[10px] uppercase tracking-widest text-hairline">
        Session
      </span>
      <span
        aria-live="off"
        className={`font-mono-ops text-sm ${isWarning ? "text-warn" : ""}`}
      >
        {formatClock(msLeft)}
      </span>
      <span aria-live="polite" className="sr-only">
        {isWarning ? "Session expiring soon" : ""}
      </span>
    </div>
  );
}
