import type { ReactNode } from "react";
import { AppShell } from "@/components/shell/AppShell";
import { MockProvider } from "@/components/MockProvider";

export default function AppLayout({ children }: { children: ReactNode }) {
  return (
    <MockProvider>
      <AppShell>{children}</AppShell>
    </MockProvider>
  );
}
