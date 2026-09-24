import { QueryClient } from "@tanstack/react-query";

// One QueryClient per browser tab, held in module scope so it survives
// re-renders but can still be torn down wholesale on tenant switch.
export function createQueryClient(): QueryClient {
  return new QueryClient({
    defaultOptions: { queries: { staleTime: 30_000 } },
  });
}

let browserQueryClient: QueryClient | undefined;

export function getQueryClient(): QueryClient {
  if (typeof window === "undefined") {
    return createQueryClient();
  }
  if (!browserQueryClient) {
    browserQueryClient = createQueryClient();
  }
  return browserQueryClient;
}

// TLY-104-AC3: tenant switch clears every query cache and in-memory store
// before the new tenant renders (NFR-SEC-01).
export function resetQueryClientForTenantSwitch(client: QueryClient): void {
  client.clear();
}
