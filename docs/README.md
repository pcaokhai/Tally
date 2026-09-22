# Tally documentation

Version 1.0 · 2026-09-22 · Owner: Tech lead

Everything needed to start Sprint 0 is here; nothing lives only in chat. Contracts in `contracts/` are normative; these documents explain and plan around them.

## Documents

| # | Document | Purpose | Primary audience | Status |
| --- | --- | --- | --- | --- |
| 01 | [PRD](01-prd.md) | Why, for whom, objectives with measurable key results, releases | Everyone | Accepted |
| 02 | [Software architecture](02-software-architecture.md) | FR/NFR drivers, C4 L1–L3, runtime views, cross-cutting rules, deployment, stack | All engineers | Accepted |
| 03 | [Webhook delivery spec](03-webhook-delivery-spec.md) | Wire contract with tenant endpoints: headers, signing, retries | WORK, CORE, tenant devs | Accepted |
| 04 | [API contract](04-api-contract.md) | REST/SSE/event conventions and full endpoint catalogues | CORE, WEB, OPSW | Accepted |
| 05 | [Data model](05-data-model.md) | Ownership, tables, modeling and migration rules, indexed queries | CORE, WORK | Accepted |
| 06 | [User stories](06-user-stories.md) | 81 stories, numbered AC, lanes, slices, traces | Every lane | Accepted |
| 07 | [Delivery plan](07-delivery-plan.md) | Lanes, slices, 16 sprints with waves, critical path, DoR/DoD, Claude Code playbook | Tech lead, every lane | Accepted |
| 08 | [Test strategy](08-test-strategy.md) | Pyramid, gates, journeys, TS-01..16, chaos suite | Every lane | Accepted |
| 09 | [Risk register](09-risk-register.md) | Pre-mortem tigers, paper tigers, elephants, actions | Tech lead | Accepted |
| 10 | [Engineering standards](10-engineering-standards.md) | Non-negotiables, patterns, per-language rules, review checklist | Every lane, reviewers | Accepted |
| — | [ADRs](adr/) | Decisions (index below) | Everyone | See table |
| — | [Plans](plans/README.md) | writing-plans output per story | Lanes | Living |
| — | [assets/ddl](assets/ddl/) | Baseline DDL (core, dispatcher) | CORE, WORK | Baseline |

## Reading order

- **Day 1, everyone:** root `CLAUDE.md` → 01 → 02 §1–5 → 07 §1–3 → 10 §0.
- **CORE:** `core/CLAUDE.md` → 02 §6–7 → 04 → 05 → 06 (your epic) → ADR-002..008, 014, 016, 017, 020, 022..028.
- **WORK:** `workers/CLAUDE.md` → 03 → 04 §7 → 05 (dispatcher) → ADR-010..013, 015, 016, 031.
- **WEB / OPSW:** `web/CLAUDE.md` or `ops-web/CLAUDE.md` → design canvas (01 §7.1) → 04 → 02 §7.8 → 06 (UI stories).
- **QA / reviewer:** 08 → 06 → 10 §12.

## Conventions

- Language: English for all documents and code; chat may be Vietnamese.
- Dates ISO 8601; times UTC unless a tenant time zone is stated; money as integer minor units with ISO currency.
- IDs: stories `TLY-<epic><nn>`, acceptance criteria `TLY-nnn-ACn`, requirements `FR-<AREA>-nn` / `NFR-<AREA>-nn`, scenarios `TS-nn`, risks `R-nn`, decisions `ADR-nnn`, slices `S<n>`, releases `R<n>`.
- RFC 2119 keywords (MUST, SHOULD, MAY) are normative where capitalized.
- Change control: docs change in the same PR as the behavior they describe; contract changes go through `contract/*` PRs; significant decisions get an ADR first.
- Generated docs: `06` and the tables in `04`/`07` are generated from `scripts/`; edit the source script, not the markdown.
- Versioning: this pack is v1.0; bump the minor version on each release (R1 = 1.1 …).

## ADR index

| ADR | Title | Status |
| --- | --- | --- |
| [ADR-001](adr/ADR-001-modular-monolith-core-with-go-workers.md) | Modular monolith core with Go workers | Accepted |
| [ADR-002](adr/ADR-002-pool-isolation-with-row-level-security-by-default-silo-tier.md) | Pool isolation with row-level security by default, silo tier for enterprise | Accepted |
| [ADR-003](adr/ADR-003-tenant-context-propagation-with-scopedvalue-and-set-local.md) | Tenant context propagation with ScopedValue and SET LOCAL | Accepted |
| [ADR-004](adr/ADR-004-money-as-integer-minor-units.md) | Money as integer minor units | Accepted |
| [ADR-005](adr/ADR-005-append-only-double-entry-ledger-with-database-enforced-balan.md) | Append-only double-entry ledger with database-enforced balance | Accepted |
| [ADR-006](adr/ADR-006-idempotency-keys-at-every-boundary.md) | Idempotency keys at every boundary | Accepted |
| [ADR-007](adr/ADR-007-identity-with-keycloak-realms-bff-sessions-and-hashed-api-ke.md) | Identity with Keycloak realms, BFF sessions and hashed API keys | Accepted |
| [ADR-008](adr/ADR-008-transactional-outbox-with-a-polling-relay.md) | Transactional outbox with a polling relay | Accepted |
| [ADR-009](adr/ADR-009-json-schema-for-event-contracts-protobuf-dropped.md) | JSON Schema for event contracts (Protobuf dropped) | Accepted |
| [ADR-010](adr/ADR-010-kafka-topics-keyed-by-tenant-id.md) | Kafka topics keyed by tenant_id | Accepted |
| [ADR-011](adr/ADR-011-dispatcher-durable-messages-in-postgres-per-tenant-fair-queu.md) | Dispatcher: durable messages in Postgres, per-tenant fair queues in Redis | Accepted |
| [ADR-012](adr/ADR-012-webhook-retries-circuit-breaker-dlq-and-auto-disable.md) | Webhook retries, circuit breaker, DLQ and auto-disable | Accepted |
| [ADR-013](adr/ADR-013-hmac-sha256-webhook-signatures-with-timestamp-and-rotation.md) | HMAC-SHA256 webhook signatures with timestamp and rotation | Accepted |
| [ADR-014](adr/ADR-014-subscription-lifecycle-as-an-explicit-state-machine.md) | Subscription lifecycle as an explicit state machine | Accepted |
| [ADR-015](adr/ADR-015-usage-aggregation-with-event-time-windows-and-exactly-once-c.md) | Usage aggregation with event-time windows and exactly-once counting | Accepted |
| [ADR-016](adr/ADR-016-single-writer-per-data-store.md) | Single writer per data store | Accepted |
| [ADR-017](adr/ADR-017-payment-processor-behind-a-port-with-a-simulator-adapter.md) | Payment processor behind a port with a simulator adapter | Accepted |
| [ADR-018](adr/ADR-018-opentelemetry-end-to-end-with-a-tenant-label-allow-list.md) | OpenTelemetry end to end with a tenant label allow-list | Accepted |
| [ADR-019](adr/ADR-019-per-tenant-rate-limiting-with-redis-token-buckets.md) | Per-tenant rate limiting with Redis token buckets | Accepted |
| [ADR-020](adr/ADR-020-per-tenant-envelope-encryption-and-crypto-shredding.md) | Per-tenant envelope encryption and crypto-shredding | Accepted |
| [ADR-021](adr/ADR-021-pool-to-silo-migration-by-logical-replication.md) | Pool-to-silo migration by logical replication | Proposed |
| [ADR-022](adr/ADR-022-separate-operator-console-api-port-and-identity-realm.md) | Separate operator console, API port and identity realm | Accepted |
| [ADR-023](adr/ADR-023-operator-access-to-tenant-data-through-the-same-rls-path-wit.md) | Operator access to tenant data through the same RLS path with a reason | Accepted |
| [ADR-024](adr/ADR-024-impersonation-via-oauth-token-exchange-with-an-act-claim.md) | Impersonation via OAuth token exchange with an act claim | Accepted |
| [ADR-025](adr/ADR-025-four-eyes-approvals-that-execute-stored-payloads.md) | Four-eyes approvals that execute stored payloads | Accepted |
| [ADR-026](adr/ADR-026-hash-chained-operator-audit-log.md) | Hash-chained operator audit log | Accepted |
| [ADR-027](adr/ADR-027-composite-tenant-foreign-keys.md) | Composite tenant foreign keys | Accepted |
| [ADR-028](adr/ADR-028-platform-plans-separate-from-the-tenant-catalog.md) | Platform plans separate from the tenant catalog | Accepted |
| [ADR-029](adr/ADR-029-runtime-stack-java-25-lts-spring-boot-4-go-1-27.md) | Runtime stack: Java 25 LTS, Spring Boot 4, Go 1.27 | Accepted |
| [ADR-030](adr/ADR-030-monorepo-with-lanes-and-contract-first-delivery.md) | Monorepo with lanes and contract-first delivery | Accepted |
| [ADR-031](adr/ADR-031-webhook-delivery-data-served-through-a-core-proxy.md) | Webhook delivery data served through a core proxy | Accepted |
