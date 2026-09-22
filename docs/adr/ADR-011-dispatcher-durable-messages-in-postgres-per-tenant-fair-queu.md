# ADR-011: Dispatcher: durable messages in Postgres, per-tenant fair queues in Redis

- **Status:** Accepted
- **Date:** 2026-09-22
- **Deciders:** Tech lead (Phan Cao Khai)
- **Related:** NFR-FAIR-02, FR-WH-04, TLY-304, TLY-305

## Context
Delivering directly from Kafka lets one slow endpoint block a partition and every tenant on it.

## Options considered
1. Deliver from the consumer loop — simple / head-of-line blocking
2. Kafka retry topics — no DB / weak per-tenant fairness
3. Persist messages, commit offset, schedule per tenant with WRR — fair, durable / two stores

## Decision
The consumer persists one delivery message per matching endpoint, then commits the offset; a scheduler serves per-tenant Redis queues by weighted round-robin; Postgres is the source of truth, Redis is rebuildable.

## Consequences
Slow tenants never block others; recovery rebuilds Redis from Postgres; operational complexity accepted for the learning goal.
