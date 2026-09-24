import type { ReactNode } from "react";
import { AppShell } from "@/components/shell/AppShell";
import { MockProvider } from "@/components/MockProvider";
import { QueryProvider } from "@/components/QueryProvider";

export default function AppLayout({ children }: { children: ReactNode }) {
  return (
    <MockProvider>
      <QueryProvider>
        <AppShell>{children}</AppShell>
      </QueryProvider>
    </MockProvider>
  );
}
