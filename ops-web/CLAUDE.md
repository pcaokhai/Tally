# ops-web — CLAUDE.md

The operator console: a separate Next.js app on a separate origin, reachable only through the VPN/zero-trust gateway, authenticated against the operator realm with WebAuthn. It calls `/ops/v1` only. Lane: **OPSW**. Owns: `ops-web/**`.

## Commands

Same as `web/` (`pnpm dev|test|test:e2e|lint|typecheck|storybook|gen`); `pnpm gen` reads `../contracts/ops-openapi.yaml`.

## Layout

```
app/(console)/layout.tsx    shell: PRODUCTION/STAGING badge (non-dismissible), session timer, ops sidebar
app/(console)/<route>/      overview, tenants, health, fleet, jobs, recon, refunds, ledger, plans, flags,
                            approvals, impersonation, audit, operators
lib/access-reason/          reason dialog + header injection (X-Access-Reason) for tenant-data reads
lib/step-up/                WebAuthn step-up before approve / write impersonation / offboarding
components/ui/              shared tokens with web/, dark-first, compact density (32–40 px rows)
```

## Rules

- Dark-first, compact, mono for ids and amounts; motion only for live-data changes and state transitions — no count-ups, no celebration.
- Read-only by default; write actions are visually separated from navigation and show exact impact (tenant, amount, count) before confirm.
- Sensitive fields are masked until revealed; a reveal calls the audited endpoint, never unmasks client-side.
- Approve buttons are disabled for the requester's own requests (UI hint only; the API enforces it).
- Impersonation banner is rendered by the tenant app from the `act` claim; ops-web only starts/ends sessions.
- No third-party scripts; strict CSP; cookies scoped to the ops origin.

## Tests

Same conventions as `web/`; plus a Playwright suite asserting every tenant-data view sends `X-Access-Reason` and every approval path requires step-up.
