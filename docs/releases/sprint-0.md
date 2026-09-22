# Sprint 0 — Walking Skeleton

Version 1.0 · 2026-09-22 · Owner: Tech lead

Sprint 0 goal (docs/07-delivery-plan.md §3): stack up, contracts pipeline, five skeletons. Not tagged
as a release (docs/07 §7: R0 has no `vX.Y.0`) — this is the sprint-review record docs/07 §9 calls for,
kept as a doc instead of only a demo clip since the walking skeleton is what every later sprint builds on.

All seven Sprint 0 stories are merged to `main`. No flags were introduced (Sprint 0 ships no
user-visible slice — see docs/07 §2, "Stories outside slices").

## What shipped

| Story | Lane | What it does |
| --- | --- | --- |
| [TLY-001](../plans/TLY-001.md) | PLAT | Monorepo scaffold: `.tool-versions`, minimal buildable skeletons in all four lanes, GitHub Actions CI (path-filtered per lane, `pr-title` check, gitleaks), `make docs-check`. |
| [TLY-002](../plans/TLY-002.md) | PLAT | `deploy/compose.yaml`: Postgres 18, Kafka 4.1 (KRaft), Redis 8, Keycloak 26 (`tally-tenants`/`tally-operators` realms), OTel collector, Prometheus, Tempo, Loki, Grafana. `make up`/`down`/`seed`. |
| [TLY-003](../plans/TLY-003.md) | PLAT | Contract pipeline: Spectral lint, oasdiff breaking-change gate (`BREAKING_APPROVED=1` escape hatch), JSON Schema compile, webhook vector self-check — all in `make contracts`. Codegen for all four lanes in `make gen`: Spring interfaces (core), Go types + guarded sqlc (workers), typed clients + MSW mocks (web, ops-web). |
| [TLY-004](../plans/TLY-004.md) | CORE | Java 25 / Spring Boot 4.1.1 / Spring Modulith skeleton: 11 module packages, 3 ArchUnit rules (`Money` never `double`/`float`, domain has no framework imports, no field injection), Flyway baseline, ports 8080 (tenant) / 8081 (ops) / 8082 (actuator), structured JSON logging with W3C traceparent, OpenAPI request+response validation active in tests. |
| [TLY-005](../plans/TLY-005.md) | WORK | Go 1.27 dispatcher/aggregator skeleton: typed fail-fast env config, OTel + slog telemetry, graceful shutdown (goleak-verified), depguard rule proving `internal/*/domain` can't import pgx/franz-go. |
| [TLY-006](../plans/TLY-006.md) | WEB | Tenant app shell (Next.js 16 / React 19): sidebar + topbar, design tokens as the single source of truth (raw-value lint ban), MSW mock mode + BFF passthrough proxy, Lighthouse perf ≥0.90 / a11y 1.0 gate. |
| [TLY-007](../plans/TLY-007.md) | OPSW | Operator console shell (Next.js 16 / React 19): 14-item dark-dense sidebar, non-dismissible environment badge + session timer on every page (including 404s), nonce-based strict CSP, mock mode from the 42 generated ops handlers. |

## How to test

### Prerequisites

Docker, JDK 25, Go 1.27, Node LTS + pnpm, Python 3.12 (contract generators). `golangci-lint` is
optional (`make lint` skips the `workers/` check with a message if it's absent; CI always runs it).

### One-shot sweep (what every PR must pass)

```bash
pnpm install --frozen-lockfile     # web/ + ops-web/ deps
make lint test docs-check
```

Expected: all green. `make lint` runs Gradle Spotless+ArchUnit, `golangci-lint`, ESLint+`tsc` for
both frontends. `make test` runs Gradle `test integrationTest` (Testcontainers), `go test -race`
+ `go test -tags=integration`, and `vitest run` for both frontends. `make docs-check` runs
`scripts/validate_pack.py` (expect `81 stories, 32 ADR files, 0 errors, 0 warnings`).

### Contract pipeline (TLY-003)

```bash
make contracts   # generators → Spectral → oasdiff → contracts/ diff gate
make gen         # codegen for all four lanes
```

To see the breaking-change gate actually fire: edit a required response field in
`scripts/gen_openapi.py`, run `make contracts` (expect a `response-required-property-removed`
error and a non-zero exit), then either revert or re-run with `BREAKING_APPROVED=1 make contracts`
to prove the escape hatch. The exact worked example (a `Me.memberships` rename that also breaks
`core`'s build) is captured verbatim in `docs/plans/TLY-003.md` under "AC3 demonstration".

### Local stack (TLY-002)

```bash
make up             # cold boot ≤120s, health + memory report printed at the end
make seed           # loads contracts/fixtures/tenants.json into Keycloak
make down ARGS=-v   # full teardown including volumes (docs/06 AC5 is written as `make down -v`,
                     # which GNU Make can't parse as written — use ARGS=-v; a doc-fix is open)
```

Manual check: `curl -s localhost:8180/realms/tally-tenants` and `.../tally-operators` should each
return a realm document. `deploy/stack-report.sh` (run automatically by `make up`) prints per-container
health and total memory against the 16 GB budget.

### Core skeleton (TLY-004)

```bash
cd core && ./gradlew bootRun --args='--spring.profiles.active=local'
curl -s localhost:8082/actuator/health   # {"status":"UP"} once Postgres/Kafka are up (make up first)
curl -s localhost:8080/v1/me             # tenant port
curl -s localhost:8081/v1/me             # ops port — expect 404, /v1/me is not an ops route
```

To see the money-safety gate fire: add a `double amount` field anywhere under
`com.tally.core.kernel.money`, run `./gradlew archTest` — expect
`Rule 'noFloatingPointMoney' was violated`, then revert.

### Workers skeleton (TLY-005)

```bash
cd workers
env -u DATABASE_URL -u REDIS_URL -u KAFKA_BROKERS go run ./cmd/dispatcher   # fails fast, lists missing vars
KAFKA_BROKERS=localhost:9092 DATABASE_URL=... REDIS_URL=... go run ./cmd/dispatcher &
kill -TERM %1   # should exit cleanly well under the 30s shutdown budget
```

Depguard proof: `golangci-lint run --build-tags=depguardfixture ./internal/example/domain/...`
should report exactly one `depguard` violation on the kept fixture file.

### Web / ops-web shells (TLY-006, TLY-007)

```bash
cd web && pnpm dev       # localhost:3000 — sidebar, topbar, test-mode banner, mock data
cd ops-web && pnpm dev   # localhost:3100 — dark console, environment badge, session timer
```

Both apps default to `mock` mode (MSW-served data from the generated contract handlers) unless
`NEXT_PUBLIC_API_MODE=live` is set. E2E/CSP checks:

```bash
make e2e   # Playwright: web's reduced-motion + token-resolution checks, ops-web's CSP + hydration checks
```

`ops-web` is the one worth trying by hand: visit an unknown section (e.g. `/nonsense`) and confirm
the environment badge and sidebar are still there on the 404 — that's the bug TLY-007's own review
caught (`docs/plans/TLY-007.md` rulings R8/R11).

## Known follow-ups (not blocking, tracked here so they aren't lost)

- **docs/06 AC5 wording** (TLY-002) — `make down -v` as literally written can't work; the Makefile
  target is `make down ARGS=-v`. Needs a one-line doc-fix PR.
- **CI must install `golangci-lint`** — `workers/internal/lint`'s depguard proof (TLY-005 AC4) skips
  itself when the binary is absent from `PATH`; confirm `.github/workflows/ci.yml`'s `work` job has
  it (it does, via `golangci-lint-action`) so this AC is actually exercised in CI, not just locally.
- **AC2's "sqlc models" (TLY-003 ruling 4)** — satisfied today as pipeline wiring only; no dispatcher
  migrations exist yet (ADR-016: that schema belongs to the webhook-delivery stories). The guard in
  `workers/internal/store/generate.go` skips with a message naming the owning story.
- **Jackson `non_null` global inclusion (TLY-004 ruling R13)** — see the follow-up notes now on
  TLY-303 and TLY-401 in `docs/06-user-stories.md`: their handlers must always populate the two
  required-and-nullable fields (`WebhookSecret.overlap_until`, `PriceTier.up_to`) explicitly.
- **PR-size exceptions** — TLY-002 (678 lines), TLY-006 (~1548 lines) and TLY-007 (~1400 lines) all
  exceeded the ≤400-line PR budget (root `CLAUDE.md` §6.14/docs/07 §8). Each is a from-scratch
  scaffold where splitting would leave an untestable partial state; recorded here as a knowing
  exception, not a silently ignored rule.

## What's next

Sprint 1 (docs/07-delivery-plan.md §3.1): tenant provisioning + RLS (TLY-101/102), identity
(TLY-103), BFF login (TLY-104), the `Money` value object landing in the ledger module (TLY-201),
and webhook signing vectors (TLY-312) — the first stories that depend on this skeleton rather than
building it.
