export type ApiMode = "mock" | "live";

function parseApiMode(value: string | undefined): ApiMode {
  if (value === undefined || value === "") {
    return "mock";
  }
  if (value === "mock" || value === "live") {
    return value;
  }
  throw new Error(`Unknown NEXT_PUBLIC_API_MODE value: ${value}`);
}

export const API_MODE: ApiMode = parseApiMode(process.env.NEXT_PUBLIC_API_MODE);
