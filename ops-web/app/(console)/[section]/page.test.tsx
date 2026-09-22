import { describe, expect, it } from "vitest";
import ConsoleSectionPage from "./page";

describe("ConsoleSectionPage", () => {
  it("renders a known section as a placeholder heading — TLY-007-AC1", async () => {
    const element = await ConsoleSectionPage({
      params: Promise.resolve({ section: "tenants" }),
    });
    expect(JSON.stringify(element)).toContain("Tenants");
  });

  it("rejects an unknown console section — TLY-007-AC1", async () => {
    await expect(
      ConsoleSectionPage({ params: Promise.resolve({ section: "bogus" }) }),
    ).rejects.toThrow();
  });
});
