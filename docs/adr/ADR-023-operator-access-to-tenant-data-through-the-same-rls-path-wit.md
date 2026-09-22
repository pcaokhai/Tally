# ADR-023: Operator access to tenant data through the same RLS path with a reason

- **Status:** Accepted
- **Date:** 2026-09-22
- **Deciders:** Tech lead (Phan Cao Khai)
- **Related:** NFR-ADM-SEC-08, NFR-ADM-AUD-01, TLY-801

## Context
A BYPASSRLS operator role would make every operator query a potential cross-tenant leak.

## Options considered
1. BYPASSRLS operator role — easy / unsafe
2. Set tenant context per request with a mandatory reason, audited — same guarantees as tenants / one tenant per request

## Decision
Operator endpoints that read tenant data require `X-Access-Reason`, set the tenant context for that request, and write an audit entry in the same transaction. Cross-tenant aggregates come from read models and metrics, not raw tables.

## Consequences
No RLS bypass anywhere; some fleet-wide views need dedicated read models.
