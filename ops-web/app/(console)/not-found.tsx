import { NotFoundContent } from "@/components/console/NotFoundContent";

/**
 * 404 for a notFound() thrown inside the console group. It renders WITHOUT ConsoleShell
 * because (console)/layout.tsx already supplies it — the root not-found.tsx nested here
 * produced two environment badges and two sidebars (caught by the e2e 404 test).
 */
export default function ConsoleNotFound() {
  return <NotFoundContent />;
}
