export type OpsEnvironment = "PRODUCTION" | "STAGING" | "LOCAL";

interface EnvironmentPresentation {
  readonly label: string;
  readonly tokenClass: string;
}

const PRESENTATION: Record<OpsEnvironment, EnvironmentPresentation> = {
  PRODUCTION: { label: "PRODUCTION", tokenClass: "bg-danger text-canvas" },
  STAGING: { label: "STAGING", tokenClass: "bg-warn text-canvas" },
  LOCAL: { label: "LOCAL", tokenClass: "bg-hairline text-current" },
};

function parseOpsEnvironment(value: string | undefined): OpsEnvironment {
  if (value === undefined || value === "") {
    return "LOCAL";
  }
  if (value === "PRODUCTION" || value === "STAGING" || value === "LOCAL") {
    return value;
  }
  throw new Error(`Unknown NEXT_PUBLIC_OPS_ENV value: ${value}`);
}

export const OPS_ENVIRONMENT: OpsEnvironment = parseOpsEnvironment(
  process.env.NEXT_PUBLIC_OPS_ENV,
);

export function presentationFor(env: OpsEnvironment): EnvironmentPresentation {
  return PRESENTATION[env];
}
