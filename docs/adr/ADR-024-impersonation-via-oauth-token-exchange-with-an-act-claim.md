# ADR-024: Impersonation via OAuth token exchange with an act claim

- **Status:** Accepted
- **Date:** 2026-09-22
- **Deciders:** Tech lead (Phan Cao Khai)
- **Related:** FR-ADM-IMP-01..05, TLY-806

## Context
Support needs to see what a tenant sees without sharing passwords, and tenants must see who did what.

## Options considered
1. Log in as the user — no attribution
2. Token exchange (RFC 8693) with act claim, TTL = session, read-only by default — attributable / Keycloak configuration

## Decision
Operators obtain a tenant-scoped token with `act.sub = operator`; READ_ONLY unless an approved request id is supplied; the tenant app shows a banner; tenants can list sessions.

## Consequences
Full attribution in audit logs; write impersonation always needs four-eyes.
