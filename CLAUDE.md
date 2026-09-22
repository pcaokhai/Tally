# CLAUDE.md — Tally

Tally is a multi-tenant payments and billing platform (learning project, production standards). Tenants call the **core** (Java 25, Spring Boot 4 modular monolith) over REST; the core writes to Postgres and publishes events through a transactional outbox to Kafka; Go **workers** deliver webhooks fairly per tenant and aggregate usage; two Next.js apps serve tenants (**web**) and operators (**ops-web**).

This file is loaded into every session. Keep it short. Detailed rules live in `docs/`; read the doc that matches your task before you plan.

## 1. Repository map

```
contracts/     OpenAPI 3.1 (tenant + ops), event JSON Schemas, webhook vectors, fixtures   → PLAT, tech lead only
core/          Java 25 · Spring Boot 4 · Spring Modulith · Postgres · Kafka producer        → core/CLAUDE.md
workers/       Go 1.27 · cmd/dispatcher (webhooks) · cmd/aggregator (usage rollups)        → workers/CLAUDE.md
web/           Next.js · tenant dashboard + BFF (session cookie, token exchange)           → web/CLAUDE.md
ops-web/       Next.js · operator console, dark dense theme, VPN-only                        → ops-web/CLAUDE.md
deploy/        docker compose (Postgres, Kafka, Redis, Keycloak, OTel/Grafana), k6, Toxiproxy → PLAT
docs/          PRD, architecture, API, data model, stories, delivery plan, tests, risks, ADRs
scripts/       contract generators (gen_openapi.py, gen_events.py, gen_webhook_vectors.py)
```

## 2. Which doc to read

| You are about to… | Read first |
| --- | --- |
| Start any story | docs/06 (story + AC), docs/07 (lane, slice, sprint, deps) |
| Touch webhook signing, headers, retries | docs/03, contracts/webhooks/signature-vectors.json |
| Touch REST (tenant or ops) | contracts/openapi.yaml or contracts/ops-openapi.yaml, docs/04 |
| Publish or consume a Kafka event | contracts/events/*.schema.json, docs/04 §7 |
| Touch tables or migrations | docs/05, docs/assets/ddl/ |
| Make a design choice | docs/02, docs/adr/ |
| Write tests | docs/08 |
| Write any code | docs/10 (summary in §6 below) |

Precedence when documents disagree: accepted ADR > contracts/ > docs/03 > docs/02 > docs/05 > docs/06 > this file. Record the conflict as a **Ruling** in your plan and open a doc-fix PR; never silently pick one.

## 3. Commands (scaffolded by TLY-001..003)

```
make up / make down        full local stack (deploy/compose.yaml); make up FLAGS=FF_S4_WEBHOOKS=true
make contracts             regenerate + lint + breaking-change check (Spectral, oasdiff, JSON Schema, vectors)
make gen                   regenerate server stubs, clients, MSW mocks from contracts/
make test                  unit + integration for every lane (Testcontainers; Docker required)
make lint / make fmt       Spotless+ArchUnit+Modulith verify · golangci-lint · eslint/prettier
make e2e                   Playwright journeys against the running stack
make seed                  load contracts/fixtures into a fresh stack
make load / make chaos     k6 skewed-tenant suite · Toxiproxy/broker-kill suite
make docs-check            python scripts/validate_pack.py .
```

## 4. How we work — Superpowers workflow (mandatory)

1. **brainstorming** — only if the story leaves a design decision open; otherwise write a one-paragraph understanding citing doc sections.
2. **using-git-worktrees** — one worktree + branch per story: `feat/<ID>-<slug>`; worktrees live in `../tally-worktrees/`.
3. **writing-plans** — save to `docs/plans/<ID>.md`; tasks of 2–5 min with exact files, the failing test first, and the verification command.
4. **subagent-driven-development** (default) or **executing-plans** (small or tightly coupled stories).
5. **test-driven-development** — RED → GREEN → REFACTOR, always. Test names carry the AC id.
6. **verification-before-completion** — run the commands and paste real output before claiming done.
7. **requesting-code-review** → **receiving-code-review**.
8. **finishing-a-development-branch** — rebase, green CI, squash-merge with a Conventional Commit title.

Bugs: **systematic-debugging** first — reproduce, root cause, regression test, fix. **dispatching-parallel-agents** only for tasks with disjoint file sets.

## 5. Parallel work rules

- **Contract first.** Any boundary change starts with a `contract/<slice>-<slug>` PR touching only `contracts/`, approved by the tech lead, merged on day 1 of the sprint.
- **Feature slices ship together.** Backend and frontend stories of a slice share one contract, run in parallel worktrees, merge behind one flag (`FF_<SLICE>_<NAME>`), release together (docs/07 §2).
- **Lanes own directories.** PLAT = `contracts/ deploy/ scripts/ .github/ Makefile docs/` · CORE = `core/` · WORK = `workers/` · WEB = `web/` · OPSW = `ops-web/`. Never edit another lane's directory; write a Ruling instead.
- **Frontend never waits.** Build against MSW mocks generated from contracts; the real API is `NEXT_PUBLIC_API_MODE=live`.
- **Migrations:** one owner per database; reserve the Flyway/goose number in your plan (docs/05 §5).
- **Integration checkpoint per slice:** `make up FLAGS=<flag>=true && make e2e` green before the flag turns on.

## 6. Engineering rules (summary — full text in docs/10)

1. **Tenant isolation is absolute.** Tenant id comes only from the credential. Every tenant table has `tenant_id`, RLS `FORCE`, composite FKs `(tenant_id, x_id)`. No code path disables RLS (ADR-002, ADR-027).
2. **Money is `long` minor units + ISO currency** in a `Money` value object. No `double`, `float`, or `BigDecimal` amounts anywhere (ADR-004).
3. **Ledger is append-only double-entry.** Postings sum to zero per currency (deferred constraint). Corrections are reversing entries. Never UPDATE/DELETE postings or finalized invoices (ADR-005).
4. **Idempotency at every boundary:** `Idempotency-Key` on money-moving/creating writes; dedup keys on every consumer; replays return the stored response (ADR-006).
5. **Events leave only through the outbox**, in the same transaction as the state change (ADR-008). Kafka key = `tenant_id`.
6. **No raw card data, secrets or unmasked PII in logs, events, fixtures or tests.** Processor tokens only (NFR-SEC-03).
7. **Single writer per store.** Go workers never write the core database; the core never writes the dispatcher database (ADR-016).
8. **Hexagonal layering** — domain has no framework imports and no I/O; enforced by ArchUnit/Spring Modulith (Java) and depguard (Go).
9. **Every I/O has a timeout; every goroutine/virtual thread has an owner and cancellation.** No unbounded queues.
10. **Errors:** business outcomes are values, not exceptions; one problem+json mapper per service; never leak stack traces or SQL.
11. **Observability:** structured JSON logs with `tenant.id`, `request_id`, `trace_id`; RED metrics per inbound interface; no high-cardinality labels.
12. **Config** from env with typed validation at startup; fail fast on missing config.
13. **Generated code is never edited by hand**; change the contract and run `make gen`.
14. **PRs:** one story, ≤ 400 changed lines excluding generated code, docs updated in the same PR.

## 7. Definition of Done

- [ ] Every AC proven by a named test (`should_<x>_when_<y>__TLY_nnn_ACn` (Java), `Test<Unit>_<Behavior>__TLY_nnn_ACn` (Go), `"… — TLY-nnn-ACn"` (TS)), failing before the change
- [ ] `make lint test contracts` green; security scan clean (gitleaks, dependency check)
- [ ] Logs, metrics, traces added for new behavior; no sensitive data in them
- [ ] docs/ updated in the same PR when behavior, contract or schema changed
- [ ] Slice partner merged behind the same flag (for slice stories)
- [ ] Code review done (requesting-code-review), plan kept in docs/plans/

## 8. Domain glossary (use these names in code)

Tenant (a Tally customer organization) · Customer (a tenant's own customer) · Operator (platform staff) · PlanVersion (what Tally charges tenants) · Product / Price (what a tenant charges its customers; prices are immutable versions) · Subscription / SubscriptionItem · Invoice (DRAFT → OPEN → PAID | VOID) · InvoiceLine · Meter / UsageEvent / UsageRollup · Payment / PaymentAttempt / Refund / DunningSchedule · LedgerAccount / JournalEntry / Posting · OutboxEvent / Event · WebhookEndpoint / EndpointSecret / DeliveryMessage / DeliveryAttempt / DLQ · ApprovalRequest (four-eyes) · Impersonation · AuditEntry · IsolationTier (POOL | SILO).

## 9. Things Claude must not do

- Edit `contracts/` in a feature branch, or hand-edit generated code.
- Add a dependency, service, topic or table not named in the story or docs without a Ruling.
- Weaken a test, gate, coverage threshold, RLS policy or ArchUnit rule to make CI pass.
- Use real personal data, real card numbers or real secrets anywhere, including tests.
- Bypass the idempotency filter, the outbox, the approval framework or the audit writer "just for this case".
- Merge with red CI, or claim done without pasted verification output.
