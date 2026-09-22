# ADR-010: Kafka topics keyed by tenant_id

- **Status:** Accepted
- **Date:** 2026-09-22
- **Deciders:** Tech lead (Phan Cao Khai)
- **Related:** NFR-FAIR-01, TLY-301

## Context
Consumers need per-tenant ordering for state changes and a unit of fairness.

## Options considered
1. Key by aggregate id — even spread / no per-tenant ordering
2. Key by tenant_id — per-tenant order, simple / hot partitions for whales

## Decision
All topics are keyed by tenant_id with 24 partitions locally; whales are mitigated in the dispatcher by fair queues (ADR-011), not by Kafka partitioning.

## Consequences
Order within a tenant; hot partitions possible — monitored by consumer lag per partition.
