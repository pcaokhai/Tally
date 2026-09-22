# ADR-003: Tenant context propagation with ScopedValue and SET LOCAL

- **Status:** Accepted
- **Date:** 2026-09-22
- **Deciders:** Tech lead (Phan Cao Khai)
- **Related:** NFR-SEC-02, TLY-102

## Context
Virtual threads make ThreadLocal expensive and leak-prone; RLS needs a per-transaction setting that cannot bleed across pooled connections.

## Options considered
1. ThreadLocal + session-level SET — familiar / leaks across pooled connections
2. Pass tenantId explicitly everywhere — explicit / noisy, easy to forget at the DB edge
3. ScopedValue (final in Java 25) + SET LOCAL in each transaction — bounded lifetime, transaction-scoped / requires a connection preparer

## Decision
The tenant filter binds `ScopedValue<TenantContext>` from the credential; `TenantConnectionPreparer` issues `SET LOCAL app.tenant_id` at transaction start; `kernel.current_tenant()` returns NULL when unset so queries return nothing.

## Consequences
No context bleed between requests or pooled connections; reads outside transactions are forbidden for tenant tables; background jobs must bind context per tenant explicitly.
