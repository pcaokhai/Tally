import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import HomePage from "./page";

describe("HomePage", () => {
  it("renders the app root — TLY-006-AC1", () => {
    render(<HomePage />);
    expect(screen.getByText("Tally")).toBeInTheDocument();
  });
});
