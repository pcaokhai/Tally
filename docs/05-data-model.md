# Data Model

Version 1.0 · 2026-09-22 · Owner: Tech lead · Normative source: `docs/assets/ddl/tally-baseline.sql`, `docs/assets/ddl/dispatcher-baseline.sql`

Both DDL files parse with the PostgreSQL parser (pglast). They become the first Flyway (core) and goose (dispatcher) migrations; after that, migrations are the source of truth and these files are regenerated from a migrated database by `make ddl-snapshot`.

## 1. Ownership

| Database | Owner service (only writer) | Migration tool | Location |
| --- | --- | --- | --- |
| `tally` (Postgres) | core | Flyway | `core/src/main/resources/db/migration/V<nnn>__<slug>.sql` |
| `dispatcher` (Postgres) | dispatcher | goose | `workers/migrations/<nnn>_<slug>.sql` |
| Redis `wh:*` | dispatcher | n/a (cache of scheduling state; rebuildable from Postgres) | `workers/internal/scheduler` |
| Kafka compacted `tally.usage.agg-state.v1` | aggregator | topic config in `deploy/kafka/topics.yaml` | PLAT |
| `keycloak` (Postgres) | Keycloak | realm export JSON | `deploy/keycloak/*.json` |
| Loki / ClickHouse request logs | OTel collector | retention config | `deploy/otel/` |

## 2. Tables

| Schema | Table | Purpose | Key constraints |
| --- | --- | --- | --- |
| tenancy | plan_versions | What Tally charges tenants (ADR-028) | UQ (plan_code, version); `limits` jsonb |
| tenancy | tenants | Tenant registry | `isolation_tier` POOL/SILO; `dek_wrapped` for crypto-shred |
| tenancy | users, memberships, invitations | Identity mirror, roles per tenant, 7-day invites | PK (tenant_id, user_id) |
| tenancy | api_keys | Scoped keys | `prefix` UQ, Argon2id `secret_hash`, `last4` |
| tenancy | tenant_branding, attention_dismissals | Settings and Home | PK tenant_id / (tenant, user, item) |
| customers | customers | Tenant's customers | UQ (tenant_id, email_hash); email encrypted |
| customers | payment_methods | Processor tokens only | composite FK to customer |
| catalog | products, prices, price_tiers | Tenant catalog; prices immutable versions | UQ (tenant_id, product_id, version); immutability trigger |
| billing | subscriptions, subscription_items | Lifecycle + items | status CHECK; `version` for ETag |
| billing | invoice_number_seqs | Gap-free numbering | one row per tenant, locked on finalize |
| billing | invoices, invoice_lines | Drafts and finalized invoices | CHECK draft ⇔ `finalized_at IS NULL`; lines frozen trigger |
| billing | credit_notes, invoice_reminders, test_clocks | Corrections, reminders, test time | composite FKs |
| metering | meters | Meter definitions | UQ (tenant_id, key) |
| metering | usage_events (partitioned monthly), usage_event_ids | Raw usage + dedup | PK (tenant_id, event_id) on `usage_event_ids` |
| metering | usage_rollups, usage_thresholds | Aggregated usage | UQ `rollup_key` (idempotent upsert) |
| payments | payments, payment_attempts, dunning_schedules, refunds | Money movement | UQ (payment_id, attempt_no); amount > 0 |
| ledger | accounts, journal_entries, postings | Double-entry | deferred balance trigger; no UPDATE/DELETE grants |
| kernel | outbox_events, inbox, idempotency_keys, audit_log | Platform plumbing | partial index on unpublished outbox |
| events | events, webhook_endpoints, endpoint_secrets | Event log (30 d), endpoint registry, secrets | secrets encrypted |
| reporting | revenue_daily, mrr_by_product, tenant_health | Read models | rebuilt from events |
| ops | operators, operator_roles, approval_requests, impersonations, operator_audit_log | Governance | CHECK decided_by ≠ requested_by; write impersonation requires approval; audit append-only, hash-chained |
| ops | recon_runs, recon_mismatches, feature_flags, flag_targets, rate_limit_overrides, job_runs, data_exports | Operations | UQ job_runs.idempotency_key |
| dispatcher | endpoint_projection, delivery_messages, delivery_attempts, endpoint_health_hourly, replays | Delivery state | UQ (event_id, endpoint_id) where not a replay |

## 3. Modeling rules

- **Tenant scoping:** every tenant table has `tenant_id uuid NOT NULL`, RLS `ENABLE` + `FORCE`, policy `tenant_id = kernel.current_tenant()` for USING and WITH CHECK. `kernel.current_tenant()` returns NULL when unset, so a missing `SET LOCAL` returns zero rows instead of all rows (fails closed).
- **Composite foreign keys** `(tenant_id, x_id) → (tenant_id, id)` on every intra-tenant reference; referenced tables carry `UNIQUE (tenant_id, id)` (ADR-027).
- **IDs:** UUIDv7 generated in the application; public ids are prefixed encodings (`cus_…`) produced at the API boundary. Events use text ids `evt_…`.
- **Money:** `bigint` minor units + `char(3)` currency; `CHECK (amount_minor > 0)` where a sign is implied by direction; no `numeric` for money (usage quantities use `numeric(20,6)`).
- **Statuses:** `text` + `CHECK (… IN …)`, mirrored by Java enums and OpenAPI enums (SCREAMING_SNAKE_CASE).
- **Time:** `timestamptz` only; business dates (`due_date`, `run_date`) as `date` in the tenant's billing time zone.
- **Immutability:** postings, journal entries, finalized invoices and their lines, used prices and the operator audit log are never updated or deleted (triggers + revoked grants).
- **Sensitive data:** customer email encrypted with the tenant DEK plus a keyed hash for lookups; webhook/API secrets hashed or encrypted; no card data exists.
- **Naming:** snake_case, plural tables, `<table>_<cols>` indexes, `fk_`/`uq_` implicit names allowed.
- **Partitioning:** `metering.usage_events` monthly by `received_at`; partitions older than 13 months detached and archived.

## 4. Deltas from the design discussion

| Table | Change | Reason | Story |
| --- | --- | --- | --- |
| `events.*` | Webhook endpoints, secrets and events moved from `kernel` to their own `events` schema | One schema per module (core `events` module owns them) | TLY-302, TLY-303 |
| customers.customers | `email` replaced by `email_enc` + `email_hash`; uniqueness on the hash | Encrypt PII at rest while keeping FR-CUS-03 | TLY-403 |
| metering.usage_events | Partitioned; dedup moved to `usage_event_ids` PK | A partitioned table's PK must include the partition key | TLY-601 |
| kernel.inbox | New | Idempotent Kafka consumption in the core (rollups, control events) | TLY-301 |
| tenancy.tenants | `dek_wrapped` added | Crypto-shred offboarding (ADR-020) | TLY-905 |
| billing.test_clocks, subscriptions.test_clock_id | New | FR-SUB-05 | TLY-508 |
| ledger.journal_entries | `reverses_id` added | Link reversal to original for voids/refunds | TLY-202 |
| billing.invoices | CHECK draft ⇔ `finalized_at IS NULL` | Make illegal states unrepresentable | TLY-505 |
| reporting.mrr_by_product | Renamed from `mrr_by_plan_daily`; snapshot not daily | Home only needs current split; history comes from revenue_daily | TLY-706 |
| dispatcher.endpoint_projection | New | Dispatcher's local copy of endpoints from control events (single writer) | TLY-304 |
| ops.data_exports | New | FR-TEN-06 | TLY-905 |
| Event schemas | Protobuf dropped in favor of JSON Schema | One schema language for internal events and public webhooks (ADR-009) | TLY-003 |

## 5. Migration rules

- Forward-only; never edit a merged migration.
- **Expand → migrate → contract** across at least two releases for any rename or type change (NFR-AVL-05).
- Reserve numbers in the story plan: core `V100–V199` E1, `V200–V299` E2, … `V900–V999` E9 (epic × 100); dispatcher `001–099`.
- No data changes except reference data (plan versions, ledger chart of accounts seed) in migrations; backfills are jobs.
- Every migration runs in CI against a copy of the fixtures and must keep RLS enabled on all tenant tables (`RlsCoverageIT`).

## 6. Key queries and indexes

| Query (screen / job) | Index |
| --- | --- |
| Invoices by status + overdue (Invoices tabs, reminders) | `invoices_status_due (tenant_id, status, due_date)` |
| Payments list by status (Payments) | `payments_status_created (tenant_id, status, created_at DESC)` |
| Renewing this week, billing run | `subscriptions_status_period (tenant_id, status, current_period_end)` |
| Rollup upsert / usage summary | `usage_rollups_lookup (tenant_id, meter_id, customer_id, period_start)` + UQ `rollup_key` |
| Relay claim | `outbox_unpublished (created_at) WHERE published_at IS NULL` + `FOR UPDATE SKIP LOCKED` |
| Events list (Events & logs) | `events_type_created (tenant_id, type, created_at DESC)` |
| Account balance as of | `postings_account (tenant_id, account_id, journal_entry_id)` |
| Customers list | `customers_created (tenant_id, created_at DESC)` |
| Dispatcher due work | `delivery_due (tenant_id, status, next_attempt_at)` |
| Tenant access log (ops) | `operator_audit_tenant (tenant_id, at DESC)` |
