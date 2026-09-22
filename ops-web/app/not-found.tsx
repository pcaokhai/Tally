import Link from "next/link";
import { ConsoleShell } from "@/components/console/ConsoleShell";

/**
 * Global 404. Rendered inside the console shell so the non-dismissible environment
 * badge and the session timer survive a mistyped URL or a dead link (TLY-007 AC2).
 */
export default function NotFound() {
  return (
    <ConsoleShell>
      <h1 className="text-sm uppercase tracking-widest text-hairline">
        Unknown console section
      </h1>
      <p className="mt-4 text-sm text-hairline">
        No operator screen is registered at this address.
      </p>
      <Link
        href="/"
        className="mt-6 inline-block text-sm text-signal underline underline-offset-4"
      >
        Return to overview
      </Link>
    </ConsoleShell>
  );
}
