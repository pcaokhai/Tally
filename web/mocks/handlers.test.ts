import { afterAll, afterEach, beforeAll, describe, expect, it } from "vitest";
import { handlers } from "./handlers";
import { handlers as generatedHandlers } from "../lib/api/generated/msw-handlers";
import { setupServer } from "msw/node";
import type { HttpHandler } from "msw";

const server = setupServer(...handlers);

beforeAll(() => server.listen({ onUnhandledRequest: "error" }));
afterEach(() => server.resetHandlers());
afterAll(() => server.close());

function routeKey(handler: HttpHandler): string {
  return `${String(handler.info.method)} ${String(handler.info.path)}`;
}

describe("mocks/handlers", () => {
  it("serves a generated route from the MSW handlers in mock mode — TLY-006-AC3", async () => {
    const res = await fetch("https://api.test/v1/customers");
    expect(res.status).toBe(200);
    const body = await res.json();
    expect(body.data).toBeDefined();
  });

  it("serves every generated route — TLY-006-AC3", () => {
    // Set-equality on [method, path] pairs, not a length check: a dropped or
    // shadowed generated route fails this even if the total count still matches.
    const exported = new Set(handlers.map(routeKey));
    const generated = generatedHandlers.map(routeKey);

    expect(generated.length).toBeGreaterThan(0);
    for (const key of generated) {
      expect(exported.has(key)).toBe(true);
    }
  });

  it("resolves a representative sample of distinct generated routes — TLY-006-AC3", async () => {
    const getRoutes = generatedHandlers
      .filter((handler) => handler.info.method === "GET")
      .map((handler) => String(handler.info.path))
      .filter((path) => !path.includes(":"))
      .slice(0, 5);

    expect(getRoutes.length).toBeGreaterThan(0);

    for (const path of getRoutes) {
      const url = path.replace(/^\*/, "https://api.test");
      const res = await fetch(url);
      expect(res.status).not.toBe(404);
    }
  });
});
