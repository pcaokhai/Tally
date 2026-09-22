# ADR-001: Modular monolith core with Go workers

- **Status:** Accepted
- **Date:** 2026-09-22
- **Deciders:** Tech lead (Phan Cao Khai)
- **Related:** NFR-MNT-01, TLY-004, TLY-005

## Context
Billing logic needs strong consistency across subscriptions, invoices, payments and ledger; delivery and aggregation need high fan-out and independent scaling. One developer maintains everything.

## Options considered
1. Microservices per domain — independent deploys / distributed transactions everywhere, heavy ops
2. Single monolith incl. workers — simplest / delivery load competes with API latency
3. Modular monolith (Spring Modulith) + two Go workers — ACID where money lives, isolation where load differs / two languages

## Decision
The core is one Spring Boot deployable with enforced modules; webhook dispatch and usage aggregation are separate Go services connected only by Kafka and a documented internal API.

## Consequences
Local transactions for money flows; module boundaries enforced so extraction stays possible; two toolchains to maintain; cross-service consistency is eventual and designed explicitly (ADR-008, ADR-016).
