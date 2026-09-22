# ADR-012: Webhook retries, circuit breaker, DLQ and auto-disable

- **Status:** Accepted
- **Date:** 2026-09-22
- **Deciders:** Tech lead (Phan Cao Khai)
- **Related:** FR-WH-04, FR-WH-05, NFR-AVL-02, TLY-306

## Context
Endpoints fail transiently and permanently; retrying too fast floods receivers, too slow loses freshness.

## Options considered
1. Fixed interval — simple / thundering herd
2. Exponential backoff with full jitter + breaker + DLQ + disable — standard / more states

## Decision
Schedule 30s, 2m, 8m, 30m, 2h, 6h, 12h, 24h with full jitter; breaker opens after 10 failures (HELD, probe 60 s); DEAD_LETTERED after 8 retries or 72 h; auto-disable after 50 consecutive failures or 410.

## Consequences
Predictable receiver load; tenants see clear states; replay and bulk DLQ replay needed (TLY-307, TLY-810).
