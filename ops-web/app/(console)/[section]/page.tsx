import { notFound } from "next/navigation";
import { NAV_ITEMS, isNavSlug } from "@/lib/nav";

export function generateStaticParams() {
  return NAV_ITEMS.filter((item) => item.slug !== "overview").map((item) => ({
    section: item.slug,
  }));
}

export default async function ConsoleSectionPage({
  params,
}: {
  params: Promise<{ section: string }>;
}) {
  const { section } = await params;
  if (!isNavSlug(section)) {
    notFound();
  }

  const item = NAV_ITEMS.find((navItem) => navItem.slug === section);

  return (
    <div>
      <h1 className="text-sm uppercase tracking-widest text-hairline">
        {item?.label}
      </h1>
      <p className="mt-4 text-sm text-hairline">
        This section lands in a later story.
      </p>
    </div>
  );
}
