const PRODUCTION_DIRECTIVES: Record<string, string[]> = {
  "default-src": ["'self'"],
  "frame-ancestors": ["'none'"],
  "object-src": ["'none'"],
  "base-uri": ["'self'"],
  "form-action": ["'self'"],
  "frame-src": ["'none'"],
  "img-src": ["'self'", "data:"],
  "font-src": ["'self'"],
  "connect-src": ["'self'"],
  "script-src": ["'self'"],
  "style-src": ["'self'"],
  "manifest-src": ["'self'"],
  "worker-src": ["'self'"],
};

export interface BuildCspOptions {
  isProduction: boolean;
}

export function buildDirectives({ isProduction }: BuildCspOptions): Record<string, string[]> {
  const directives: Record<string, string[]> = {};
  for (const [name, values] of Object.entries(PRODUCTION_DIRECTIVES)) {
    directives[name] = [...values];
  }

  if (!isProduction) {
    // ponytail: Next dev/Turbopack needs eval + inline styles for HMR; dev-only, gated below.
    directives["script-src"] = [...(directives["script-src"] ?? []), "'unsafe-eval'"];
    directives["style-src"] = [...(directives["style-src"] ?? []), "'unsafe-inline'"];
  }

  return directives;
}

export function buildCsp(options: BuildCspOptions): string {
  const directives = buildDirectives(options);
  const parts = Object.entries(directives).map(([name, values]) => `${name} ${values.join(" ")}`);
  parts.push("upgrade-insecure-requests");
  return parts.join("; ");
}

export interface SecurityHeader {
  key: string;
  value: string;
}

export function buildSecurityHeaders(options: BuildCspOptions): SecurityHeader[] {
  return [
    { key: "Content-Security-Policy", value: buildCsp(options) },
    { key: "X-Frame-Options", value: "DENY" },
    { key: "X-Content-Type-Options", value: "nosniff" },
    { key: "Referrer-Policy", value: "strict-origin-when-cross-origin" },
    { key: "Permissions-Policy", value: "camera=(), microphone=(), geolocation=()" },
    { key: "Strict-Transport-Security", value: "max-age=31536000; includeSubDomains; preload" },
    { key: "Cross-Origin-Opener-Policy", value: "same-origin" },
  ];
}

export const SECURITY_HEADERS: SecurityHeader[] = buildSecurityHeaders({
  isProduction: process.env.NODE_ENV === "production",
});
