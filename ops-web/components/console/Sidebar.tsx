"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { NAV_ITEMS, type NavGroup } from "@/lib/nav";

const GROUPS: readonly NavGroup[] = [
  "Fleet",
  "Operations",
  "Configuration",
  "Governance",
];

function hrefFor(slug: string): string {
  return slug === "overview" ? "/" : `/${slug}`;
}

export function Sidebar() {
  const pathname = usePathname();

  return (
    <nav aria-label="Operator console" className="border-r border-hairline">
      <ul>
        {GROUPS.map((group) => (
          <li key={group}>
            <span className="block px-3 pt-3 pb-1 font-mono-ops text-[10px] uppercase tracking-widest text-hairline">
              {group}
            </span>
            <ul>
              {NAV_ITEMS.filter((item) => item.group === group).map((item) => {
                const href = hrefFor(item.slug);
                const isActive = pathname === href;
                return (
                  <li key={item.slug}>
                    <Link
                      href={href}
                      aria-current={isActive ? "page" : undefined}
                      className={`flex h-[30px] items-center border-l-2 px-3 text-sm focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-signal ${
                        isActive
                          ? "border-signal text-signal"
                          : "border-transparent"
                      }`}
                    >
                      {item.label}
                    </Link>
                  </li>
                );
              })}
            </ul>
          </li>
        ))}
      </ul>
    </nav>
  );
}
