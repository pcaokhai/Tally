"use client";

import { useQuery, useQueryClient } from "@tanstack/react-query";
import { useState } from "react";
import { resetQueryClientForTenantSwitch } from "@/lib/queryClient";

interface Membership {
  tenant_id: string;
  tenant_name: string;
  role: string;
}

interface Me {
  active_tenant: { id: string; name: string; role: string };
  memberships: Membership[];
}

async function fetchMe(): Promise<Me> {
  const res = await fetch("/v1/me");
  if (!res.ok) throw new Error(`GET /v1/me failed: ${res.status}`);
  return (await res.json()) as Me;
}

// switchTenant/reload are injectable for tests; production code uses fetch + a
// hard reload so the very first render of the new tenant starts from a clean
// module/browser state (NFR-SEC-01), not just a cleared query cache.
export interface TenantSwitcherProps {
  switchTenant?: (tenantId: string) => Promise<void>;
  reload?: () => void;
}

export function TenantSwitcher({
  switchTenant = defaultSwitchTenant,
  reload = () => window.location.reload(),
}: TenantSwitcherProps) {
  const queryClient = useQueryClient();
  const [pending, setPending] = useState(false);
  const { data, isLoading, error } = useQuery({ queryKey: ["me"], queryFn: fetchMe });

  if (isLoading) return <span className="text-(length:--text-sm)">Loading tenant…</span>;
  if (error || !data) return <span className="text-(length:--text-sm) text-(--color-danger)">Tenant unavailable</span>;

  async function handleSelect(tenantId: string) {
    if (tenantId === data?.active_tenant.id || pending) return;
    setPending(true);
    try {
      await switchTenant(tenantId);
      // TLY-104-AC3: clear every query cache before the new tenant renders.
      resetQueryClientForTenantSwitch(queryClient);
      reload();
    } finally {
      setPending(false);
    }
  }

  return (
    <label className="flex items-center gap-(--spacing-8) text-(length:--text-sm)">
      <span className="sr-only">Active tenant</span>
      <select
        value={data.active_tenant.id}
        disabled={pending}
        onChange={(event) => handleSelect(event.target.value)}
        className="rounded-(--radius-md) border border-(--color-line) bg-(--color-surface) px-(--spacing-8) py-(--spacing-6)"
      >
        {data.memberships.map((membership) => (
          <option key={membership.tenant_id} value={membership.tenant_id}>
            {membership.tenant_name}
          </option>
        ))}
      </select>
    </label>
  );
}

async function defaultSwitchTenant(tenantId: string): Promise<void> {
  const res = await fetch("/switch-tenant", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ tenantId }),
  });
  if (!res.ok) throw new Error(`switch-tenant failed: ${res.status}`);
}
