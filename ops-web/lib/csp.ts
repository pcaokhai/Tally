const PRODUCTION_DIRECTIVES: Record<string, string[]> = {
  "default-src": ["'self'"],
  "frame-ancestors": ["'none'"],
  "object-src": ["'none'"],
  "base-uri": ["'self'"],
  "form-action": ["'self'"],
  "frame-src": ["'none'"],
  "img-src": ["'self'", "data:"],
  "font-src": ["'self'"],
  // ponytail: same-origin only until TLY-808/TLY-809 add live /ops/v1 calls and
  // the health SSE stream — revisit then if either turns out to be cross-origin.
  "connect-src": ["'self'"],
  "manifest-src": ["'self'"],
  "worker-src": ["'self'"],
};

export interface BuildCspOptions {
  isProduction: boolean;
  nonce: string;
}

export function buildDirectives({ isProduction, nonce }: BuildCspOptions): Record<string, string[]> {
  const directives: Record<string, string[]> = {};
  for (const [name, values] of Object.entries(PRODUCTION_DIRECTIVES)) {
    directives[name] = [...values];
  }

  // Fail safe: only an explicit "development" NODE_ENV gets the relaxed policy;
  // anything else (unset, "test", a typo) gets the strict production policy.
  if (isProduction) {
    directives["script-src"] = ["'self'", `'nonce-${nonce}'`, "'strict-dynamic'"];
    directives["style-src"] = ["'self'", `'nonce-${nonce}'`];
  } else {
    directives["script-src"] = ["'self'", `'nonce-${nonce}'`, "'strict-dynamic'", "'unsafe-eval'"];
    directives["style-src"] = ["'self'", `'nonce-${nonce}'`, "'unsafe-inline'"];
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

/** Non-CSP headers: static per response, set in next.config.ts. The CSP itself
 * is per-request (it carries a nonce) and is set in proxy.ts instead. */
export function buildSecurityHeaders(): SecurityHeader[] {
  return [
    { key: "X-Frame-Options", value: "DENY" },
    { key: "X-Content-Type-Options", value: "nosniff" },
    { key: "Referrer-Policy", value: "strict-origin-when-cross-origin" },
    { key: "Permissions-Policy", value: "camera=(), microphone=(), geolocation=()" },
    { key: "Strict-Transport-Security", value: "max-age=31536000; includeSubDomains; preload" },
    { key: "Cross-Origin-Opener-Policy", value: "same-origin" },
  ];
}
