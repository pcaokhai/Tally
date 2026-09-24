import { http, HttpResponse } from "msw";
import { handlers as generatedHandlers } from "../lib/api/generated/msw-handlers";

// Hand-written fixture overrides go here, first — MSW resolves first-match-wins,
// so overrides listed before generatedHandlers take precedence over the stub responses.
// TLY-104-AC5: two fixture tenants so the tenant switcher has something to switch between.
const overrides: typeof generatedHandlers = [
  http.get("*/v1/me", () => {
    return HttpResponse.json(
      {
        user: { id: "user_1", email: "khai@example.com", name: "Khai" },
        active_tenant: { id: "tenant_a", name: "Acme Inc", role: "OWNER" },
        memberships: [
          { tenant_id: "tenant_a", tenant_name: "Acme Inc", role: "OWNER" },
          { tenant_id: "tenant_b", tenant_name: "Globex Corp", role: "ADMIN" },
        ],
        livemode: false,
      },
      { status: 200 },
    );
  }),
];

export const handlers = [...overrides, ...generatedHandlers];
