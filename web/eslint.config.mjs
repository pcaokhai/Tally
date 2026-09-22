import nextConfig from "eslint-config-next";

const rawValueBan = {
  files: ["app/**/*.{ts,tsx}", "components/**/*.{ts,tsx}", "mocks/**/*.{ts,tsx}"],
  rules: {
    "no-restricted-syntax": [
      "error",
      {
        selector: "Literal[value=/#[0-9a-fA-F]{3,8}\\b/]",
        message:
          "Raw hex colors are banned in components. Use a token from lib/tokens.ts or app/globals.css.",
      },
      {
        selector: "TemplateElement[value.raw=/#[0-9a-fA-F]{3,8}\\b/]",
        message:
          "Raw hex colors are banned in components. Use a token from lib/tokens.ts or app/globals.css.",
      },
      {
        selector: "Literal[value=/\\b(rgb|rgba|hsl|hsla|oklch)\\(/]",
        message:
          "Raw functional colors are banned in components. Use a token from lib/tokens.ts or app/globals.css.",
      },
      {
        selector: "TemplateElement[value.raw=/\\b(rgb|rgba|hsl|hsla|oklch)\\(/]",
        message:
          "Raw functional colors are banned in components. Use a token from lib/tokens.ts or app/globals.css.",
      },
      {
        selector: "Literal[value=/\\b\\d+(\\.\\d+)?m?s\\b/]",
        message:
          "Raw CSS durations are banned in components. Use a token from lib/motion.ts.",
      },
      {
        selector: "TemplateElement[value.raw=/\\b\\d+(\\.\\d+)?m?s\\b/]",
        message:
          "Raw CSS durations are banned in components. Use a token from lib/motion.ts.",
      },
      {
        selector: "Literal[value=/cubic-bezier\\(/]",
        message:
          "Raw easing functions are banned in components. Use a token from lib/motion.ts.",
      },
      {
        selector: "TemplateElement[value.raw=/cubic-bezier\\(/]",
        message:
          "Raw easing functions are banned in components. Use a token from lib/motion.ts.",
      },
      {
        selector: "Literal[value=/\\[[a-zA-Z-]*:?--[a-zA-Z0-9-]+\\]/]",
        message:
          "Tailwind bracket syntax on a bare custom property compiles to an invalid literal " +
          "(e.g. `height: --foo`), not `var(--foo)` — use the parenthesis form instead, e.g. " +
          "`h-(--layout-topbar-height)` not `h-[--layout-topbar-height]` (see ruling R7).",
      },
    ],
  },
};

const config = [
  { ignores: [".next/**", "lib/api/generated/**", "node_modules/**"] },
  ...nextConfig,
  rawValueBan,
];

export default config;
