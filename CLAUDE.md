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

This is a **solo learning/showcase project** — one person, no other reviewers, no real tenant traffic. Keep the *engineering* discipline (TDD, review depth per §4a, RLS/ledger/idempotency rules) at full enterprise rigor — that rigor is the point of the project. Cut the *team-coordination* ceremony that exists only to protect against concurrent humans/agents you don't actually have running right now (see §4b, §5).

1. **brainstorming** — only if the story leaves a design decision open; otherwise write a one-paragraph understanding citing doc sections. Docs/06 and docs/07 already spec what to build and in what order — do not do market/library/"reference implementation" research for work the docs already fully decide; go straight to planning.
2. **Isolation.** Only use a worktree when ≥2 agents/sessions will genuinely run concurrently on disjoint stories (see §4b). For sequential, one-at-a-time work — the normal case for a solo session — skip the worktree: create `feat/<ID>-<slug>` directly off `main` in the main checkout and commit there. When a worktree is warranted, use the native tool available in your session (`EnterWorktree`, or `Agent(isolation:"worktree")`) — it owns placement (in practice `.claude/worktrees/`) and cleanup; don't also run `git worktree add` by hand on top of it. The native tool's auto-generated branch name (e.g. `worktree-agent-<id>`) won't match `feat/<ID>-<slug>` — rename it (`git branch -m feat/<ID>-<slug>`) as the first step, before any commits land.
3. **writing-plans** — save to `docs/plans/<ID>.md`; tasks of 2–5 min with exact files, the failing test first, and the verification command.
4. **subagent-driven-development** or **executing-plans** — picked by the story's risk track (§4a), not by default.
5. **test-driven-development** — RED → GREEN → REFACTOR, always. Test names carry the AC id.
6. **verification-before-completion** — run the commands and paste real output before claiming done.
7. **requesting-code-review** → **receiving-code-review**.
8. **finishing-a-development-branch** — green CI, squash-merge with a Conventional Commit title. Open the PR and merge it yourself once checks are green — there is no other reviewer to wait on; don't leave PRs open pending human approval. A PR is still worth opening (it's what triggers the GitHub Actions CI run and gives a durable diff/history), just don't treat "waiting for a reviewer" as a real step here.

Bugs: **systematic-debugging** first — reproduce, root cause, regression test, fix. **dispatching-parallel-agents** only for tasks with disjoint file sets. **Fix it yourself when you already know the root cause from a CI log or test failure you just read — don't dispatch a subagent to "investigate and fix" a cause you've already found.** That costs a full round trip to rediscover what you already know. Reserve a fresh dispatch for cases where the cause is genuinely unclear.

**Dispatch prompts point at files, they don't restate them.** A fresh subagent can read `docs/plans/<ID>.md`, `docs/06`, `docs/07`, and a sibling story's plan itself — tell it which file and which section, don't paste the content (a convention, an AC table, an SQL pattern) into the prompt. Copy in only what genuinely isn't written down anywhere yet (a decision made this turn, a fact only the dispatcher currently knows).

## 4a. Story risk track (triage before picking workflow depth)

Classify every story **before** writing its plan. Default to Track A; only Track B stories get the full ceremony below — applying Track B's review depth to Track A work is the single biggest source of wasted time and tokens (see `docs/releases/sprint-0.md` "Retrospective: process cost" for the numbers behind this rule).

**Track B (full ceremony)** — a story is Track B if it touches ANY of: money/ledger, RLS/tenant isolation, idempotency, auth/impersonation/four-eyes, webhook signing/delivery, payment processor integration, or any table migration carrying tenant data. Everything in docs/06 epics E2, E5, E7, E8 and most of E4/E6 is Track B by default.

**Track A (lightweight)** — everything else: scaffolding, CI/tooling, app shells, design tokens, docs generators, dev-stack config. Most of E0 and E3's platform stories are Track A.

| | Track A | Track B |
| --- | --- | --- |
| Execution | `executing-plans` (single session) or one dispatch per group of same-shape tasks, not one per task | `subagent-driven-development` as written below |
| Review | no dedicated review-agent dispatch — the orchestrating session itself independently rebuilds and reruns the real verification commands before merging; never trust a subagent's self-report alone | per-task review + fix loop + a dedicated independent review-agent dispatch against the diff + final whole-branch review + re-review — this is the one place a separate reviewer agent is worth its cost, because Track B is exactly the money/RLS/idempotency/auth/webhook-signing/payment surface where a fresh, unbiased pass has caught real bugs (races, missed ArchUnit coverage, security issues) |
| Model | Sonnet for the coordinator AND every implementer/reviewer it dispatches; Haiku for purely mechanical tasks (copy a config file, wire a script name) | Sonnet for the coordinator, every implementer, and every review pass including the final whole-branch review. No Opus tier — this project doesn't release to real users, and Sonnet's independent reviews have caught real bugs (races, security issues, missed ArchUnit coverage) without it. |
| Resume cap | 3 — past that, the coordinating session finishes the remaining work directly instead of resuming again | no hard cap; use the fix-loop breaker at round 5 as already specified |
| Before marking a task DONE | actually run the built artifact once (`curl`, open the page, run the binary) — not just unit tests in isolation; this is what catches the class of bug (CSP breaking hydration, a 404 route escaping a shared layout) that isolated tests miss, and catching it here is cheaper than catching it in final review | same, plus the full per-task review |

A story can be reclassified mid-plan if brainstorming surfaces a Track B concern in what looked like Track A work — record that as a ruling, don't silently apply Track B ceremony without saying why.

**Track B-small.** A Track B story that touches only 1-2 files with a small, well-understood blast radius (e.g. one new endpoint handler, one small migration, a bugfix in already-tested domain code) does not need the full multi-agent `subagent-driven-development` fan-out. Do it inline: TDD in the current session, one independent review pass before merge (worktree only if §4b's concurrency threshold is met). Reserve full multi-agent SDD for stories where the size or number of moving parts genuinely benefits from parallel task decomposition (a new state machine, a multi-table migration, a cross-module feature). When in doubt, start inline — escalate to full SDD mid-story if it turns out bigger than expected, rather than defaulting to the heaviest process.

**Trivial and doc-only changes skip the pipeline entirely.** A change under ~20 lines that touches no domain logic — a doc fix, a config typo, a one-line Makefile tweak, updating a comment — does not need a worktree, a plan file, or a dedicated review pass. Self-review the diff, run the relevant real check if one applies (e.g. `make docs-check` for a docs edit), and commit directly to the working branch. Reserve the full ceremony for changes that actually carry risk.

**Coordinating multiple stories in parallel:** before dispatching a review or fix wave into any worktree, check `ListAgents` first — never assume a sibling coordinator's own dispatch has finished. Two review/fix dispatches racing the same worktree index is a real failure mode, not a theoretical one. Model tiering (row above) already keeps most of the fleet on Sonnet — a most-capable-model dispatch should be rare and short-lived (one final review at a time), not a standing coordinator process, which is what actually avoids tripping a shared rate limit.

**Handback reports are delta-only.** When a dispatched coordinator resumes after being cut off, its report states only what changed since its last report (new commits, new findings, current blocker) and points at the plan/ledger file for full history. Do not re-paste the full ruling list or AC status table on every resume — that's pure restated context that compounds cost with every resume. The full history lives once, in the plan file; the handback is a diff against it.

**Tooling boundary — Superpowers only.** This repo's workflow is Superpowers (worktrees, plans, subagent-driven-development) and nothing else. Do not invoke a skill from outside that set for this repo, even if one looks like a plausible name match. When unsure, do the work directly instead of reaching for an unfamiliar skill on spec.

**Ponytail governs code, not process.** If Ponytail (lazy-dev) mode is active, it shapes *what you write* — simplest implementation that satisfies the AC, no speculative abstraction — never *whether* the workflow gates in §4/§4a run. A Track B story stays Track B under Ponytail; "keep it simple" means a simpler correct implementation, not a skipped test or review.

Known toolchain gotchas (Gradle/JDK, ESLint versions, etc.) that would otherwise be rediscovered per-story live in `docs/ENVIRONMENT.md` — read it before troubleshooting a build failure that looks environmental rather than code-related, and add to it when you find a new one.

## 4b. When to actually run things in parallel

Default to **one story at a time, sequentially, no worktree, no lane fence.** Only reach for worktrees + lane ownership (§5) when you are dispatching genuinely concurrent agents/sessions on disjoint stories in the same sprint wave — e.g. Sprint 1's CORE/WEB/PLAT stories fired off together. If you're driving one session through one story after another, none of §5's isolation machinery applies; just work on `main` (or a single `feat/<ID>-<slug>` branch) directly.

**Cap parallel dispatch at 2–3 subagents per wave.** Dispatching 4+ at once risks all of them hitting a session usage limit simultaneously and losing the whole wave to a reset wait. Stagger a bigger sprint wave into two smaller batches instead of firing everything at once.

## 5. Parallel work rules

Always-on, regardless of concurrency (these are correctness/safety practices, not team coordination):

- **Contract first.** Any boundary change starts with a `contract/<slice>-<slug>` PR touching only `contracts/`, merged before the stories that build on it — this is what `make contracts`' breaking-change gate protects, not a multi-person approval step.
- **Frontend never waits.** Build against MSW mocks generated from contracts; the real API is `NEXT_PUBLIC_API_MODE=live`.
- **Migrations:** one owner per database; reserve the Flyway/goose number in your plan (docs/05 §5).
- **Feature flags: don't create one unless something will actually flip it.** docs/07 §2 names a `FF_<SLICE>_<NAME>` flag per slice as the reference design, but skip it by default — pre-launch, with no real tenant traffic and no staged rollout to protect, a slice merges directly and goes live immediately. Only add a flag when you genuinely need to decouple two concurrently-running agents/sessions on the same slice, or a story's own AC explicitly calls for staged rollout. Don't scaffold `FF_S1_AUTH`-style flags "because the reference design has one" if nothing in this project will ever toggle them.

Only when §4b's concurrency threshold is actually met:

- **Lane ownership is real when ≥2 agents/sessions are actually running in parallel on different lanes** — PLAT = `contracts/ deploy/ scripts/ .github/ Makefile docs/` · CORE = `core/` · WORK = `workers/` · WEB = `web/` · OPSW = `ops-web/`. In that situation, don't edit another lane's directory; write a Ruling instead. Otherwise (§4b's default case) lane boundaries are just reading context, not a fence.
- **Integration checkpoint per slice, when a flag is actually in play:** `make up FLAGS=<flag>=true && make e2e` green before the flag turns on. Not applicable to a slice that skipped flag-gating per the point above — its own story-level `make lint test docs-check`/`make e2e` gate already covers it.

**Sprint bookkeeping is kept lightweight.** Use docs/07's sprint groupings (which stories, which order, what depends on what) — that's real planning value. Don't track points/capacity/buffer math, review-queue-age, or other multi-person velocity metrics from docs/07 §1/§11; there's no team throughput to manage. If docs/07's point numbers drift from reality, ignore them rather than reconciling them.

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
- [ ] For a slice story that used a flag (§5): partner side merged behind the same flag; if the slice skipped flag-gating (the default), this bullet doesn't apply
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
