# ADR-002: Pool isolation with row-level security by default, silo tier for enterprise

- **Status:** Accepted
- **Date:** 2026-09-22
- **Deciders:** Tech lead (Phan Cao Khai)
- **Related:** NFR-SEC-01, FR-TEN-02, TLY-102

## Context
Thousands of small tenants must be cheap; a few large tenants may require dedicated storage. Application-only filtering fails silently when a developer forgets a WHERE clause.

## Options considered
1. Database per tenant — strongest / costly, migrations × N
2. Schema per tenant — medium / catalog bloat at 10k tenants
3. Shared tables + tenant_id + RLS FORCE — cheap, DB-enforced / needs disciplined context setting

## Decision
All tenants start in the shared pool with `tenant_id` on every table and RLS enabled and forced; the app role has no BYPASSRLS. `isolation_tier` allows moving a tenant to a silo database later (ADR-021).

## Consequences
One schema to migrate; isolation enforced by the database; every connection must set context (ADR-003); noisy neighbors handled at the app layer (ADR-019).
