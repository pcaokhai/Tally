import { readFileSync } from "node:fs";
import { join } from "node:path";
import { describe, expect, it } from "vitest";
import { color, radius } from "./tokens";
import { duration, ease, spring } from "./motion";

describe("design and motion tokens", () => {
  it("exposes the docs/02 §7.8 motion durations — TLY-006-AC2", () => {
    expect(duration.fast).toBe("120ms");
    expect(duration.base).toBe("200ms");
    expect(duration.slow).toBe("350ms");
    expect(ease.standard).toBe("cubic-bezier(0.2, 0, 0, 1)");
    expect(ease.exit).toBe("cubic-bezier(0.4, 0, 1, 1)");
    expect(spring.soft).toEqual({ stiffness: 260, damping: 26 });
    expect(spring.snappy).toEqual({ stiffness: 500, damping: 30 });
  });

  it("exposes the canvas design tokens — TLY-006-AC2", () => {
    expect(color.accent).toBe("#4B3FD1");
    expect(color.ground).toBe("#F7F6F3");
    expect(color.ink).toBe("#16161A");
    expect(radius.md).toBe("8px");
    expect(radius.full).toBe("999px");
  });

  it("keeps every token value in one module — TLY-006-AC2", () => {
    const cssPath = join(process.cwd(), "app/globals.css");
    const css = readFileSync(cssPath, "utf-8");
    expect(css).toContain("#4b3fd1");
    expect(css).toContain("200ms");
  });
});
