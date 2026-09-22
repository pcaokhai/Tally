# Product Requirements Document — Tally

Version 1.0 · 2026-09-22 · Owner: Tech lead (Phan Cao Khai)

## 1. Summary

Tally is a multi-tenant payments and billing platform: tenants (SaaS businesses) use it to run subscriptions, usage-based pricing, invoices, payments and webhooks for their own customers, while platform operators run the fleet safely. It is built as a production-grade learning project that exercises ledger correctness, tenant isolation and fair webhook delivery end to end.

## 2. Contacts

| Name / role | Responsibility | Comment |
| --- | --- | --- |
| Phan Cao Khai — Tech lead, product owner | Scope, contracts, reviews every PR, owns PLAT lane | Review capacity is the delivery bottleneck (docs/07 §1) |
| CORE lane (Claude Code session) | `core/` — Java 25, Spring Boot 4 | One worktree per story |
| WORK lane (Claude Code session) | `workers/` — Go 1.27 dispatcher + aggregator | |
| WEB lane (Claude Code session) | `web/` — tenant dashboard + BFF | Builds on MSW mocks |
| OPSW lane (Claude Code session) | `ops-web/` — operator console | Starts Sprint 10 |
| PLAT lane (tech lead) | contracts, compose stack, CI, perf/chaos/security suites | |

## 3. Background

Billing SaaS is where multi-tenancy, money correctness and asynchronous delivery meet: one bug leaks another tenant's data, loses a cent, or floods a customer's endpoint. The design (architecture, requirements, API, ERD and 30 screen mockups) was completed in conversation and is now frozen into this pack. What makes it possible now: Java 25 LTS (final `ScopedValue`, virtual threads without pinning), Spring Boot 4 / Spring Modulith, Go 1.27, and AI coding lanes that can build backend and frontend in parallel from shared contracts.

## 4. Objective

Deliver a platform a senior engineer can demo, load-test and defend in a system-design interview, with every hard property proven by an automated test.

| Objective | Key result (measurable) |
| --- | --- |
| Money is always correct | 0 ledger invariant violations across 10⁶ property-test entries and all chaos runs; daily reconciliation 0 unexplained differences (NFR-COR-01, NFR-COR-05) |
| Tenants are isolated | Generated cross-tenant suite covers 100 % of tenant endpoints, 0 failures (NFR-SEC-01) |
| No event is lost | Broker-kill and relay-kill chaos runs: outbox count = consumed count, 0 duplicates delivered per (event, endpoint) (NFR-COR-02) |
| Fair under a noisy neighbor | With one tenant at 50 % of traffic, others' p99 API latency degrades ≤ 20 %; webhook p95 ≤ 10 % (NFR-FAIR-01/02) |
| Fast enough on a laptop | Writes p99 < 200 ms @ 200 rps; usage ingest 5,000 ev/s; ledger 1,000 entries/s (NFR-PER-01..04) |
| Engineering quality | Line coverage ≥ 80 % (ledger/money ≥ 90 % branch); mutation score ≥ 70 % on `ledger` and `billing`; 0 critical CVEs |
| Usable by non-experts | Tenant admin completes "create subscription → see first paid invoice" in < 3 min in a moderated test; WCAG 2.2 AA axe score 0 violations |
| Personal goal | 10 interview stories (decision · trade-off · measurement) backed by merged PRs and test output |

## 5. Market segments

| Segment (job to be done) | Constraints |
| --- | --- |
| SaaS founders and finance admins who need to bill seats + usage and get paid without building billing | Non-technical users; need plain-language money views and safe actions |
| Developers integrating billing into their product | Need a predictable API, idempotency, signed webhooks, logs to debug |
| Platform operators (support, SRE, finance) running the fleet | Need cross-tenant visibility without breaking isolation; every action audited |

## 6. Value propositions

| Job | Gain | Pain avoided | Why better than alternatives |
| --- | --- | --- | --- |
| Bill seats and usage | Versioned prices, proration preview, gap-free invoices | Surprise charges, broken invoice numbering | Preview uses the same pricing code as the real charge |
| Integrate reliably | Idempotent API, signed webhooks, replay, live delivery monitor | Double charges, silent webhook loss | Fair per-tenant delivery: one broken endpoint never delays others |
| Trust the numbers | Double-entry ledger, reconciliation | Books that don't match the processor | Invariants enforced by the database, not only code |
| Operate safely | Four-eyes approvals, time-boxed impersonation, hash-chained audit | Insider risk, untraceable changes | Least privilege by design, reason-before-access |

## 7. Solution

### 7.1 UX

Design canvas (30 artboards, 16 tenant + 14 operator, all interactive): https://claude.ai/artifact/2qhb5nQT1s9m9SCuUoqHJu · Requirements, API & data model working doc: https://claude.ai/artifact/LeZjTXFZyExjwoo7kBEJr5. Screens are not re-described here; stories link to them by name.

### 7.2 Key features

| Feature | Description | Epic |
| --- | --- | --- |
| Platform & contracts | Monorepo, local stack, contract pipeline, service skeletons | E0 |
| Tenancy, identity & access | Tenant registry, RLS isolation, Keycloak + BFF, API keys, team, branding, rate limits | E1 |
| Ledger & money | Money type, double-entry ledger, balance queries | E2 |
| Events & webhooks | Outbox, event log, endpoints, fair dispatcher, retries/DLQ, monitor, request logs | E3 |
| Catalog & customers | Products with immutable price versions, customers, tokenized payment methods | E4 |
| Subscriptions & invoicing | State machine, pricing preview, billing run, finalize/void, reminders, test clocks | E5 |
| Metering & usage | Ingest, aggregator rollups, usage summary & projection, usage explorer | E6 |
| Payments & dashboard | Processor port, payments, dunning, refunds, reconciliation, Home read models | E7 |
| Operator console | Operator realm, approvals, audit, tenants, impersonation, health, fleet, jobs, money ops, config | E8 |
| Hardening | Observability, load, chaos, security suites, crypto-shred offboarding, silo migration | E9 |

### 7.3 Technology

Java 25 + Spring Boot 4 modular monolith for the correctness-heavy core; Go 1.27 workers for fan-out delivery and aggregation; PostgreSQL with RLS; Kafka with a transactional outbox; Redis for scheduler state; Keycloak (tenant realm + operator realm); Next.js for both frontends. Details in docs/02 §9.

### 7.4 Assumptions

| ID | Assumption | How validated | By |
| --- | --- | --- | --- |
| A-01 | One laptop (32 GB) runs the full stack within 16 GB | `make up` memory report | TLY-002, Sprint 0 |
| A-02 | Stripe test mode + in-repo simulator are enough to exercise every payment path | Simulator tokens cover success, decline, timeout, duplicate settle | TLY-701, Sprint 9 |
| A-03 | Spring Modulith boundaries hold without splitting services | `archTest` green every sprint; no cross-module repository access | TLY-004 onward |
| A-04 | Redis-based per-tenant queues give fairness within NFR-FAIR-02 | Fairness test with 100k-message backlog | TLY-305, Sprint 4 |
| A-05 | One reviewer can sustain ~30 points/sprint across 5 lanes | Review-queue age < 1 day tracked per sprint | docs/07 §10, every sprint |
| A-06 | JSON Schema is sufficient for internal events (Protobuf dropped, ADR-009) | Codegen works for Java + Go; CI compatibility check catches breaks | TLY-003, Sprint 0 |
| A-07 | Polling outbox relay meets p99 < 1 s lag at target load | k6 run with relay lag metric | TLY-902, Sprint 13 |

## 8. Release

| Release | Content | When |
| --- | --- | --- |
| R0 Walking skeleton | Stack up, contracts pipeline, all five skeletons deploy and pass a smoke test | End of Sprint 0 |
| R1 Developer platform | Tenancy + isolation, auth, API keys, team/settings, ledger, outbox, webhooks end to end, events & logs, products | End of Sprint 5 |
| R2 Billing | Customers, subscriptions, invoices, usage metering and explorer | End of Sprint 8 |
| R3 Payments & dashboard | Payments, dunning, refunds, reconciliation, Home dashboard | End of Sprint 10 |
| R4 Operator console | All 14 operator screens and APIs, four-eyes, impersonation, audit | End of Sprint 13 |
| R5 Hardening | Load, chaos, security suites green; crypto-shred offboarding; pool→silo migration | End of Sprint 15 |

Out of scope: tax calculation, FX and multi-currency invoices, real card processing and settlement, disputes/chargebacks, multi-region data residency, mobile apps, public marketing site.
