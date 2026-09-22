import type { NextConfig } from "next";

// TODO(TLY-007 T3): security headers (strict CSP, HSTS, X-Frame-Options)
const nextConfig: NextConfig = {
  reactStrictMode: true,
};

export default nextConfig;
