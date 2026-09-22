import { afterAll, afterEach, beforeAll, describe, expect, it } from "vitest";
import { handlers as generatedHandlers } from "@/lib/api/generated/msw-handlers";
import { handlers } from "./handlers";
import { server } from "./server";

beforeAll(() => server.listen({ onUnhandledRequest: "error" }));
afterEach(() => server.resetHandlers());
afterAll(() => server.close());

describe("mock handlers", () => {
  it("the generated handlers answer the ops overview endpoint — TLY-007-AC4", async () => {
    const response = await fetch("http://localhost/ops/v1/overview");
    expect(response.status).toBe(200);
    const body = (await response.json()) as Record<string, unknown>;
    expect(body).toBeTypeOf("object");
  });

  it("the generated handlers answer the ops tenants endpoint — TLY-007-AC4", async () => {
    const response = await fetch("http://localhost/ops/v1/tenants");
    expect(response.status).toBe(200);
    const body = (await response.json()) as Record<string, unknown>;
    expect(body).toBeTypeOf("object");
  });

  it("every operator operation in the contract has a mock handler — TLY-007-AC4", () => {
    expect(generatedHandlers.length).toBeGreaterThan(40);
    expect(handlers.length).toBe(generatedHandlers.length);
  });

  it("an unhandled ops path is not silently mocked — TLY-007-AC4", async () => {
    await expect(fetch("http://localhost/ops/v1/definitely-not-a-real-path")).rejects.toThrow();
  });
});
