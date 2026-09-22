# Test Strategy and Scenarios

Version 1.0 · 2026-09-22 · Owner: Tech lead

## 1. Principles

- **Test first.** Every AC gets a failing test before code (test-driven-development). Test names carry the AC id: `should_x_when_y__TLY_505_AC2` (Java), `TestFinalize_GapFree__TLY_505_AC2` (Go), `it("… — TLY-505-AC2")` (TS).
- **Invariants are asserted, not assumed:** ledger balance, tenant isolation, exactly-once effects, gap-free numbering.
- **Real infrastructure for integration:** Testcontainers for Postgres, Kafka, Redis, Keycloak; no H2, no embedded Kafka.
- **Determinism:** injected `Clock` (fixed), seeded randomness (jqwik seeds logged), fake processor via the simulator; no `sleep` in tests (use fake clocks or awaitility with bounds).
- **Fixtures only** from `contracts/fixtures`; never real personal or card data.

## 2. Test pyramid

| Level | Scope | Tools | Owner lane | Runs |
| --- | --- | --- | --- | --- |
| Unit | domain + application with fakes | JUnit 5/AssertJ, Go testing, Vitest | each | every commit |
| Property | ledger, money, pricing, proration, rollups | jqwik, Go `testing/quick` + rapid | CORE, WORK | every commit |
| Architecture | module boundaries, forbidden types/imports | Spring Modulith verify, ArchUnit, depguard | CORE, WORK | every commit |
| Integration | adapters with real infra, RLS on | Testcontainers (Java + Go) | CORE, WORK | every PR |
| Contract (provider) | responses/events validate against `contracts/` | OpenAPI validator filter, JSON Schema validator | CORE, WORK | every PR |
| Contract (consumer) | UI against MSW mocks from the same spec | MSW + Vitest | WEB, OPSW | every PR |
| Component / visual | every UI state incl. reduced motion | Storybook + Playwright screenshots, axe | WEB, OPSW | every PR |
| E2E | slice journeys across the whole stack | Playwright, `make up` | PLAT | nightly + integration checkpoint |
| Security | cross-tenant, SSRF, secrets, DAST | generated IDOR suite, SSRF suite, log scan, ZAP | PLAT | nightly |
| Chaos / resilience | kill/partition/latency | Toxiproxy, container kill | PLAT | nightly (from Sprint 14), before release |
| Performance | throughput, latency, fairness | k6, JMH | PLAT | weekly + before release |

## 3. Quality gates

| Gate | Threshold | Blocks |
| --- | --- | --- |
| Line coverage | ≥ 80 % per module | merge |
| Branch coverage `kernel.money`, `ledger`, `billing.pricing` | ≥ 90 % | merge |
| Mutation score (PIT) `ledger`, `billing` | ≥ 70 % | release |
| Static analysis | 0 new errors (Error Prone, golangci-lint, eslint) | merge |
| Architecture tests | 0 violations | merge |
| Contracts | Spectral 0 errors; oasdiff no unapproved breaking change; all provider contract tests green | merge |
| Security | gitleaks 0; 0 critical/high CVEs; IDOR suite 0 failures; ZAP 0 high | merge (first three), release (ZAP) |
| E2E | all journeys of slices being released green | flag on |
| Performance | k6 thresholds = NFR-PER/FAIR numbers in docs/02 §2.2 | release |
| Accessibility | axe 0 violations on changed screens | merge |

## 4. Test data rules

- Tenants, customers and processor tokens only from `contracts/fixtures/tenants.json`; builders in `testFixtures` (Java) and `internal/testfx` (Go) create variants.
- Emails use `.example`; card data never exists — tokens only.
- Every integration test creates at least two tenants and asserts the second sees nothing of the first.
- Fixed clock `2026-09-22T07:00:00Z` by default; time-dependent tests advance it explicitly.

## 5. Critical E2E journeys

| Journey | Slices | Tag |
| --- | --- | --- |
| J1 Sign in, switch tenant, caches cleared | S1 | @S1 |
| J2 Create API key, call API with it, revoke, call fails within 5 s | S2 | @S2 |
| J3 Register endpoint → finalize invoice → signed webhook received → replay from monitor | S4, S9 | @S4 |
| J4 Create product and price → customer → subscription with preview → billing run → finalize → pay → Home shows revenue | S6–S9, S11, S12 | @S8 |
| J5 Ingest usage (with duplicates) → rollup → metered invoice line matches | S10, S9 | @S10 |
| J6 Failed payment → dunning via test clock → PAST_DUE → recovery → ACTIVE | S11, S8 | @S11 |
| J7 Operator suspends tenant (requester + approver + step-up) → tenant writes 423 → audit chain verified | S13, S14 | @S13 |
| J8 Read-only impersonation → tenant banner → write blocked → tenant sees history | S15 | @S15 |

## 6. Scenarios

**TS-01 — Cross-tenant access is impossible** (TLY-102, TLY-904)
Objective: no endpoint leaks or mutates another tenant's data.
Starting conditions: tenants A and B from fixtures, each with one object of every type; API keys for both.
Role: tenant developer (B).
Steps: 1. For every GET/PATCH/POST/DELETE path with an id, call it with B's key and A's id → 404 NOT_FOUND. 2. For every list endpoint, call with B's key → none of A's ids appear. 3. Send `tenant_id` of A in header, query and body → ignored, B's data only.
Expected outcomes: 0 cross-tenant reads or writes; responses never reveal existence; test generated from `contracts/openapi.yaml` so new endpoints are covered automatically.

**TS-02 — Idempotent payment creation** (TLY-110, TLY-702)
Objective: retries never double-charge.
Starting conditions: tenant A, customer with `pm_sim_ok_4242`, open invoice $1,250.00.
Role: tenant backend.
Steps: 1. POST /v1/payments with key K → 201 SUCCEEDED. 2. Repeat same body + K → same response, header `Idempotent-Replayed: true`. 3. Same K, amount changed → 422 IDEMPOTENCY_KEY_REUSED. 4. Two concurrent requests with new key K2 → one 201, one 409 IDEMPOTENCY_IN_PROGRESS or the replay.
Expected outcomes: exactly one processor charge in the simulator log; one ledger entry; invoice PAID once.

**TS-03 — Unknown processor outcome** (TLY-701, TLY-702)
Objective: timeouts never become guessed outcomes.
Starting conditions: payment method `pm_sim_timeout`.
Role: tenant backend.
Steps: 1. Create payment → 201 with status PROCESSING. 2. Check ledger → no entry. 3. Simulator later reports success; status poller runs → SUCCEEDED. 4. Ledger → one entry.
Expected outcomes: no ledger entry while unknown; exactly one after resolution; `payment.succeeded` emitted once.

**TS-04 — Gap-free invoice numbers under concurrency** (TLY-505)
Objective: numbering is sequential with no gaps.
Starting conditions: 50 draft invoices for tenant A; 5 of them made to fail after numbering by a test hook.
Role: platform.
Steps: 1. Finalize all 50 concurrently. 2. Collect assigned numbers.
Expected outcomes: 45 finalized invoices with consecutive numbers starting at the next value, no gaps, no duplicates; the 5 failures consumed no number.

**TS-05 — Ledger balances under any sequence** (TLY-202)
Objective: the books always balance.
Starting conditions: empty ledger for tenant A.
Role: platform.
Steps: 1. jqwik generates 10⁶ random valid postings across accounts. 2. Attempt a raw-SQL unbalanced insert. 3. Attempt UPDATE on a posting as the app role.
Expected outcomes: trial balance 0; every account balance equals the sum of its postings; step 2 fails at commit; step 3 is denied.

**TS-06 — Webhook signature verification** (TLY-305, TLY-312)
Objective: receivers can verify every signature, including during rotation.
Starting conditions: `contracts/webhooks/signature-vectors.json`.
Role: tenant receiver implementation.
Steps: 1. Run each vector through the Go signer and each guide snippet.
Expected outcomes: VALID for basic and rotation-overlap; REJECT_TIMESTAMP for stale; REJECT_SIGNATURE for tampered and whitespace-changed bodies.

**TS-07 — Fair delivery with a dead endpoint** (TLY-305, TLY-306)
Objective: one tenant's broken endpoint does not delay others.
Starting conditions: tenant Quantis with an endpoint returning 503 and 100k backlog; tenants A, Bluefin, Meridian healthy.
Role: platform.
Steps: 1. Measure baseline p95 time-to-first-attempt for healthy tenants. 2. Start Quantis backlog. 3. Measure again for 30 min.
Expected outcomes: healthy tenants' p95 within 10 % of baseline; Quantis circuit opens after 10 failures; no messages lost.

**TS-08 — SSRF protection** (TLY-303, TLY-305, TLY-904)
Objective: webhook URLs cannot reach internal networks.
Starting conditions: resolver fixtures for private, link-local, IPv6 ULA, decimal-encoded IPs, redirect target, and a rebinding host.
Role: tenant developer.
Steps: 1. Register each URL → 400 at registration where resolvable. 2. For the rebinding host (public at registration, private at dial) trigger a delivery.
Expected outcomes: registration rejects static cases; the dialer blocks the rebinding case with `SSRF_BLOCKED` and disables the endpoint.

**TS-09 — Usage counted exactly once** (TLY-601, TLY-602, TLY-603)
Objective: duplicates and restarts never change totals.
Starting conditions: meter `api_calls` (SUM); 100k events with 5 % duplicate ids, 2 % late within lateness.
Role: tenant backend.
Steps: 1. Ingest in batches of 1,000. 2. `kill -9` the aggregator midway; restart. 3. Replay `tally.usage.rollups.v1` into the core from offset 0.
Expected outcomes: `duplicates` counts match; rollup total = count of unique events; core totals unchanged after replay.

**TS-10 — Subscription preview equals first invoice** (TLY-502, TLY-504)
Objective: no surprise charges.
Starting conditions: graduated price and seat price; random item sets.
Role: tenant admin.
Steps: 1. Preview. 2. Create with the same items. 3. Run billing for the first period.
Expected outcomes: invoice total equals preview `due_today` / `recurring_total` for every generated case.

**TS-11 — API key revocation within 5 s** (TLY-105)
Objective: revoked keys stop working quickly everywhere.
Starting conditions: two core instances behind the local load balancer.
Role: tenant admin + tenant backend.
Steps: 1. Call API with the key on both instances → 200. 2. Revoke. 3. Poll both instances every 250 ms.
Expected outcomes: both return 401 within 5 s; no 200 after 5 s.

**TS-12 — Four-eyes suspend** (TLY-802, TLY-804)
Objective: no single operator can suspend a tenant.
Starting conditions: operators Mai (SUPPORT) and Khoa (PLATFORM_ADMIN) with WebAuthn.
Role: operators.
Steps: 1. Mai requests suspend → 202 approval. 2. Mai tries to approve → 403. 3. Khoa approves without fresh step-up → 401 STEP_UP_REQUIRED. 4. Khoa steps up and approves.
Expected outcomes: tenant SUSPENDED; tenant writes 423, reads 200; deliveries HELD; audit entries for request, approval and execution with both ids.

**TS-13 — Audit tamper evidence** (TLY-801)
Objective: tampering with the operator audit log is detected.
Starting conditions: 1,000 audit entries.
Role: platform.
Steps: 1. Run verification → ok. 2. As superuser in the test, modify entry 500. 3. Run verification.
Expected outcomes: `ok=false`, `first_broken_seq=500`; alert fires.

**TS-14 — Impersonation is visible and bounded** (TLY-806, TLY-807, TLY-815)
Objective: support access is transparent and read-only by default.
Starting conditions: operator Mai, tenant Acme.
Role: operator + tenant admin.
Steps: 1. Start 15-min READ_ONLY session with reason. 2. Load tenant Home → banner shows Mai, READ_ONLY, countdown. 3. Try to refund → 403. 4. Tenant admin opens Settings → session listed. 5. Advance clock 15 min → token rejected.
Expected outcomes: all steps as described; audit entries carry actor = Mai.

**TS-15 — Accessibility and reduced motion** (TLY-909)
Objective: every screen usable by keyboard and screen reader, with reduced motion honored.
Starting conditions: `prefers-reduced-motion: reduce`, keyboard only.
Role: tenant admin / operator.
Steps: 1. Complete J4 with keyboard only. 2. Run axe on all 30 screens. 3. Capture reduced-motion snapshots.
Expected outcomes: journey completes; 0 axe violations; no transform animations in snapshots; statuses have text labels.

**TS-16 — Tenant switch clears state** (TLY-104)
Objective: no data from tenant A is visible after switching to B.
Starting conditions: user with memberships in Acme and Bluefin.
Role: tenant admin.
Steps: 1. Open Customers in Acme. 2. Switch to Bluefin. 3. Inspect query cache and render.
Expected outcomes: cache empty before first Bluefin render; no Acme names flash on screen (video frame check).

## 7. Chaos and resilience suite

| Scenario | Injection | Invariant |
| --- | --- | --- |
| C1 Broker down during writes | stop Kafka 2 min under 200 rps | API writes succeed; outbox drains; 0 lost events |
| C2 Relay crash after produce | kill relay between produce and mark | duplicates deduped; 0 duplicate effects |
| C3 Dispatcher crash mid-delivery | kill -9 with 10k in flight | every (event, endpoint) delivered ≥ 1, dedup at receiver shows 0 missing |
| C4 Redis loss | flush Redis during delivery | scheduler rebuilds from Postgres; no loss |
| C5 Processor latency | Toxiproxy +6 s on 30 % | payments PROCESSING then resolved; ledger = processor |
| C6 Postgres failover | restart primary | API recovers < 60 s; no partial ledger entries |
| C7 Aggregator restart | kill -9 during windows | totals unchanged |
| C8 Endpoint flapping | alternate 200/503 each minute | circuit opens/closes; no flood; ≤ 8 in flight |
