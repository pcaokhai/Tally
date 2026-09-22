# ADR-015: Usage aggregation with event-time windows and exactly-once counting

- **Status:** Accepted
- **Date:** 2026-09-22
- **Deciders:** Tech lead (Phan Cao Khai)
- **Related:** FR-MET-02, FR-MET-03, TLY-602

## Context
Usage arrives late, duplicated and out of order; invoices must count each event exactly once.

## Options considered
1. Aggregate in Postgres at invoice time — simple / slow at scale, late events hard
2. Stream processor with windows + dedup + deterministic rollup keys — scalable / more moving parts

## Decision
The aggregator uses 5-minute tumbling windows by event time with 2-minute lateness, dedups by event id, checkpoints state to a compacted topic and emits rollups with deterministic `rollup_key` that the core upserts.

## Consequences
Exactly-once effect end to end without Kafka transactions; period close decides what is billable.
