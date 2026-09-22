import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { EnvironmentBadge } from "./EnvironmentBadge";

describe("EnvironmentBadge", () => {
  it("PRODUCTION renders the red environment badge — TLY-007-AC2", () => {
    render(<EnvironmentBadge env="PRODUCTION" />);
    const badge = screen.getByRole("status");
    expect(badge).toHaveTextContent("PRODUCTION");
    expect(badge.className).toContain("bg-danger");
  });

  it("STAGING renders the amber environment badge — TLY-007-AC2", () => {
    render(<EnvironmentBadge env="STAGING" />);
    const badge = screen.getByRole("status");
    expect(badge).toHaveTextContent("STAGING");
    expect(badge.className).toContain("bg-warn");
  });

  it("the environment badge has no dismiss control — TLY-007-AC2", () => {
    render(<EnvironmentBadge env="PRODUCTION" />);
    expect(screen.queryAllByRole("button")).toHaveLength(0);
  });
});
