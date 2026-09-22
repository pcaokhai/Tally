import type { HttpHandler } from "msw";
import { handlers as generatedHandlers } from "@/lib/api/generated/msw-handlers";

// Local fixture overrides for later stories (e.g. error states, pagination
// scenarios). Must precede generatedHandlers so msw matches them first.
const overrideHandlers: HttpHandler[] = [];

export const handlers: HttpHandler[] = [...overrideHandlers, ...generatedHandlers];
