# ADR-027: Composite tenant foreign keys

- **Status:** Accepted
- **Date:** 2026-09-22
- **Deciders:** Tech lead (Phan Cao Khai)
- **Related:** NFR-SEC-01, TLY-102

## Context
RLS protects reads/writes, but a buggy insert could still link a row to another tenant's parent if the policy were ever misconfigured.

## Options considered
1. Single-column FKs — standard / cross-tenant references possible
2. Composite FKs (tenant_id, x_id) → (tenant_id, id) — DB rejects cross-tenant links / wider indexes

## Decision
Every intra-tenant reference is a composite FK; referenced tables carry `UNIQUE (tenant_id, id)`.

## Consequences
Defense in depth independent of RLS; slightly larger indexes.
