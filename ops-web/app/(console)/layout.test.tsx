import { render, screen } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import ConsoleLayout from "./layout";

vi.mock("next/navigation", () => ({
  usePathname: () => "/tenants",
}));

describe("ConsoleLayout", () => {
  beforeEach(() => {
    vi.useFakeTimers();
  });

  afterEach(() => {
    vi.useRealTimers();
  });

  it("the shell renders the badge and session timer on a section page — TLY-007-AC2", () => {
    render(
      <ConsoleLayout>
        <h1>Tenants</h1>
      </ConsoleLayout>,
    );

    expect(screen.getByRole("status")).toBeInTheDocument();
    expect(screen.getByText(/^\d{2}:\d{2}$/)).toBeInTheDocument();
    expect(screen.getByRole("heading", { name: "Tenants" })).toBeInTheDocument();
  });
});
