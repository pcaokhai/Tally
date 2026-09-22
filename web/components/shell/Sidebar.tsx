"use client";

import { usePathname } from "next/navigation";
import Link from "next/link";
import { navGroups, settingsItem, type NavItem } from "./nav-items";

function NavLink({ item, active }: { item: NavItem; active: boolean }) {
  return (
    <Link
      href={item.href}
      aria-current={active ? "page" : undefined}
      className={`flex h-[--layout-nav-item-height] items-center gap-[--spacing-10] rounded-[--radius-md] px-[--spacing-10] text-[length:--text-base] transition-colors duration-[--motion-duration-fast] ${
        active
          ? "bg-[--color-accent-wash] font-semibold text-[--color-accent-ink]"
          : "text-[--color-ink-muted] hover:bg-[--color-hover]"
      }`}
    >
      {item.icon}
      <span>{item.label}</span>
      {item.badge === "issues" ? (
        <span
          aria-label="Has issues"
          className="ml-auto h-2 w-2 rounded-full bg-[--color-danger]"
        />
      ) : null}
    </Link>
  );
}

export function Sidebar() {
  const pathname = usePathname();

  return (
    <aside className="flex w-[--layout-sidebar-width] shrink-0 flex-col gap-[--spacing-18] border-r border-[--color-line] bg-[--color-surface] p-[--spacing-20] px-[--spacing-14]">
      <div className="flex items-center gap-[--spacing-10] px-[--spacing-6]">
        <svg width="30" height="30" viewBox="0 0 30 30" aria-hidden="true">
          <rect width="30" height="30" rx="9" fill="var(--color-ink)" />
          <rect x="9" y="8" width="2" height="14" rx="1" fill="var(--color-on-ink)" />
          <rect x="14" y="11" width="2" height="11" rx="1" fill="var(--color-on-ink)" />
          <rect x="19" y="5" width="2" height="17" rx="1" fill="var(--color-accent-soft)" />
        </svg>
        <span className="font-[family-name:--font-serif] text-[length:--text-2xl]">Tally</span>
      </div>

      <button
        type="button"
        aria-label="Switch tenant, currently Acme Robotics"
        className="flex min-h-[52px] w-full items-center gap-[--spacing-10] rounded-[--radius-xl] border border-[--color-line] bg-[--color-raised] px-[--spacing-10]"
      >
        <span className="flex h-8 w-8 items-center justify-center rounded-[--radius-md] bg-[--color-success-bg] text-[12px] font-bold text-[--color-success-ink]">
          AR
        </span>
        <span className="flex flex-col items-start">
          <span className="text-[length:--text-base] font-semibold text-[--color-ink]">
            Acme Robotics
          </span>
          <span className="text-[length:--text-sm] text-[--color-ink-subtle]">Growth plan</span>
        </span>
        <svg
          className="ml-auto"
          width="14"
          height="14"
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          strokeWidth="1.7"
          aria-hidden="true"
        >
          <path d="m7 9 5-5 5 5M7 15l5 5 5-5" />
        </svg>
      </button>

      <nav aria-label="Main" className="flex flex-1 flex-col gap-[2px]">
        {navGroups.map((group, index) => (
          <div key={group.heading ?? `group-${index}`}>
            {group.heading ? (
              <span className="block px-[--spacing-10] pb-[--spacing-6] pt-[--spacing-14] text-[length:--text-xs] font-semibold tracking-[0.08em] text-[--color-ink-faint]">
                {group.heading}
              </span>
            ) : null}
            {group.items.map((item) => (
              <NavLink key={item.href} item={item} active={pathname === item.href} />
            ))}
          </div>
        ))}
      </nav>

      <NavLink item={settingsItem} active={pathname === settingsItem.href} />
    </aside>
  );
}
