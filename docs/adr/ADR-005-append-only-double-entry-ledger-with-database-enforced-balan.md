# ADR-005: Append-only double-entry ledger with database-enforced balance

- **Status:** Accepted
- **Date:** 2026-09-22
- **Deciders:** Tech lead (Phan Cao Khai)
- **Related:** FR-LED-01..04, NFR-COR-01, TLY-202

## Context
Balances derived from mutable columns drift under retries and partial failures; auditors need to see every change.

## Options considered
1. Balance columns updated in place — simple / no history, drift
2. Event-sourced balances only in code — flexible / invariant only as good as the code
3. Double-entry postings, append-only, deferred constraint trigger — invariant in the DB / insert-heavy

## Decision
Every money movement is a journal entry with ≥ 2 postings summing to zero per currency, checked by a deferred constraint trigger at commit; postings are never updated or deleted; corrections are reversing entries.

## Consequences
Ledger is the source of truth for money; jOOQ for explicit SQL; balances are queries (cacheable later); property tests guard the invariant.
