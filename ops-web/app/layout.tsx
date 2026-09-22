import type { Metadata } from "next";
import { connection } from "next/server";
import "./globals.css";
import { MockProvider } from "@/components/MockProvider";

export const metadata: Metadata = {
  title: "Tally Operator Console",
};

export default async function RootLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  // Forces dynamic rendering: a per-request CSP nonce (proxy.ts) can only be
  // stamped onto inline scripts during server-side rendering of an actual
  // request — a statically prerendered page has no nonce to stamp (see
  // docs/plans/TLY-007.md ruling: static→dynamic for the nonce CSP).
  await connection();

  return (
    <html lang="en" className="dark">
      <body>
        <MockProvider>{children}</MockProvider>
      </body>
    </html>
  );
}
