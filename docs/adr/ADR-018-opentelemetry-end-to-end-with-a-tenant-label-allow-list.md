# ADR-018: OpenTelemetry end to end with a tenant label allow-list

- **Status:** Accepted
- **Date:** 2026-09-22
- **Deciders:** Tech lead (Phan Cao Khai)
- **Related:** NFR-OBS-01..03, TLY-901

## Context
Debugging async flows needs one trace across services; per-tenant metrics explode cardinality.

## Options considered
1. Logs only — cheap / no causality
2. OTel traces + metrics + logs, tenant as span attribute, tenant label only on allow-listed metrics — full picture / discipline needed

## Decision
W3C traceparent through HTTP and Kafka headers; `tenant.id` on every span and log; `tenant_id` metric label only on the allow-list enforced in CI.

## Consequences
Incidents traceable across core and workers; cardinality bounded.
