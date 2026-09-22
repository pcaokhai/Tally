# Risk Register (Pre-mortem)

Version 1.0 · 2026-09-22 · Owner: Tech lead

It is the end of Sprint 15 and Tally failed: the demo breaks, the numbers do not add up, or the owner cannot explain the code. What happened?

## Tigers (real risks)

| ID | Risk | Urgency | Mitigation | Owner | Check by |
| --- | --- | --- | --- | --- | --- |
| R-01 | Review bottleneck: five AI lanes produce more code than one person can review well; defects and "unexplainable" code slip in | Launch-blocking | Commit ≤ 30 pts; PRs ≤ 400 lines; daily review block; critical path first; requesting-code-review before human review | Tech lead | Every sprint (review age < 1 day) |
| R-02 | Owner cannot explain code the agents wrote (interview goal fails) | Launch-blocking | Plans kept in docs/plans; each sprint the owner rewrites one core piece from memory (ledger, scheduler, RLS filter) and records a 5-minute explanation | Tech lead | End of Sprints 2, 4, 7, 10 |
| R-03 | RLS or tenant-context bug leaks data silently | Launch-blocking | Fail-closed `current_tenant()`; `RlsCoverageIT`; generated IDOR suite from Sprint 1 (basic) and Sprint 14 (full) | CORE | Sprint 1, 14 |
| R-04 | Contract drift between mocked frontends and real backends | Launch-blocking | Contracts merged day 1; provider validation filter; integration checkpoint per slice before flag on | PLAT | Each slice |
| R-05 | Ledger drift under failure (retries, timeouts, partial writes) | Launch-blocking | Deferred balance trigger; property tests; unknown-outcome rule; chaos C2/C5/C6; daily reconciliation | CORE | Sprints 2, 9, 14 |
| R-06 | Fairness claim false (dispatcher starves under a dead endpoint) | Fast-follow | Fairness AC inside TLY-305; chaos C8; load suite noisy neighbor | WORK | Sprint 4, 14 |
| R-07 | Laptop cannot run the full stack or load tests (NFR-COST-01) | Fast-follow | Memory budget AC in TLY-002; load runs on k3d profile with reduced replicas; publish hardware in results | PLAT | Sprint 0, 13 |
| R-08 | Scope creep from the 30 designed screens and ops console | Fast-follow | Out-of-scope list in docs/01 §8; new ideas go to backlog only; Sprint 15 is buffer; silo migration may be cut | Tech lead | Every planning |
| R-09 | Flaky integration/E2E tests erode trust in CI | Fast-follow | No sleeps; Testcontainers reuse; quarantine label with 48 h fix SLA; flake rate metric | PLAT | Every sprint |
| R-10 | Java 25 / Spring Boot 4 / Go 1.27 library gaps (Jackson 3, Modulith, codegen) | Track | Pin versions in TLY-001/004; spike on day 1 of Sprint 0; fallback: generator options or thin hand-written adapters | CORE | Sprint 0 |
| R-11 | Sensitive data leaks into logs, fixtures or screenshots | Launch-blocking | Log scan in CI; fixtures `.example` only; masking utilities; PR checklist | PLAT | Sprint 14 (full scan), every PR |
| R-12 | Operator console becomes a backdoor | Launch-blocking | Separate realm/port/filter chain; four-eyes; reason header; hash-chained audit; no RLS bypass (ADR-022/023/025/026) | CORE | Sprint 10–11 |

## Paper tigers

| Concern | Why it is not a real risk here |
| --- | --- |
| "A modular monolith will not scale" | Load targets fit one instance; module boundaries are enforced, so extraction stays possible (ADR-001) |
| "Kafka is overkill locally" | KRaft single node fits the memory budget and is the point of the learning project |
| "Polling outbox relay is too slow" | 100 ms poll with batches meets p99 < 1 s at target load; CDC is a documented upgrade path (ADR-008) |
| "JSON Schema instead of Protobuf loses type safety" | Codegen for Java and Go from JSON Schema plus CI compatibility checks gives the needed safety (ADR-009) |
| "Next.js BFF adds latency" | One hop inside the same host; server components remove client waterfalls |

## Elephants

| Concern | Investigation |
| --- | --- |
| Is 30 points/sprint realistic alongside a full-time job? | Measure Sprints 0–2 actual velocity; re-plan calendar (not order) if below 20 |
| Will Keycloak token exchange + organizations behave as designed? | Spike in Sprint 1 inside TLY-103; fallback: BFF-minted short-lived JWT signed by core (ADR-007 alternative) |
| Does the design canvas match what users actually need? | Moderated test of J4 with 3 non-engineers at R2 |
| Is silo migration worth its complexity for a learning project? | Decide at Sprint 13 review; if cut, write the design-only ADR-021 addendum |

## Action plans for launch-blocking tigers

| Risk | Action | Owner | Due |
| --- | --- | --- | --- |
| R-01 | Add review-age and PR-size dashboards; enforce 400-line CI warning | Tech lead | Sprint 0 |
| R-02 | Create `docs/learning-log.md`; first entry after TLY-202 | Tech lead | Sprint 2 |
| R-03 | TLY-102 AC2–AC5 + basic IDOR test for the first two endpoints | CORE | Sprint 1 |
| R-04 | Provider validation filter in TLY-004; integration checkpoint template in slice issues | PLAT | Sprint 0 |
| R-05 | Property tests in TLY-202; unknown-outcome AC in TLY-701/702 | CORE | Sprints 2, 9 |
| R-11 | gitleaks + log scan job in TLY-001; masking helpers in TLY-004 | PLAT | Sprint 0 |
| R-12 | TLY-801 scheduled before any operator screen; RBAC matrix test | CORE | Sprint 10 |
