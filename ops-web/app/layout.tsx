import type { Metadata } from "next";
import "./globals.css";
import { MockProvider } from "@/components/MockProvider";

export const metadata: Metadata = {
  title: "Tally Operator Console",
};

export default function RootLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <html lang="en" className="dark">
      <body>
        <MockProvider>{children}</MockProvider>
      </body>
    </html>
  );
}
