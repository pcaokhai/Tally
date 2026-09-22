import type { NextConfig } from "next";
import { buildSecurityHeaders } from "./lib/csp";

const nextConfig: NextConfig = {
  reactStrictMode: true,
  async headers() {
    const headers = buildSecurityHeaders();
    return [
      {
        source: "/(.*)",
        headers: headers.map((h) => ({ key: h.key, value: h.value })),
      },
    ];
  },
};

export default nextConfig;
