import nextConfig from "eslint-config-next";

const config = [
  { ignores: [".next/**", "lib/api/generated/**", "node_modules/**"] },
  ...nextConfig,
];

export default config;
