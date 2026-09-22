import { describe, expect, it } from "vitest";
import { isNavSlug, NAV_ITEMS } from "./nav";

describe("NAV_ITEMS", () => {
  it("has exactly 14 items with unique slugs — TLY-007-AC1", () => {
    expect(NAV_ITEMS).toHaveLength(14);
    expect(new Set(NAV_ITEMS.map((item) => item.slug)).size).toBe(14);
  });

  it("groups items into Fleet, Operations, Configuration, Governance — TLY-007-AC1", () => {
    const groups = new Set(NAV_ITEMS.map((item) => item.group));
    expect(groups).toEqual(
      new Set(["Fleet", "Operations", "Configuration", "Governance"]),
    );
  });
});

describe("isNavSlug", () => {
  it("returns true for a known slug — TLY-007-AC1", () => {
    expect(isNavSlug("tenants")).toBe(true);
  });

  it("returns false for an unknown slug — TLY-007-AC1", () => {
    expect(isNavSlug("not-a-route")).toBe(false);
  });
});
