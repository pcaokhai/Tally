# ADR-019: Per-tenant rate limiting with Redis token buckets

- **Status:** Accepted
- **Date:** 2026-09-22
- **Deciders:** Tech lead (Phan Cao Khai)
- **Related:** NFR-FAIR-03, TLY-111

## Context
One tenant must not starve others on the shared pool.

## Options considered
1. Global limit — simple / unfair
2. Per-tenant token bucket by plan with overrides — fair / Redis dependency

## Decision
Token bucket per tenant and per surface (API, ingest) in Redis, limits from plan version or operator override; fail-open 60 s then in-memory at 2× plan.

## Consequences
Fairness at the edge; Redis outage degrades gracefully; limits visible to tenants in Settings.
