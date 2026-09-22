import { describe, expect, it } from "vitest";
import { presentationFor } from "./environment";

describe("presentationFor", () => {
  it("labels PRODUCTION with the danger token — TLY-007-AC2", () => {
    expect(presentationFor("PRODUCTION")).toEqual({
      label: "PRODUCTION",
      tokenClass: "bg-danger text-canvas",
    });
  });

  it("labels STAGING with the warn token — TLY-007-AC2", () => {
    expect(presentationFor("STAGING")).toEqual({
      label: "STAGING",
      tokenClass: "bg-warn text-canvas",
    });
  });

  it("labels LOCAL with a neutral token — TLY-007-AC2", () => {
    expect(presentationFor("LOCAL").tokenClass).toContain("hairline");
  });
});
