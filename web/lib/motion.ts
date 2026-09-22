// The ONLY place animation values may be written (web/CLAUDE.md). Mirrors
// app/globals.css and docs/02-software-architecture.md §7.8.

export const duration = {
  fast: "120ms",
  base: "200ms",
  slow: "350ms",
} as const;

export const ease = {
  standard: "cubic-bezier(0.2, 0, 0, 1)",
  exit: "cubic-bezier(0.4, 0, 1, 1)",
} as const;

export const spring = {
  soft: { stiffness: 260, damping: 26 },
  snappy: { stiffness: 500, damping: 30 },
} as const;

export const stagger = {
  delay: "35ms",
  maxItems: 8,
} as const;
