import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render, screen, type RenderResult } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import userEvent from "@testing-library/user-event";
import { usePathname } from "next/navigation";
import type { ReactNode } from "react";
import { AppShell } from "./AppShell";

vi.mock("next/navigation", () => ({
  usePathname: vi.fn(() => "/"),
}));

const mockedUsePathname = vi.mocked(usePathname);

// TenantSwitcher (rendered inside AppShell's Topbar) needs a QueryClient in
// context; these tests don't exercise it, so a bare no-retry client is enough.
function renderShell(children: ReactNode): RenderResult {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return render(<QueryClientProvider client={client}>{children}</QueryClientProvider>);
}

describe("AppShell", () => {
  it("renders every sidebar nav item — TLY-006-AC1", () => {
    renderShell(<AppShell>content</AppShell>);
    const labels = [
      "Home",
      "Customers",
      "Subscriptions",
      "Invoices",
      "Payments",
      "Usage",
      "Products & pricing",
      "API keys",
      "Webhooks",
      "Events & logs",
      "Settings",
    ];
    for (const label of labels) {
      expect(screen.getByRole("link", { name: new RegExp(label) })).toBeInTheDocument();
    }
    expect(screen.getByText("BILLING")).toBeInTheDocument();
    expect(screen.getByText("DEVELOPERS")).toBeInTheDocument();
  });

  it("marks the active route with aria-current — TLY-006-AC1", () => {
    mockedUsePathname.mockReturnValue("/invoices");
    renderShell(<AppShell>content</AppShell>);
    expect(screen.getByRole("link", { name: /Invoices/ })).toHaveAttribute(
      "aria-current",
      "page",
    );
    expect(screen.getByRole("link", { name: /^Home$/ })).not.toHaveAttribute("aria-current");
  });

  it("renders the shell on a non-home route — TLY-006-AC1", () => {
    mockedUsePathname.mockReturnValue("/developers/webhooks");
    renderShell(<AppShell>content</AppShell>);
    expect(screen.getByRole("navigation", { name: "Main" })).toBeInTheDocument();
    expect(screen.getByRole("searchbox")).toBeInTheDocument();
  });

  it("renders the topbar search, test-mode switch, bell and avatar — TLY-006-AC1", () => {
    mockedUsePathname.mockReturnValue("/");
    renderShell(<AppShell>content</AppShell>);
    expect(screen.getByRole("searchbox")).toBeInTheDocument();
    expect(screen.getByRole("switch", { name: /test mode/i })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: /notifications/i })).toBeInTheDocument();
    expect(screen.getByText("LN")).toBeInTheDocument();
  });

  it("toggles test mode and shows the non-dismissible banner — TLY-006-AC1", async () => {
    mockedUsePathname.mockReturnValue("/");
    const user = userEvent.setup();
    renderShell(<AppShell>content</AppShell>);
    const toggle = screen.getByRole("switch", { name: /test mode/i });
    expect(toggle).toHaveAttribute("aria-checked", "false");
    await user.click(toggle);
    expect(toggle).toHaveAttribute("aria-checked", "true");
    expect(
      screen.getByText(/You're in test mode\. No real money moves\./),
    ).toBeInTheDocument();
    expect(screen.queryByRole("button", { name: /dismiss/i })).not.toBeInTheDocument();
  });

  it("exposes a skip link to main content — TLY-006-AC1", () => {
    mockedUsePathname.mockReturnValue("/");
    renderShell(<AppShell>content</AppShell>);
    const skipLink = screen.getByRole("link", { name: /skip to content/i });
    expect(skipLink).toHaveAttribute("href", "#main-content");
    expect(screen.getByRole("main")).toHaveAttribute("id", "main-content");
  });
});
