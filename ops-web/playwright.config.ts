import { defineConfig, devices } from "@playwright/test";

export default defineConfig({
  testDir: "e2e",
  fullyParallel: true,
  reporter: "list",
  use: {
    baseURL: "http://localhost:3100",
  },
  projects: [
    {
      name: "chromium",
      use: { ...devices["Desktop Chrome"] },
    },
  ],
  webServer: {
    command: "pnpm build && pnpm start",
    url: "http://localhost:3100",
    // ponytail: if a stray `pnpm dev` is already bound to :3100, this reuses it
    // instead of the strict-CSP production build — the CSP assertions below will
    // fail against dev's relaxed policy, confusingly. Kill anything on :3100 first.
    reuseExistingServer: !process.env.CI,
    timeout: 120_000,
  },
});
