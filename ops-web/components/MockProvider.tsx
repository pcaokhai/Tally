"use client";

import { useEffect } from "react";
import { API_MODE } from "@/lib/api/mode";

interface MockProviderProps {
  readonly children: React.ReactNode;
}

/**
 * Starts the MSW worker in mock mode. It deliberately does NOT gate rendering on the
 * worker being ready: blanking the tree until then left every page with an empty
 * server-rendered body, so the non-dismissible environment badge and the session timer
 * existed only after JavaScript ran — which breaks TLY-007 AC2 (NFR-ADM-UX-02).
 *
 * The shell has no data fetching, so nothing can race the worker today. A later story
 * that adds real /ops/v1 reads should gate at the data layer (TanStack Query), not by
 * withholding the whole document.
 */
export function MockProvider({ children }: MockProviderProps): React.ReactElement {
  useEffect(() => {
    if (API_MODE !== "mock") {
      return;
    }
    let cancelled = false;
    void import("@/mocks/browser").then(({ worker }) => {
      if (cancelled) {
        return;
      }
      return worker.start({ onUnhandledRequest: "bypass" });
    });
    return () => {
      cancelled = true;
    };
  }, []);

  return <>{children}</>;
}
