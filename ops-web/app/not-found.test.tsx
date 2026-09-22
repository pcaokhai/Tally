import { render, screen } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import NotFound from "./not-found";

vi.mock("next/navigation", () => ({
  usePathname: () => "/bogus",
}));

describe("NotFound", () => {
  beforeEach(() => {
    vi.useFakeTimers();
  });

  afterEach(() => {
    vi.useRealTimers();
  });

  it("keeps the environment badge and session timer on the 404 page — TLY-007-AC2", () => {
    render(<NotFound />);

    expect(screen.getByRole("status")).toBeInTheDocument();
    expect(screen.getByText(/^\d{2}:\d{2}$/)).toBeInTheDocument();
    expect(
      screen.getByRole("navigation", { name: "Operator console" }),
    ).toBeInTheDocument();
  });
});
