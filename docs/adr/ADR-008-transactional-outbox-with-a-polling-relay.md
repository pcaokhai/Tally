# ADR-008: Transactional outbox with a polling relay

- **Status:** Accepted
- **Date:** 2026-09-22
- **Deciders:** Tech lead (Phan Cao Khai)
- **Related:** NFR-COR-02, NFR-PER-06, TLY-301

## Context
Publishing to Kafka after commit loses events on crash; publishing before commit invents events on rollback.

## Options considered
1. Dual write — simple / loses or invents events
2. CDC (Debezium) — low latency / extra infrastructure
3. Outbox table + polling relay with SKIP LOCKED — simple, reliable / 100 ms latency, DB load

## Decision
Events are inserted into `kernel.outbox_events` in the business transaction; a relay claims batches with `FOR UPDATE SKIP LOCKED`, produces keyed by tenant, then marks them published. The relay reads through a SECURITY DEFINER function, the only RLS exception.

## Consequences
At-least-once publication; consumers must dedupe (ADR-006); CDC remains an upgrade path if lag exceeds NFR-PER-06.
