# ADR-020: Per-tenant envelope encryption and crypto-shredding

- **Status:** Accepted
- **Date:** 2026-09-22
- **Deciders:** Tech lead (Phan Cao Khai)
- **Related:** NFR-SEC-05, FR-TEN-07, TLY-905

## Context
Deleting a tenant from backups and replicas is impractical; PII must be unreadable after offboarding.

## Options considered
1. Delete rows only — backups still hold data
2. Per-tenant DEK wrapped by a KEK; destroy DEK on offboarding — data unreadable everywhere / key management

## Decision
Sensitive columns are encrypted with the tenant DEK; the DEK is stored wrapped by a KEK from the secret store; offboarding destroys the DEK after export.

## Consequences
Offboarding is provable; lookups need keyed hashes (e.g. customer email); key rotation procedure required.
