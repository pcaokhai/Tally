import { OPS_ENVIRONMENT, presentationFor, type OpsEnvironment } from "@/lib/environment";

interface EnvironmentBadgeProps {
  readonly env?: OpsEnvironment;
}

export function EnvironmentBadge({ env = OPS_ENVIRONMENT }: EnvironmentBadgeProps) {
  const presentation = presentationFor(env);

  return (
    <div
      role="status"
      className={`flex h-[24px] w-full items-center justify-center font-mono-ops text-[10px] uppercase tracking-widest ${presentation.tokenClass}`}
    >
      {presentation.label}
    </div>
  );
}
