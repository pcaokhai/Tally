export type ApiMode = "mock" | "live";

export function resolveApiMode(raw: string | undefined): ApiMode {
  if (raw === undefined) return "mock";
  if (raw === "mock" || raw === "live") return raw;
  throw new Error(
    `Invalid NEXT_PUBLIC_API_MODE: "${raw}" — must be "mock" or "live" (unset defaults to "mock")`,
  );
}

export const apiMode: ApiMode = resolveApiMode(process.env.NEXT_PUBLIC_API_MODE);

export const apiBaseUrl: string = apiMode === "live" ? "/api/bff" : "";
