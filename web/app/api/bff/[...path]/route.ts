import { NextRequest } from "next/server";

const UPSTREAM_TIMEOUT_MS = 10_000;

function coreApiUrl(): string {
  const raw = process.env.CORE_API_URL ?? "http://localhost:8080";
  try {
    return new URL(raw).toString().replace(/\/$/, "");
  } catch {
    throw new Error(`Invalid CORE_API_URL: "${raw}"`);
  }
}

function problemJson(status: number, detail: string): Response {
  return new Response(JSON.stringify({ status, detail }), {
    status,
    headers: { "content-type": "application/problem+json" },
  });
}

async function forward(request: NextRequest, path: string[]): Promise<Response> {
  const upstreamUrl = `${coreApiUrl()}/${path.join("/")}${request.nextUrl.search}`;

  const headers = new Headers();
  const idempotencyKey = request.headers.get("Idempotency-Key");
  if (idempotencyKey) headers.set("Idempotency-Key", idempotencyKey);
  headers.set("content-type", request.headers.get("content-type") ?? "application/json");
  // TLY-104: attach the tenant's access token here once session handling lands.

  const hasBody = request.method !== "GET" && request.method !== "HEAD";

  let upstreamResponse: Response;
  try {
    upstreamResponse = await fetch(upstreamUrl, {
      method: request.method,
      headers,
      ...(hasBody ? { body: await request.text() } : {}),
      signal: AbortSignal.timeout(UPSTREAM_TIMEOUT_MS),
    });
  } catch {
    return problemJson(502, "The upstream service is unavailable.");
  }

  const responseBody = await upstreamResponse.text();
  return new Response(responseBody, {
    status: upstreamResponse.status,
    headers: {
      "content-type": upstreamResponse.headers.get("content-type") ?? "application/json",
    },
  });
}

export async function GET(request: NextRequest, { params }: { params: Promise<{ path: string[] }> }) {
  return forward(request, (await params).path);
}
export async function POST(request: NextRequest, { params }: { params: Promise<{ path: string[] }> }) {
  return forward(request, (await params).path);
}
export async function PATCH(request: NextRequest, { params }: { params: Promise<{ path: string[] }> }) {
  return forward(request, (await params).path);
}
export async function DELETE(request: NextRequest, { params }: { params: Promise<{ path: string[] }> }) {
  return forward(request, (await params).path);
}
