# ADR-031: Webhook delivery data served through a core proxy

- **Status:** Accepted
- **Date:** 2026-09-22
- **Deciders:** Tech lead (Phan Cao Khai)
- **Related:** FR-WH-06, NFR-RT-01, TLY-307

## Context
Delivery data lives in the dispatcher database (ADR-016), but tenant authentication and API conventions live in the core.

## Options considered
1. Expose dispatcher publicly with its own auth — duplicate auth logic
2. Core reads dispatcher DB — breaks single writer/ownership
3. Core `events` module proxies the dispatcher internal API — one auth path / an extra hop

## Decision
Tenant and operator requests for deliveries, health, replay, fleet and SSE go through the core, which passes the tenant id from its context; the dispatcher filters by it and is reachable only on the internal network.

## Consequences
Single auth and error model; +1 hop latency (acceptable); dispatcher API is an internal contract versioned with ops-openapi conventions.
