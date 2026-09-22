export type NavGroup = "Fleet" | "Operations" | "Configuration" | "Governance";

export interface NavItem {
  readonly slug: string;
  readonly label: string;
  readonly group: NavGroup;
}

export const NAV_ITEMS: readonly NavItem[] = [
  { slug: "overview", label: "Overview", group: "Fleet" },
  { slug: "tenants", label: "Tenants", group: "Fleet" },
  { slug: "health", label: "Health", group: "Fleet" },
  { slug: "fleet", label: "Fleet", group: "Fleet" },
  { slug: "jobs", label: "Jobs", group: "Operations" },
  { slug: "recon", label: "Reconciliation", group: "Operations" },
  { slug: "refunds", label: "Refunds", group: "Operations" },
  { slug: "ledger", label: "Ledger", group: "Operations" },
  { slug: "plans", label: "Plan versions", group: "Configuration" },
  { slug: "flags", label: "Feature flags", group: "Configuration" },
  { slug: "approvals", label: "Approvals", group: "Governance" },
  { slug: "impersonation", label: "Impersonation", group: "Governance" },
  { slug: "audit", label: "Audit log", group: "Governance" },
  { slug: "operators", label: "Operators", group: "Governance" },
] as const;

const NAV_SLUGS = new Set(NAV_ITEMS.map((item) => item.slug));

export function isNavSlug(s: string): boolean {
  return NAV_SLUGS.has(s);
}
