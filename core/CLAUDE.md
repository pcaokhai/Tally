# core — CLAUDE.md

The billing core: a Spring Boot 4 modular monolith (Java 25) that owns tenants, catalog, customers, subscriptions, invoicing, metering ingestion, payments, the ledger and the operator API. It is the **only writer** of the `tally` Postgres database and publishes every integration event through the transactional outbox. Lane: **CORE**. Owns: `core/**`, database `tally` (schemas below), migrations `core/src/main/resources/db/migration/`.

## Commands

```
./gradlew build                    compile + unit tests + Spotless check
./gradlew test                     unit tests (no Docker)
./gradlew integrationTest          Testcontainers: Postgres, Kafka, Redis, Keycloak
./gradlew spotlessApply            format
./gradlew archTest                 ArchUnit + Spring Modulith verify (also part of build)
./gradlew bootRun --args='--spring.profiles.active=local'
./gradlew openApiGenerate          regenerate interfaces from ../contracts/*.yaml (never edit output)
```

## Layout (one Spring Modulith module per bounded context)

```
com.tally.core
  kernel/           Money, TenantId, TenantContext (ScopedValue), idempotency filter, outbox, audit writer, problem mapper
  tenancy/          tenants, plan versions, memberships, invitations, API keys, branding, rate limits
  catalog/          products, versioned prices, tiers
  customers/        customers, payment methods (tokens only), timeline
  billing/          subscriptions (state machine), pricing engine + preview, invoices, billing run, reminders
  metering/         meters, usage ingest, rollup ingestion, usage summary/projection
  payments/         processor port + adapters, payments, attempts, dunning, refunds, reconciliation job
  ledger/           accounts, journal entries, postings, balance queries   (jOOQ, no JPA)
  events/           events table, request-log proxy, webhook endpoint registry + dispatcher query proxy
  reporting/        read models (revenue_daily, mrr_by_plan, attention items), projectors
  operations/       /ops/v1 API: approvals, impersonation, audit log, tenant ops, recon, plans, flags
Each module:  domain/ (pure) · application/ (use cases, ports in/out) · adapter/in (web, kafka) · adapter/out (db, kafka, http) · config/
```

Modules talk only through a module's `api` package or application events (Spring Modulith); no cross-module repositories, no cross-schema foreign keys. `./gradlew archTest` fails otherwise.

## Spring Boot 4 / Java 25 rules

- Constructor injection only; `final` fields; no field injection, no Lombok.
- Tenant context is a **`ScopedValue<TenantContext>`** (final in Java 25) bound in `TenantFilter` for the request; never `ThreadLocal`. Virtual threads are on (`spring.threads.virtual.enabled=true`); do not hold `synchronized` around I/O.
- Every transactional use case runs `SET LOCAL app.tenant_id = ?` via `TenantConnectionPreparer` before the first statement (ADR-003). Reads outside a transaction are forbidden for tenant tables (ArchUnit rule `noTenantRepositoryOutsideTransaction`).
- Operator API (`/ops/v1`) runs on a **separate port (8081)** with its own `SecurityFilterChain` bound to the operator realm; it never shares a filter chain with `/v1` (ADR-022).
- Jackson 3 is the JSON mapper; money is serialized only through `MoneyModule`.
- Preview features are not allowed (no `--enable-preview`); structured concurrency stays out until final.
- Use JSpecify `@NullMarked` packages; public methods never return `null`, `Optional` only as a return type.

## Domain rules

- `Money` (record: `long minor`, `Currency currency`) with `Math.addExact`; cross-currency arithmetic throws.
- Ledger postings are written by `LedgerService.post(JournalEntry)` only; a deferred constraint trigger rejects unbalanced entries at commit (docs/05 §3).
- Invoice finalize is one transaction: lock `invoice_number_seqs` row (`FOR UPDATE`), assign number, post receivable entry, insert outbox event.
- Subscription transitions go through `SubscriptionStateMachine`; illegal transitions throw `IllegalTransition` (maps to 409).
- Payment outcome unknown (processor timeout) ⇒ status stays `PROCESSING`, reconciliation resolves it; never guess success.
- Approval-gated operations are executed only by `ApprovalExecutor` from the stored payload after re-checking `payload_sha256` (ADR-025).

## Java rules (see docs/10 §4.2)

Records for value objects/commands/DTOs; sealed interfaces + pattern-matching `switch` for closed result sets; `List.copyOf` for immutability; checked exceptions only in adapters; functions ≤ 30 lines.

## Tests

- JUnit 5 + AssertJ; names `should_<behavior>_when_<condition>__TLY_nnn_ACn`.
- Integration tests extend `PostgresKafkaIT` (Testcontainers, reused containers); every repository test runs with RLS on and asserts a second tenant sees nothing.
- `CrossTenantAccessIT` (TLY-904) is generated from the OpenAPI spec and must stay green.
- Ledger: jqwik property tests (random entry sequences keep every balance invariant).
- Contract tests: responses validated against `contracts/openapi.yaml` with the OpenAPI request/response validator filter in tests.
