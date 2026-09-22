import { afterEach, describe, expect, it, vi } from "vitest";

const ENV_KEY = "NEXT_PUBLIC_API_MODE";

describe("API_MODE", () => {
  afterEach(() => {
    delete process.env[ENV_KEY];
    vi.resetModules();
  });

  it("defaults to mock when unset — TLY-007-AC4", async () => {
    delete process.env[ENV_KEY];
    vi.resetModules();
    const { API_MODE } = await import("./mode");
    expect(API_MODE).toBe("mock");
  });

  it("accepts live — TLY-007-AC4", async () => {
    process.env[ENV_KEY] = "live";
    vi.resetModules();
    const { API_MODE } = await import("./mode");
    expect(API_MODE).toBe("live");
  });

  it("throws at module load on an unknown value — TLY-007-AC4", async () => {
    process.env[ENV_KEY] = "staging";
    vi.resetModules();
    await expect(import("./mode")).rejects.toThrow(/Unknown NEXT_PUBLIC_API_MODE value/);
  });
});
