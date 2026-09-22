const STAT_READOUTS = [
  { label: "Active tenants", value: "—" },
  { label: "Open incidents", value: "—" },
  { label: "Jobs in flight", value: "—" },
  { label: "Pending approvals", value: "—" },
] as const;

export default function OverviewPage() {
  return (
    <div>
      <h1 className="text-sm uppercase tracking-widest text-hairline">
        Overview
      </h1>
      <dl className="mt-4 grid grid-cols-2 gap-4 sm:grid-cols-4">
        {STAT_READOUTS.map((stat) => (
          <div key={stat.label} className="border border-hairline p-3">
            <dt className="font-mono-ops text-[10px] uppercase tracking-widest text-hairline">
              {stat.label}
            </dt>
            <dd className="font-mono-ops text-lg">{stat.value}</dd>
          </div>
        ))}
      </dl>
      <p className="mt-4 text-sm text-hairline">Wired in TLY-805.</p>
    </div>
  );
}
