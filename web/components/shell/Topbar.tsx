"use client";

export function Topbar({
  testMode,
  onTestModeChange,
}: {
  testMode: boolean;
  onTestModeChange: (next: boolean) => void;
}) {
  return (
    <header className="flex h-[--layout-topbar-height] shrink-0 items-center justify-between border-b border-[--color-line] bg-[--color-surface] px-[--spacing-32]">
      <label className="flex h-[42px] w-[440px] items-center gap-[--spacing-10] rounded-[--radius-lg] border border-[--color-line-strong] bg-[--color-raised] px-[--spacing-14]">
        <span className="sr-only">Search</span>
        <svg
          width="16"
          height="16"
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          strokeWidth="1.7"
          aria-hidden="true"
        >
          <circle cx="11" cy="11" r="7" />
          <path d="m20 20-3.5-3.5" />
        </svg>
        <input
          type="search"
          placeholder="Search customers, invoices, payments…"
          className="flex-1 bg-transparent text-[length:--text-base] outline-none"
        />
        <kbd className="rounded-[--radius-sm] bg-[--color-hover] px-[--spacing-6] font-[family-name:--font-geist-mono] text-[length:--text-xs] text-[--color-ink-subtle]">
          ⌘K
        </kbd>
      </label>

      <div className="flex items-center gap-[--spacing-12]">
        <button
          type="button"
          role="switch"
          aria-checked={testMode}
          aria-label="Test mode"
          onClick={() => onTestModeChange(!testMode)}
          className={`relative h-5 w-9 rounded-[--radius-full] transition-colors duration-[--motion-duration-base] ${
            testMode ? "bg-[--color-warn-accent]" : "bg-[--color-line-strong]"
          }`}
        >
          <span
            aria-hidden="true"
            className="absolute top-0.5 left-0.5 h-4 w-4 rounded-[--radius-full] bg-white transition-transform duration-[--motion-duration-base]"
            style={{ transform: testMode ? "translateX(16px)" : "translateX(0)" }}
          />
        </button>

        <button
          type="button"
          aria-label="Notifications, 4 unread"
          className="relative flex h-[42px] w-[42px] items-center justify-center rounded-[--radius-md] hover:bg-[--color-hover]"
        >
          <svg
            width="18"
            height="18"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            strokeWidth="1.7"
            aria-hidden="true"
          >
            <path d="M6 10a6 6 0 0 1 12 0c0 5 2 6 2 6H4s2-1 2-6Z" />
            <path d="M10 20a2 2 0 0 0 4 0" />
          </svg>
          <span
            aria-hidden="true"
            className="absolute right-2 top-2 h-2 w-2 rounded-full bg-[--color-danger] ring-2 ring-[--color-raised]"
          />
        </button>

        <span className="flex h-10 w-10 items-center justify-center rounded-full bg-[--color-ink] text-[length:--text-sm-plus] font-semibold text-[--color-on-ink]">
          LN
        </span>
      </div>
    </header>
  );
}
