import { describe, expect, it } from "vitest";
import { placeholder } from "./index";

describe("placeholder", () => {
  it("returns a string — TLY-001-AC1", () => {
    expect(typeof placeholder()).toBe("string");
  });
});
