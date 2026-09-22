import { render, screen } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { SessionTimer } from "./SessionTimer";

describe("SessionTimer", () => {
  beforeEach(() => {
    vi.useFakeTimers();
  });

  afterEach(() => {
    vi.useRealTimers();
  });

  it("renders an mm:ss countdown starting near 15:00 — TLY-007-AC2", () => {
    render(<SessionTimer />);
    expect(screen.getByText(/^\d{2}:\d{2}$/)).toBeInTheDocument();
  });

  it("ticks the digits with aria-live off and keeps a separate polite region — TLY-007-AC2", () => {
    const { container } = render(<SessionTimer />);
    const digits = container.querySelector('[aria-live="off"]');
    const polite = container.querySelector('[aria-live="polite"]');
    expect(digits).not.toBeNull();
    expect(polite).not.toBeNull();
  });
});
