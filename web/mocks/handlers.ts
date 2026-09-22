import { handlers as generatedHandlers } from "../lib/api/generated/msw-handlers";

// Hand-written fixture overrides go here, first — MSW resolves first-match-wins,
// so overrides listed before generatedHandlers take precedence over the stub responses.
const overrides: typeof generatedHandlers = [];

export const handlers = [...overrides, ...generatedHandlers];
