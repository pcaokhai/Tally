"use client";

import { useEffect, useState } from "react";
import { API_MODE } from "@/lib/api/mode";

interface MockProviderProps {
  readonly children: React.ReactNode;
}

export function MockProvider({ children }: MockProviderProps): React.ReactElement {
  const [ready, setReady] = useState(API_MODE !== "mock");

  useEffect(() => {
    if (API_MODE !== "mock") {
      return;
    }
    let cancelled = false;
    import("@/mocks/browser").then(({ worker }) =>
      worker.start({ onUnhandledRequest: "bypass" }).then(() => {
        if (!cancelled) {
          setReady(true);
        }
      }),
    );
    return () => {
      cancelled = true;
    };
  }, []);

  if (!ready) {
    return <></>;
  }
  return <>{children}</>;
}
