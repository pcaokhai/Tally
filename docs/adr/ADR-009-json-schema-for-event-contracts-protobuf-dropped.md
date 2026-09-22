# ADR-009: JSON Schema for event contracts (Protobuf dropped)

- **Status:** Accepted
- **Date:** 2026-09-22
- **Deciders:** Tech lead (Phan Cao Khai)
- **Related:** NFR-MNT-04, TLY-003

## Context
The earlier design chose Protobuf with a schema registry. Webhook payloads delivered to tenants are JSON anyway, and a registry adds a container to the laptop budget.

## Options considered
1. Protobuf + registry — compact, typed / second schema language, extra service
2. Avro — evolution rules / same drawbacks
3. JSON Schema 2020-12 in contracts/, codegen for Java and Go, CI compatibility check — one language for internal and public events / larger messages

## Decision
All events use one envelope and JSON Schema files in `contracts/events/`; envelopes carry `api_version`; compatibility is checked in CI; this supersedes the Protobuf decision from the design discussion.

## Consequences
Same schema documents internal topics and public webhooks; messages are larger (acceptable at target load); schema registry not needed.
