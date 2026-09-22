export function TestModeBanner() {
  return (
    <div className="flex h-(--layout-test-band-height) items-center gap-(--spacing-8) border-b border-(--color-warn-line) bg-(--color-warn-bg) px-(--spacing-32) text-(length:--text-sm) text-(--color-warn-ink)">
      <svg
        width="14"
        height="14"
        viewBox="0 0 24 24"
        fill="none"
        stroke="currentColor"
        strokeWidth="1.7"
        aria-hidden="true"
      >
        <path d="M9 2.5h6M10 3v6.5L4.5 19a2 2 0 0 0 1.7 3h11.6a2 2 0 0 0 1.7-3L14 9.5V3" />
      </svg>
      <span>You&apos;re in test mode. No real money moves.</span>
    </div>
  );
}
