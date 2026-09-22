import Link from "next/link";

/** Body of a 404, without the console chrome — see the two not-found routes. */
export function NotFoundContent() {
  return (
    <>
      <h1 className="text-sm uppercase tracking-widest text-hairline">
        Unknown console section
      </h1>
      <p className="mt-4 text-sm text-hairline">
        No operator screen is registered at this address.
      </p>
      <Link
        href="/"
        className="mt-6 inline-block text-sm text-signal underline underline-offset-4"
      >
        Return to overview
      </Link>
    </>
  );
}
