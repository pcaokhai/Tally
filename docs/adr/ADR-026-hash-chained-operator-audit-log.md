# ADR-026: Hash-chained operator audit log

- **Status:** Accepted
- **Date:** 2026-09-22
- **Deciders:** Tech lead (Phan Cao Khai)
- **Related:** NFR-ADM-AUD-02, NFR-ADM-AUD-03, TLY-801

## Context
Audit logs are only useful if tampering is detectable.

## Options considered
1. Plain append-only table — deletions undetectable by DB superuser
2. Hash chain with daily verification and alert — tamper-evident / sequential writes

## Decision
Each entry stores `prev_hash` and `hash = sha256(prev_hash || canonical entry)`; UPDATE/DELETE revoked; a daily job verifies the chain and alerts on breaks.

## Consequences
Tampering is detected, not prevented; writes are serialized on the sequence (acceptable at operator volumes).
