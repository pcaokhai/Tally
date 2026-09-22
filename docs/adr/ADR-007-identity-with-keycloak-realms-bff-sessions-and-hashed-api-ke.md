# ADR-007: Identity with Keycloak realms, BFF sessions and hashed API keys

- **Status:** Accepted
- **Date:** 2026-09-22
- **Deciders:** Tech lead (Phan Cao Khai)
- **Related:** FR-TEN-04, FR-TEN-05, NFR-SEC-04, TLY-103, TLY-104

## Context
Tenant users need SSO-style login and tenant switching; machines need API keys; operators need a completely separate identity.

## Options considered
1. Build auth in core — control / high risk
2. Hosted IdP — easy / cost and less learning
3. Keycloak with organizations and token exchange + BFF + hashed API keys — standard protocols / Keycloak ops

## Decision
Tenant users authenticate in realm `tally-tenants`; the Next.js BFF keeps tokens server-side and sets an encrypted httpOnly cookie; switching tenant uses token exchange. API keys are prefix + Argon2id hash, cached 5 s. Operators use realm `tally-operators` (ADR-022).

## Consequences
No tokens in the browser; revocation within 5 s; fallback if token exchange misbehaves: core-minted short-lived JWTs (risk register elephant).
