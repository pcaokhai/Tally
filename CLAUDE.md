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
2. **Isolation, one mechanism only.** One worktree + branch per story: `feat/<ID>-<slug>`. Use the native tool available in your session (`EnterWorktree`, or `Agent(isolation:"worktree")` when dispatching a top-level story-delivery agent) — it owns placement (in practice `.claude/worktrees/`) and cleanup. Do **not** also run `git worktree add` by hand or have a dispatched agent re-run the using-git-worktrees skill's own manual-fallback steps inside a directory a native tool already isolated — that creates a second, redundant worktree layer for no benefit. Only fall back to manual `git worktree add` when no native tool is available in that session. The native tool's auto-generated branch name (e.g. `worktree-agent-<id>`) won't match `feat/<ID>-<slug>` — rename it (`git branch -m feat/<ID>-<slug>`) as the first step after the worktree is created, before any commits land, so every commit on the branch carries the right name from the start.
3. **writing-plans** — save to `docs/plans/<ID>.md`; tasks of 2–5 min with exact files, the failing test first, and the verification command.
4. **subagent-driven-development** or **executing-plans** — picked by the story's risk track (§4a), not by default.
5. **test-driven-development** — RED → GREEN → REFACTOR, always. Test names carry the AC id.
6. **verification-before-completion** — run the commands and paste real output before claiming done.
7. **requesting-code-review** → **receiving-code-review**.
8. **finishing-a-development-branch** — rebase, green CI, squash-merge with a Conventional Commit title.

Bugs: **systematic-debugging** first — reproduce, root cause, regression test, fix. **dispatching-parallel-agents** only for tasks with disjoint file sets.

## 4a. Story risk track (triage before picking workflow depth)

Classify every story **before** writing its plan. Default to Track A; only Track B stories get the full ceremony below — applying Track B's review depth to Track A work is the single biggest source of wasted time and tokens observed in Sprint 0 (TLY-003 alone needed 15+ resumes running full multi-round review on pure scaffolding).

**Track B (full ceremony)** — a story is Track B if it touches ANY of: money/ledger, RLS/tenant isolation, idempotency, auth/impersonation/four-eyes, webhook signing/delivery, payment processor integration, or any table migration carrying tenant data. Everything in docs/06 epics E2, E5, E7, E8 and most of E4/E6 is Track B by default.

**Track A (lightweight)** — everything else: scaffolding, CI/tooling, app shells, design tokens, docs generators, dev-stack config. Most of E0 and E3's platform stories are Track A.

| | Track A | Track B |
| --- | --- | --- |
| Execution | `executing-plans` (single session) or one dispatch per group of same-shape tasks, not one per task | `subagent-driven-development` as written below |
| Review | one review pass + one light final pass | per-task review + fix loop + final whole-branch review + re-review |
| Model | Sonnet for the coordinator AND every implementer/reviewer it dispatches; Haiku for purely mechanical tasks (copy a config file, wire a script name) | Sonnet for the coordinator and every implementer. **Opus is used exactly once per story: the single final whole-branch review**, dispatched as one call after all tasks are committed. Never Opus for the coordinator's own routine turns, never Opus for a per-task review, never more than one Opus dispatch per story. |
| Resume cap | 3 — past that, the coordinating session finishes the remaining work directly instead of resuming again | no hard cap; use the fix-loop breaker at round 5 as already specified |
| Before marking a task DONE | actually run the built artifact once (`curl`, open the page, run the binary) — not just unit tests in isolation; this is what catches the class of bug (CSP breaking hydration, a 404 route escaping a shared layout) that isolated tests miss, and catching it here is cheaper than catching it in final review | same, plus the full per-task review |

A story can be reclassified mid-plan if brainstorming surfaces a Track B concern in what looked like Track A work — record that as a ruling, don't silently apply Track B ceremony without saying why.

**Track B-small.** A Track B story that touches only 1-2 files with a small, well-understood blast radius (e.g. one new endpoint handler, one small migration, a bugfix in already-tested domain code) does not need the full multi-agent `subagent-driven-development` fan-out. Do it inline: TDD in the current session, one independent review pass before merge, still worktree-isolated. Reserve full multi-agent SDD for stories where the size or number of moving parts genuinely benefits from parallel task decomposition (a new state machine, a multi-table migration, a cross-module feature). When in doubt, start inline — escalate to full SDD mid-story if it turns out bigger than expected, rather than defaulting to the heaviest process.

**Trivial and doc-only changes skip the pipeline entirely.** A change under ~20 lines that touches no domain logic — a doc fix, a config typo, a one-line Makefile tweak, updating a comment — does not need a worktree, a plan file, or a dedicated review pass. Self-review the diff, run the relevant real check if one applies (e.g. `make docs-check` for a docs edit), and commit directly to the working branch. Reserve the full ceremony for changes that actually carry risk.

**Coordinating multiple stories in parallel:** before dispatching a review or fix wave into any worktree, check `ListAgents` first — never assume a sibling coordinator's own dispatch has finished. Two review/fix dispatches racing the same worktree index is a real failure mode, not a theoretical one. Model tiering (row above) already keeps most of the fleet on Sonnet — a most-capable-model dispatch should be rare and short-lived (one final review at a time), not a standing coordinator process, which is what actually avoids tripping a shared rate limit.

**Handback reports are delta-only.** When a dispatched coordinator resumes after being cut off, its report states only what changed since its last report (new commits, new findings, current blocker) and points at the plan/ledger file for full history. Do not re-paste the full ruling list or AC status table on every resume — that's pure restated context, and it's what made Sprint 0's longest-running story (TLY-003) grow slower with every resume instead of faster. The full history lives once, in the plan file; the handback is a diff against it.

**Tooling boundary — Superpowers only.** This repo's workflow is Superpowers (worktrees, plans, subagent-driven-development) and nothing else. Do not invoke a skill from outside that set for this repo, even if one looks like a plausible name match (Sprint 0 wasted a pass on a mismatched skill that assumed a README/CHANGELOG/VERSION layout this repo doesn't use). When unsure, do the work directly instead of reaching for an unfamiliar skill on spec.

**Ponytail governs code, not process.** If Ponytail (lazy-dev) mode is active, it shapes *what you write* — simplest implementation that satisfies the AC, no speculative abstraction — never *whether* the workflow gates in §4/§4a run. A Track B story stays Track B under Ponytail; "keep it simple" means a simpler correct implementation, not a skipped test or review.

Known toolchain gotchas (Gradle/JDK, ESLint versions, etc.) that would otherwise be rediscovered per-story live in `docs/ENVIRONMENT.md` — read it before troubleshooting a build failure that looks environmental rather than code-related, and add to it when you find a new one.

## 5. Parallel work rules

- **Contract first.** Any boundary change starts with a `contract/<slice>-<slug>` PR touching only `contracts/`, approved by the tech lead, merged on day 1 of the sprint.
- **Feature flags are pre-launch-optional.** docs/07 §2 still names a `FF_<SLICE>_<NAME>` flag per slice as the reference design — keep using one when you genuinely need to decouple two concurrently-running agents/sessions on the same slice, or when a story's own AC calls for staged rollout. Otherwise, pre-launch (no real tenant traffic on `main` yet), a slice may merge directly and go live immediately without flag-gating — don't pay for flag plumbing and cleanup that has no rollout audience to protect yet. Revisit this once there's real traffic.
- **Lane ownership is real when ≥2 agents/sessions are actually running in parallel on different lanes** — PLAT = `contracts/ deploy/ scripts/ .github/ Makefile docs/` · CORE = `core/` · WORK = `workers/` · WEB = `web/` · OPSW = `ops-web/`. In that situation, don't edit another lane's directory; write a Ruling instead. Working solo/sequentially (one agent, one story at a time), lane boundaries are advisory context (which doc to read, whose plan owns what), not a hard fence — there's no concurrent writer to protect against.
- **Frontend never waits.** Build against MSW mocks generated from contracts; the real API is `NEXT_PUBLIC_API_MODE=live`.
- **Migrations:** one owner per database; reserve the Flyway/goose number in your plan (docs/05 §5).
- **Integration checkpoint per slice, when a flag is actually in play:** `make up FLAGS=<flag>=true && make e2e` green before the flag turns on. Not applicable to a slice that skipped flag-gating per the point above — its own story-level `make lint test docs-check`/`make e2e` gate already covers it.

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
14. **PRs:** one story, ≤ 400 changed lines excluding generated code, docs updated in the same PR. A from-scratch Track A scaffold (a new app shell, a new lane's toolchain) is exempt by default — splitting a first-time scaffold into multiple PRs usually produces an untestable partial state — but record the exemption as a ruling instead of silently going over.

## 7. Definition of Done

- [ ] Every AC proven by a named test (`should_<x>_when_<y>__TLY_nnn_ACn` (Java), `Test<Unit>_<Behavior>__TLY_nnn_ACn` (Go), `"… — TLY-nnn-ACn"` (TS)), failing before the change
- [ ] `make lint test contracts` green; security scan clean (gitleaks, dependency check)
- [ ] Logs, metrics, traces added for new behavior; no sensitive data in them
- [ ] docs/ updated in the same PR when behavior, contract or schema changed
- [ ] Slice partner merged behind the same flag (for slice stories)
- [ ] Review done at the depth §4a's track calls for (self-review for trivial, one pass for Track A/B-small, full requesting-code-review chain for Track B), plan kept in docs/plans/

## 8. Domain glossary (use these names in code)

Tenant (a Tally customer organization) · Customer (a tenant's own customer) · Operator (platform staff) · PlanVersion (what Tally charges tenants) · Product / Price (what a tenant charges its customers; prices are immutable versions) · Subscription / SubscriptionItem · Invoice (DRAFT → OPEN → PAID | VOID) · InvoiceLine · Meter / UsageEvent / UsageRollup · Payment / PaymentAttempt / Refund / DunningSchedule · LedgerAccount / JournalEntry / Posting · OutboxEvent / Event · WebhookEndpoint / EndpointSecret / DeliveryMessage / DeliveryAttempt / DLQ · ApprovalRequest (four-eyes) · Impersonation · AuditEntry · IsolationTier (POOL | SILO).

## 9. Things Claude must not do

- Edit `contracts/` in a feature branch, or hand-edit generated code.
- Add a dependency, service, topic or table not named in the story or docs without a Ruling.
- Weaken a test, gate, coverage threshold, RLS policy or ArchUnit rule to make CI pass.
- Use real personal data, real card numbers or real secrets anywhere, including tests.
- Bypass the idempotency filter, the outbox, the approval framework or the audit writer "just for this case".
- Merge with red CI, or claim done without pasted verification output.
