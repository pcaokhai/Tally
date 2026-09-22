import type { paths } from "./generated/schema";
import { apiBaseUrl } from "./mode";

const REQUEST_TIMEOUT_MS = 10_000;

type GetPaths = { [K in keyof paths]: paths[K] extends { get: unknown } ? K : never }[keyof paths];

// ponytail: minimal typed GET wrapper, only what the shell needs today.
// Widen to POST/PATCH/DELETE (or swap in openapi-fetch) when a screen needs writes.
export async function apiGet<Path extends GetPaths & string>(path: Path): Promise<unknown> {
  const res = await fetch(`${apiBaseUrl}${path}`, {
    signal: AbortSignal.timeout(REQUEST_TIMEOUT_MS),
  });
  if (!res.ok) throw new Error(`GET ${path} failed: ${res.status}`);
  return res.json();
}
