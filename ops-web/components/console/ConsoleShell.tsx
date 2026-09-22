import { EnvironmentBadge } from "@/components/console/EnvironmentBadge";
import { SessionTimer } from "@/components/console/SessionTimer";
import { Sidebar } from "@/components/console/Sidebar";

/**
 * The operator console chrome: environment badge, session timer, ops sidebar.
 *
 * NFR-ADM-UX-02 / TLY-007 AC2 require the badge and timer on *every* page, so this
 * lives in one component used by both the console layout and the global not-found
 * page. A page rendered outside this shell is an AC2 violation (R2 review CRITICAL).
 */
export function ConsoleShell({ children }: { children: React.ReactNode }) {
  return (
    <div className="flex min-h-screen flex-col">
      <EnvironmentBadge />
      <div className="flex h-[40px] items-center justify-end border-b border-hairline px-3">
        <SessionTimer />
      </div>
      <div className="flex flex-1">
        <Sidebar />
        <main className="flex-1 p-4">{children}</main>
      </div>
    </div>
  );
}
