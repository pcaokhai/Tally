# ADR-006: Idempotency keys at every boundary

- **Status:** Accepted
- **Date:** 2026-09-22
- **Deciders:** Tech lead (Phan Cao Khai)
- **Related:** NFR-COR-03, FR-PAY-01, TLY-110

## Context
Clients retry on timeouts; Kafka redelivers; jobs rerun. Without idempotency, retries create duplicate charges and objects.

## Options considered
1. Best-effort dedup in code — cheap / inconsistent
2. Idempotency keys + stored responses + consumer dedup tables — systematic / storage and cleanup

## Decision
Creating/money-moving REST writes require `Idempotency-Key` scoped per tenant, stored with a request hash and response for 24 h; consumers dedupe by event id; jobs by (job, tenant, date); processor calls by payment id + attempt.

## Consequences
Safe retries everywhere; a filter and inbox table to maintain; clear 409/422 semantics documented in docs/04.
