import { EnvironmentBadge } from "@/components/console/EnvironmentBadge";
import { SessionTimer } from "@/components/console/SessionTimer";
import { Sidebar } from "@/components/console/Sidebar";

export default function ConsoleLayout({
  children,
}: {
  children: React.ReactNode;
}) {
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
