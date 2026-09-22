# web — CLAUDE.md

The tenant dashboard (Next.js App Router, TypeScript) plus its BFF: the Next.js server holds tokens, the browser gets an httpOnly session cookie. Screens and motion are specified by the design canvas (link in docs/01 §7.1). Lane: **WEB**. Owns: `web/**`.

## Commands

```
pnpm dev                       dev server; NEXT_PUBLIC_API_MODE=mock (default) | live
pnpm test                      Vitest + Testing Library
pnpm test:e2e                  Playwright (needs `make up` for live mode)
pnpm lint / pnpm typecheck     eslint + tsc --noEmit
pnpm storybook                 components in all states (loading, empty, error, partial)
pnpm gen                       openapi-typescript + MSW handlers from ../contracts/openapi.yaml (never edit output)
```

## Layout

```
app/(auth)/                 login callback routes (BFF), tenant switch
app/(app)/layout.tsx        shell: sidebar (full nav, always), topbar (search ⌘K, test mode, bell, avatar)
app/(app)/<route>/          home, customers, customers/[id], subscriptions, subscriptions/new, invoices, invoices/[id],
                            payments, usage, products, developers/api-keys, developers/webhooks, developers/events, settings
app/api/bff/[...path]/      BFF proxy: attaches access token, Idempotency-Key, forwards problem+json unchanged
lib/api/                    generated types + typed client (openapi-fetch); query keys include tenantId
lib/motion/                 motion tokens (durations, easings, springs) — the only source of animation values
components/ui/              shadcn/ui primitives (owned code)   ·  components/<feature>/  feature components
mocks/                      MSW handlers generated from contracts + fixture overrides
```

## Next.js rules

- Server Components by default; client components are small leaves (`"use client"` only where state/motion lives).
- No data fetching in `useEffect`; TanStack Query for client data; server components call the BFF-internal client.
- **Tenant switch clears every query cache and in-memory store** before rendering the new tenant (NFR-SEC-01 in the UI).
- Money is formatted only by `formatMoney(money, locale)` using `Intl.NumberFormat`; amounts that users read exactly never animate (KPI count-up only on Home).
- Every mutating button sends an `Idempotency-Key` generated once per user intent and disables itself while pending.
- Test mode shows the amber banner on every screen; it cannot be dismissed.

## Motion and accessibility

- Durations/easings only from `lib/motion` tokens (120/200/350 ms; standard, exit, spring.soft, spring.snappy). Animate `transform`/`opacity` only.
- `prefers-reduced-motion`: every animation has a reduced variant (fade or none) — enforced by a Storybook/Playwright check.
- WCAG 2.2 AA; status never by color alone (color + icon/dot + label); real `<button>`/`<a>`; focus visible; live regions for async results.

## TypeScript rules (see docs/10 §4.4)

`strict`, `noUncheckedIndexedAccess`, `exactOptionalPropertyTypes`; no `any`; Zod at trust boundaries; named exports; one component per file.

## Tests

- Unit/component tests name the AC: `it("finalized invoice hides edit controls — TLY-506-AC3")`.
- Every screen has Storybook stories for loading, empty, error and partial states; Chromatic/Playwright visual snapshots.
- Playwright journeys per slice (docs/08 §5) run in mock mode in CI and live mode at the integration checkpoint.
