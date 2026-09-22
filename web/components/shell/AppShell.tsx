"use client";

import { useState, type ReactNode } from "react";
import { Sidebar } from "./Sidebar";
import { Topbar } from "./Topbar";
import { TestModeBanner } from "./TestModeBanner";

export function AppShell({ children }: { children: ReactNode }) {
  const [testMode, setTestMode] = useState(false);

  return (
    <div className="flex min-h-screen">
      <a
        href="#main-content"
        className="sr-only focus:not-sr-only focus:absolute focus:left-(--spacing-14) focus:top-(--spacing-14) focus:z-50 focus:rounded-(--radius-md) focus:bg-(--color-ink) focus:px-(--spacing-14) focus:py-(--spacing-8) focus:text-(--color-on-ink)"
      >
        Skip to content
      </a>
      <Sidebar />
      <div className="flex flex-1 flex-col">
        {testMode ? <TestModeBanner /> : null}
        <Topbar testMode={testMode} onTestModeChange={setTestMode} />
        <main id="main-content" className="flex-1 bg-(--color-ground) p-(--spacing-32)">
          {children}
        </main>
      </div>
    </div>
  );
}
