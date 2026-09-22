# ADR-016: Single writer per data store

- **Status:** Accepted
- **Date:** 2026-09-22
- **Deciders:** Tech lead (Phan Cao Khai)
- **Related:** NFR-MNT-01, TLY-304, TLY-603

## Context
Shared databases couple deploys and hide ownership of invariants.

## Options considered
1. Workers write core tables — fewer hops / broken ownership
2. Each store has one writer; others use events or APIs — clear ownership / eventual consistency

## Decision
The core is the only writer of `tally`; the dispatcher of `dispatcher` and `wh:*`; the aggregator of its state topic. State the core needs from workers arrives as events.

## Consequences
Invariants live next to their owner; some UI data is seconds stale.
