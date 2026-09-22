// Small inline-SVG icons for the sidebar nav. Decorative — always aria-hidden.

function iconProps() {
  return {
    width: 18,
    height: 18,
    viewBox: "0 0 24 24",
    fill: "none",
    stroke: "currentColor",
    strokeWidth: 1.7,
    strokeLinecap: "round" as const,
    strokeLinejoin: "round" as const,
    "aria-hidden": true,
  };
}

export function HomeIcon() {
  return (
    <svg {...iconProps()}>
      <path d="M3 11.5 12 4l9 7.5" />
      <path d="M5.5 10v9a1 1 0 0 0 1 1H9v-6h6v6h2.5a1 1 0 0 0 1-1v-9" />
    </svg>
  );
}

export function CustomersIcon() {
  return (
    <svg {...iconProps()}>
      <circle cx="9" cy="8" r="3.5" />
      <path d="M2.5 20a6.5 6.5 0 0 1 13 0" />
      <path d="M16 5.5a3.5 3.5 0 0 1 0 7" />
      <path d="M17.5 13.5a6.5 6.5 0 0 1 4 6.5" />
    </svg>
  );
}

export function SubscriptionsIcon() {
  return (
    <svg {...iconProps()}>
      <path d="M4 4v7a8 8 0 0 0 8 8 8 8 0 0 0 8-8" />
      <path d="M4 4h7" />
      <path d="M20 4v7" />
    </svg>
  );
}

export function InvoicesIcon() {
  return (
    <svg {...iconProps()}>
      <path d="M6 2.5h9l3 3V21a.5.5 0 0 1-.5.5h-11a.5.5 0 0 1-.5-.5V3a.5.5 0 0 1 .5-.5Z" />
      <path d="M9 9h6M9 13h6M9 17h4" />
    </svg>
  );
}

export function PaymentsIcon() {
  return (
    <svg {...iconProps()}>
      <rect x="2.5" y="5.5" width="19" height="13" rx="2" />
      <path d="M2.5 10h19" />
    </svg>
  );
}

export function UsageIcon() {
  return (
    <svg {...iconProps()}>
      <path d="M4 20V10M12 20V4M20 20v-6" />
    </svg>
  );
}

export function ProductsIcon() {
  return (
    <svg {...iconProps()}>
      <path d="M12 2.5 20.5 7 12 11.5 3.5 7 12 2.5Z" />
      <path d="M3.5 7v10L12 21.5 20.5 17V7" />
      <path d="M12 11.5V21.5" />
    </svg>
  );
}

export function ApiKeysIcon() {
  return (
    <svg {...iconProps()}>
      <circle cx="8" cy="16" r="4" />
      <path d="M11 13 20 4M17 7l2.5 2.5M14 10l2 2" />
    </svg>
  );
}

export function WebhooksIcon() {
  return (
    <svg {...iconProps()}>
      <circle cx="6" cy="6" r="2.5" />
      <circle cx="18" cy="6" r="2.5" />
      <circle cx="12" cy="18" r="2.5" />
      <path d="M8.2 7.2 11 16M15.8 7.2 13 16M8.5 6h7" />
    </svg>
  );
}

export function EventsIcon() {
  return (
    <svg {...iconProps()}>
      <path d="M4 4.5h16v15H4Z" />
      <path d="M4 9h16M8 4.5v-2M16 4.5v-2" />
    </svg>
  );
}

export function SettingsIcon() {
  return (
    <svg {...iconProps()}>
      <circle cx="12" cy="12" r="3" />
      <path d="M19.5 12a7.5 7.5 0 0 0-.13-1.4l2-1.55-2-3.46-2.36.95a7.6 7.6 0 0 0-2.42-1.4L14.2 2.5H9.8l-.4 2.64a7.6 7.6 0 0 0-2.42 1.4l-2.36-.95-2 3.46 2 1.55a7.5 7.5 0 0 0 0 2.8l-2 1.55 2 3.46 2.36-.95c.72.61 1.54 1.08 2.42 1.4l.4 2.64h4.4l.4-2.64a7.6 7.6 0 0 0 2.42-1.4l2.36.95 2-3.46-2-1.55c.09-.46.13-.93.13-1.4Z" />
    </svg>
  );
}
