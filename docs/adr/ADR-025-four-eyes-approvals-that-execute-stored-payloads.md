# ADR-025: Four-eyes approvals that execute stored payloads

- **Status:** Accepted
- **Date:** 2026-09-22
- **Deciders:** Tech lead (Phan Cao Khai)
- **Related:** FR-ADM-GOV-01, FR-ADM-GOV-02, TLY-802

## Context
High-risk operator actions need a second person, and the approved action must be exactly what was reviewed.

## Options considered
1. Approver re-submits the action — the payload can change
2. Store payload + sha256 at request time; executor re-hashes and runs the stored payload — tamper-evident

## Decision
Gated actions create `approval_requests` with payload and hash (24 h expiry); a different operator approves after step-up; `ApprovalExecutor` verifies the hash and executes the stored payload.

## Consequences
Requester cannot approve; reviewers see exactly what runs; executor code paths must be idempotent.
