import type { ReactNode } from "react";
import {
  ApiKeysIcon,
  CustomersIcon,
  EventsIcon,
  HomeIcon,
  InvoicesIcon,
  PaymentsIcon,
  ProductsIcon,
  SettingsIcon,
  SubscriptionsIcon,
  UsageIcon,
  WebhooksIcon,
} from "./icons";

export interface NavItem {
  label: string;
  href: string;
  icon: ReactNode;
  badge?: "issues";
}

export interface NavGroup {
  heading?: string;
  items: NavItem[];
}

export const navGroups: NavGroup[] = [
  {
    items: [
      { label: "Home", href: "/", icon: <HomeIcon /> },
      { label: "Customers", href: "/customers", icon: <CustomersIcon /> },
    ],
  },
  {
    heading: "BILLING",
    items: [
      { label: "Subscriptions", href: "/subscriptions", icon: <SubscriptionsIcon /> },
      { label: "Invoices", href: "/invoices", icon: <InvoicesIcon /> },
      { label: "Payments", href: "/payments", icon: <PaymentsIcon /> },
      { label: "Usage", href: "/usage", icon: <UsageIcon /> },
      { label: "Products & pricing", href: "/products", icon: <ProductsIcon /> },
    ],
  },
  {
    heading: "DEVELOPERS",
    items: [
      { label: "API keys", href: "/developers/api-keys", icon: <ApiKeysIcon /> },
      {
        label: "Webhooks",
        href: "/developers/webhooks",
        icon: <WebhooksIcon />,
        badge: "issues",
      },
      { label: "Events & logs", href: "/developers/events", icon: <EventsIcon /> },
    ],
  },
];

export const settingsItem: NavItem = {
  label: "Settings",
  href: "/settings",
  icon: <SettingsIcon />,
};
