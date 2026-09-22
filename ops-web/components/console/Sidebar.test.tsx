import { render, screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import { NAV_ITEMS } from "@/lib/nav";
import { Sidebar } from "./Sidebar";

vi.mock("next/navigation", () => ({
  usePathname: () => "/",
}));

describe("Sidebar", () => {
  it("renders the 14 ops sidebar items — TLY-007-AC1", () => {
    render(<Sidebar />);

    for (const item of NAV_ITEMS) {
      const expectedHref = item.slug === "overview" ? "/" : `/${item.slug}`;
      const link = screen.getByRole("link", { name: item.label });
      expect(link).toHaveAttribute("href", expectedHref);
    }

    expect(screen.getAllByRole("link")).toHaveLength(NAV_ITEMS.length);
  });
});
