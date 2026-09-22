import { describe, expect, it } from "vitest";
import { resolveApiMode } from "./mode";

describe("resolveApiMode", () => {
  it("defaults to mock mode when NEXT_PUBLIC_API_MODE is unset — TLY-006-AC3", () => {
    expect(resolveApiMode(undefined)).toBe("mock");
  });

  it("uses the BFF base url in live mode — TLY-006-AC3", () => {
    expect(resolveApiMode("live")).toBe("live");
  });

  it("rejects an unknown api mode — TLY-006-AC3", () => {
    expect(() => resolveApiMode("staging")).toThrow();
  });
});
