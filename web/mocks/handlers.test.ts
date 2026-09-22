import { afterAll, afterEach, beforeAll, describe, expect, it } from "vitest";
import { handlers } from "./handlers";
import { handlers as generatedHandlers } from "../lib/api/generated/msw-handlers";
import { setupServer } from "msw/node";

const server = setupServer(...handlers);

beforeAll(() => server.listen({ onUnhandledRequest: "error" }));
afterEach(() => server.resetHandlers());
afterAll(() => server.close());

describe("mocks/handlers", () => {
  it("serves a generated route from the MSW handlers in mock mode — TLY-006-AC3", async () => {
    const res = await fetch("https://api.test/v1/customers");
    expect(res.status).toBe(200);
    const body = await res.json();
    expect(body.data).toBeDefined();
  });

  it("keeps every generated handler — TLY-006-AC3", () => {
    expect(handlers.length).toBeGreaterThanOrEqual(generatedHandlers.length);
  });
});
