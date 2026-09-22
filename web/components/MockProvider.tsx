"use client";

import { useEffect, useState, type ReactNode } from "react";
import { apiMode } from "@/lib/api/mode";

export function MockProvider({ children }: { children: ReactNode }) {
  const [ready, setReady] = useState(apiMode !== "mock");

  useEffect(() => {
    if (apiMode !== "mock") return;
    let cancelled = false;
    import("../mocks/browser").then(({ worker }) =>
      worker.start({ onUnhandledRequest: "bypass" }).then(() => {
        if (!cancelled) setReady(true);
      }),
    );
    return () => {
      cancelled = true;
    };
  }, []);

  if (!ready) return null;
  return <>{children}</>;
}
