import { ConsoleShell } from "@/components/console/ConsoleShell";
import { NotFoundContent } from "@/components/console/NotFoundContent";

/**
 * Global 404, for addresses outside the console route group. It supplies ConsoleShell
 * itself so the non-dismissible environment badge and the session timer survive a
 * mistyped URL or a dead link (TLY-007 AC2). A notFound() raised inside the group is
 * handled by app/(console)/not-found.tsx, which must NOT repeat the shell.
 */
export default function NotFound() {
  return (
    <ConsoleShell>
      <NotFoundContent />
    </ConsoleShell>
  );
}
