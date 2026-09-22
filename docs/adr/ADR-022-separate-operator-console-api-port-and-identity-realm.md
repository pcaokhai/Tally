# ADR-022: Separate operator console, API port and identity realm

- **Status:** Accepted
- **Date:** 2026-09-22
- **Deciders:** Tech lead (Phan Cao Khai)
- **Related:** NFR-ADM-SEC-01, NFR-ADM-SEC-05, TLY-007, TLY-801

## Context
Mixing operator powers into the tenant app makes every tenant-app bug an operator-level bug.

## Options considered
1. Admin pages in the tenant app — cheap / shared attack surface
2. Separate app, origin, port (8081), filter chain and realm — isolation / more code

## Decision
ops-web is a separate Next.js app on a VPN-only origin; `/ops/v1` runs on its own port and SecurityFilterChain accepting only operator-realm tokens with WebAuthn.

## Consequences
Tenant tokens can never reach ops endpoints; duplicated UI primitives are shared as a package.
