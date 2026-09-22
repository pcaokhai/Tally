import { readFileSync } from "node:fs";
import { join } from "node:path";
import { describe, expect, it } from "vitest";
import { color, fontSize, layout, radius, shadow, spacing } from "./tokens";
import { duration, ease, spring, stagger } from "./motion";

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
    // globals.css wraps the accent-raise shadow across two lines; collapse
    // whitespace so a textual `toContain` still matches its logical value.
    const css = readFileSync(cssPath, "utf-8").replace(/\s+/g, " ");

    // Directly textually matchable categories: CSS uses lowercase hex, ours is
    // uppercase, so lowercase before comparing.
    for (const value of Object.values(color)) {
      expect(css).toContain(value.toLowerCase());
    }
    for (const value of Object.values(fontSize)) {
      expect(css).toContain(value);
    }
    for (const value of Object.values(radius)) {
      expect(css).toContain(value);
    }
    for (const value of Object.values(spacing)) {
      expect(css).toContain(value);
    }
    for (const value of Object.values(layout)) {
      expect(css).toContain(value);
    }
    for (const value of Object.values(shadow)) {
      expect(css).toContain(value.replace(/\s+/g, " "));
    }
    for (const value of Object.values(duration)) {
      expect(css).toContain(value);
    }
    for (const value of Object.values(ease)) {
      expect(css).toContain(value);
    }

    // Springs/stagger have no single CSS value to match textually — globals.css
    // stores them as split :root custom properties (`--motion-spring-soft-stiffness`,
    // `--motion-spring-soft-damping`, ...), not one composite value like `lib/motion.ts`'s
    // `{ stiffness, damping }` object. Assert each numeric field individually instead.
    expect(css).toContain(`--motion-spring-soft-stiffness: ${spring.soft.stiffness};`);
    expect(css).toContain(`--motion-spring-soft-damping: ${spring.soft.damping};`);
    expect(css).toContain(`--motion-spring-snappy-stiffness: ${spring.snappy.stiffness};`);
    expect(css).toContain(`--motion-spring-snappy-damping: ${spring.snappy.damping};`);
    expect(css).toContain(`--motion-stagger: ${stagger.delay};`);
    expect(css).toContain(`--motion-stagger-max-items: ${stagger.maxItems};`);
  });
});
