# ADR-028: Platform plans separate from the tenant catalog

- **Status:** Accepted
- **Date:** 2026-09-22
- **Deciders:** Tech lead (Phan Cao Khai)
- **Related:** FR-TEN-02, FR-ADM-CFG-01, TLY-101, TLY-813

## Context
What Tally charges tenants and what tenants charge their customers look similar but have different owners and lifecycles.

## Options considered
1. Reuse catalog tables for platform plans — less code / tenant-scoped RLS on platform data, confusion
2. `tenancy.plan_versions` owned by operators, versioned and immutable — clear / some duplication

## Decision
Platform plans live in `tenancy.plan_versions` (limits jsonb, immutable versions, migrations by operators); the tenant catalog lives in `catalog.*` under RLS.

## Consequences
No mixing of operator and tenant data; plan changes are explicit migrations with dry runs.
