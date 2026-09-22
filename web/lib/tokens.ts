// Typed mirror of app/globals.css `@theme` — the design tokens from the
// canvas artboard `Main / Home (shell)`. Keep in sync with globals.css.

export const color = {
  ground: "#F7F6F3",
  surface: "#FBFAF8",
  raised: "#FFFFFF",
  ink: "#16161A",
  inkMuted: "#3D3B44",
  inkSubtle: "#5E5C66",
  inkFaint: "#6B6973",
  line: "#E7E5E0",
  lineStrong: "#E2E0DA",
  hover: "#EFEDE8",
  hoverSoft: "#F1EFEA",
  rowHover: "#FAF9F6",
  accent: "#4B3FD1",
  accentHover: "#3F33C4",
  accentInk: "#3A2FB8",
  accentWash: "#EEEBFF",
  accentSoft: "#9C92F5",
  warnBg: "#FDF1DE",
  warnInk: "#7A4500",
  warnLine: "#F3DDB5",
  warnAccent: "#E39A2D",
  danger: "#D9432F",
  successBg: "#E2F0EA",
  successInk: "#0F6B45",
  onAccent: "#FFFFFF",
  onInk: "#FBFAF8",
} as const;

export const fontSize = {
  xs: "11px",
  sm: "12px",
  smPlus: "12.5px",
  base: "13.5px",
  body: "14px",
  bodyLg: "14.5px",
  lg: "17px",
  xl: "20px",
  "2xl": "26px",
  "3xl": "30px",
  display: "40px",
} as const;

export const radius = {
  sm: "6px",
  md: "8px",
  lg: "10px",
  xl: "12px",
  "2xl": "14px",
  full: "999px",
} as const;

export const spacing = {
  2: "2px",
  4: "4px",
  6: "6px",
  8: "8px",
  10: "10px",
  12: "12px",
  14: "14px",
  16: "16px",
  18: "18px",
  20: "20px",
  24: "24px",
  28: "28px",
  32: "32px",
} as const;

export const layout = {
  sidebarWidth: "240px",
  topbarHeight: "64px",
  testBandHeight: "34px",
  navItemHeight: "38px",
} as const;

export const shadow = {
  cardHover: "0 12px 28px -16px rgba(22, 22, 26, 0.28)",
  accentRaise:
    "0 1px 0 rgba(255, 255, 255, 0.2) inset, 0 6px 16px -8px rgba(75, 63, 209, 0.8)",
} as const;
