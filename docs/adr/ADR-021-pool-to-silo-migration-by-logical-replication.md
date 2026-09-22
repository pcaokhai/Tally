# ADR-021: Pool-to-silo migration by logical replication

- **Status:** Proposed
- **Date:** 2026-09-22
- **Deciders:** Tech lead (Phan Cao Khai)
- **Related:** FR-OPS-03, FR-ADM-TEN-08, TLY-906

## Context
Enterprise tenants may require dedicated storage without downtime.

## Options considered
1. Dump and restore with downtime — simple / downtime
2. Logical replication filtered by tenant + routing switch — near-zero downtime / complex

## Decision
Proposed: replicate tenant rows to a dedicated database, verify counts and ledger balances, pause writes ≤ 5 s, switch the tenant's datasource in the routing table; rollback within 15 minutes.

## Consequences
Time-boxed in Sprint 15; if cut, this ADR stays Proposed with a design-only addendum.
