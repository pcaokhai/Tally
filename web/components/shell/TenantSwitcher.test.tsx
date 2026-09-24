import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, describe, expect, it, vi } from "vitest";
import { TenantSwitcher } from "./TenantSwitcher";

const me = {
  active_tenant: { id: "tenant_a", name: "Acme Inc", role: "OWNER" },
  memberships: [
    { tenant_id: "tenant_a", tenant_name: "Acme Inc", role: "OWNER" },
    { tenant_id: "tenant_b", tenant_name: "Globex Corp", role: "ADMIN" },
  ],
};

function renderSwitcher(client: QueryClient, props: Partial<React.ComponentProps<typeof TenantSwitcher>> = {}) {
  return render(
    <QueryClientProvider client={client}>
      <TenantSwitcher switchTenant={vi.fn().mockResolvedValue(undefined)} reload={vi.fn()} {...props} />
    </QueryClientProvider>,
  );
}

afterEach(() => {
  vi.unstubAllGlobals();
});

describe("TenantSwitcher — TLY-104-AC2 / AC5", () => {
  it("lists memberships from GET /v1/me for two fixture tenants", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn().mockResolvedValue({ ok: true, json: async () => me }),
    );
    const client = new QueryClient();
    renderSwitcher(client);

    expect(await screen.findByRole("option", { name: "Acme Inc" })).toBeInTheDocument();
    expect(await screen.findByRole("option", { name: "Globex Corp" })).toBeInTheDocument();
  });

  it("calls /auth/switch-tenant when a different tenant is chosen", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn().mockResolvedValue({ ok: true, json: async () => me }),
    );
    const client = new QueryClient();
    const switchTenant = vi.fn().mockResolvedValue(undefined);
    const reload = vi.fn();
    renderSwitcher(client, { switchTenant, reload });

    await screen.findByRole("option", { name: "Acme Inc" });
    await userEvent.selectOptions(screen.getByRole("combobox"), "tenant_b");

    await waitFor(() => expect(switchTenant).toHaveBeenCalledWith("tenant_b"));
    expect(reload).toHaveBeenCalled();
  });
});

describe("TenantSwitcher — TLY-104-AC3", () => {
  it("clears every query cache before reload when switching tenants", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn().mockResolvedValue({ ok: true, json: async () => me }),
    );
    const client = new QueryClient();
    client.setQueryData(["stale-tenant-a-data"], { leftover: true });
    expect(client.getQueryCache().getAll().length).toBeGreaterThan(0);

    renderSwitcher(client, { switchTenant: vi.fn().mockResolvedValue(undefined), reload: vi.fn() });
    await screen.findByRole("option", { name: "Acme Inc" });
    await userEvent.selectOptions(screen.getByRole("combobox"), "tenant_b");

    await waitFor(() => {
      const remaining = client.getQueryCache().getAll().filter((q) => q.queryKey[0] === "stale-tenant-a-data");
      expect(remaining).toHaveLength(0);
    });
  });
});
