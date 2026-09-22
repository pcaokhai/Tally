# User Stories

Version 1.0 · 2026-09-22 · Owner: Tech lead

Stories follow the 3 C's (card, conversation, confirmation) and INVEST. Acceptance criteria are numbered; tests reference them as `TLY-nnn-ACn`. Stories are ≤ 8 points. Frontend stories depend on the contract (merged day 1), never on backend stories. Generated from `scripts/stories.py`; edit there and run `python scripts/gen_stories_doc.py`.

Design canvas: https://claude.ai/artifact/2qhb5nQT1s9m9SCuUoqHJu (artboard names are given per UI story).

Legend: **Lane** PLAT · CORE · WORK · WEB · OPSW (docs/07 §1) · **pts** story points · **Slice** feature slice or — (docs/07 §2) · **Depends** stories that must be merged first · **Traces** requirement ids (docs/02 §2) and ADRs.

Roles: tenant admin (OWNER/ADMIN), tenant developer, finance user, viewer, operator (SUPPORT, FINANCE, SRE, PLATFORM_ADMIN), the platform (system behavior).

## E0 — Platform, contracts and skeletons (Sprint 0)

### TLY-001 Monorepo scaffold, Makefile and CI skeleton
Lane PLAT · 5 pts · Slice — · Depends: — · Traces: NFR-MNT-01, NFR-MNT-04

As the tech lead, I want one repository with lane directories, a Makefile and CI, so that every lane builds and tests the same way from day one.

1. `make lint test` runs every lane's (empty) checks and exits 0 on a clean clone with only Docker, JDK 25, Go 1.27 and pnpm installed.
2. GitHub Actions runs lint, test, contracts and gitleaks jobs per changed lane (path filters) and posts results on the PR.
3. `.github/CODEOWNERS` and the PR template from docs/07 §8 are present; a PR without a story id in the title fails the `pr-title` check.
4. Toolchain versions (JDK 25, Go 1.27, Node LTS, pnpm) are pinned in one file (`.tool-versions`) and CI reads them from there.
5. `make docs-check` runs `scripts/validate_pack.py` and fails CI on any error.

### TLY-002 Local stack with docker compose
Lane PLAT · 5 pts · Slice — · Depends: TLY-001 · Traces: NFR-COST-01, NFR-OBS-01

As a developer, I want `make up` to start Postgres, Kafka, Redis, Keycloak and the observability stack, so that every lane integrates locally.

1. `make up` starts Postgres (databases `tally`, `dispatcher`, `keycloak`), Kafka KRaft, Redis, Keycloak with realms `tally-tenants` and `tally-operators`, OTel collector, Prometheus, Tempo, Loki and Grafana; all health checks green within 120 s.
2. Total resident memory of the stack is ≤ 16 GB, reported by `make up` (A-01).
3. Kafka topics from docs/04 §7 are created with the documented partitions, retention and compaction.
4. `make seed` loads `contracts/fixtures/tenants.json` into Keycloak and the core API once the core is running.
5. `make down -v` removes all containers and volumes; `make up` afterwards starts from a clean state.

### TLY-003 Contract pipeline and code generation
Lane PLAT · 5 pts · Slice — · Depends: TLY-001 · Traces: NFR-MNT-04, ADR-009

As the tech lead, I want contracts linted, diffed and turned into code for every lane, so that contract drift fails CI instead of production.

1. `make contracts` runs the three generators, Spectral lint (0 errors), oasdiff against `main` (breaking change fails unless the PR has label `breaking-approved`), JSON Schema compile and the webhook vector self-check.
2. `make gen` produces Spring interfaces for core, Go types + sqlc models for workers, and openapi-typescript clients + MSW handlers for web and ops-web; generated folders are marked read-only in CODEOWNERS.
3. Changing a response field in `scripts/gen_openapi.py` and running `make gen` produces a compile error in core until the handler is updated (demonstrated in the PR).
4. CI fails if `make contracts` leaves a diff in `contracts/` (generated files must be committed).

### TLY-004 Core skeleton: Java 25, Spring Boot 4, Spring Modulith
Lane CORE · 5 pts · Slice — · Depends: TLY-001, TLY-003 · Traces: NFR-MNT-01, NFR-MNT-03, NFR-COR-04

As the CORE lane, I want a modular monolith skeleton with enforced boundaries, so that modules stay independent as they grow.

1. Gradle build with Java 25 toolchain, Spring Boot 4.x, Spring Modulith and the eleven module packages from core/CLAUDE.md; exact versions pinned in `gradle/libs.versions.toml`.
2. `./gradlew archTest` runs Spring Modulith `verify()` and ArchUnit rules (domain has no framework imports; no `double`/`float`/`BigDecimal` in `..money..` and `..ledger..`; no field injection); a deliberate violation in a test fixture fails the build.
3. Flyway applies `V001__baseline_roles_schemas.sql`; `PostgresKafkaIT` base class starts Testcontainers and a sample repository test passes.
4. Ports 8080 (`/v1`), 8081 (`/ops/v1`) and 8082 (actuator) serve `GET /v1/health` style probes; virtual threads enabled; JSON logs include `service` and `trace_id`.
5. The OpenAPI request/response validation filter is active in tests and rejects a response that violates `contracts/openapi.yaml`.

### TLY-005 Workers skeleton: Go 1.27 dispatcher and aggregator
Lane WORK · 3 pts · Slice — · Depends: TLY-001, TLY-003 · Traces: NFR-MNT-01

As the WORK lane, I want two runnable Go binaries with config, telemetry and graceful shutdown, so that delivery and aggregation stories start from a sound base.

1. `go build ./...` builds `cmd/dispatcher` and `cmd/aggregator` with `go 1.27` in go.mod; golangci-lint config from workers/CLAUDE.md passes.
2. Both binaries load typed env config and exit non-zero with a clear message when a required variable is missing.
3. SIGTERM stops intake and exits within 30 s; a `goleak` test proves no goroutine outlives shutdown.
4. depguard forbids `internal/*/domain`-style packages from importing pgx, franz-go or net/http; a violating import fails lint.

### TLY-006 Web shell, design tokens and mock mode
Lane WEB · 5 pts · Slice — · Depends: TLY-001, TLY-003 · Traces: NFR-FE-01, NFR-FE-02, NFR-FE-05

As a tenant user, I want a consistent app shell, so that every screen feels like one product.
Design: canvas artboard(s) `Main / Home (shell)`.

1. The shell renders the full sidebar (Home, Customers, Subscriptions, Invoices, Payments, Usage, Products, Developers group, Settings) on every route, with the active item and topbar (search, test-mode toggle, bell, avatar) matching the canvas.
2. Design tokens (colors, type scale with Geist + Instrument Serif, spacing, radii) and motion tokens from docs/02 §7.8 live in one module; lint forbids raw hex colors and raw durations in components.
3. `NEXT_PUBLIC_API_MODE=mock` serves every route from generated MSW handlers; `live` goes through the BFF.
4. With `prefers-reduced-motion: reduce`, no transform animations run (Playwright check).
5. Lighthouse on the empty Home shell: performance ≥ 90, accessibility 100.

### TLY-007 Operator console shell
Lane OPSW · 3 pts · Slice — · Depends: TLY-001, TLY-003 · Traces: NFR-ADM-UX-02, NFR-ADM-SEC-09

As an operator, I want a distinct dark, dense console shell, so that I never confuse it with the tenant app.
Design: canvas artboard(s) `Admin Overview (shell)`.

1. Separate Next.js app on port 3100 with the dark compact theme and the 14-item ops sidebar from the canvas.
2. A non-dismissible environment badge (PRODUCTION red / STAGING amber) and a session timer render on every page.
3. Strict CSP (no third-party scripts, `frame-ancestors 'none'`) is served and verified by a Playwright header check.
4. Mock mode works from `contracts/ops-openapi.yaml` MSW handlers.

## E1 — Tenancy, identity and access (Sprint 1–4)

### TLY-101 Tenant provisioning and plan versions
Lane CORE · 5 pts · Slice S1 · Depends: TLY-004 · Traces: FR-TEN-01, FR-TEN-02, ADR-028

As an operator, I want to create a tenant with a plan version and isolation tier, so that a new business can start using Tally.

1. `POST /ops/v1/tenants` with an Idempotency-Key creates the tenant, owner membership invitation, invoice number sequence and default ledger accounts in one transaction; a repeat with the same key returns the same tenant.
2. The tenant gets `isolation_tier = POOL` unless the plan version's limits specify SILO; the value is returned by the API.
3. Plan versions are seeded (`starter v1`, `growth v1`, `scale v1`) and immutable; a tenant references exactly one version.
4. A `tenant.created` audit entry records the operator as actor.
5. Creating a tenant with an unknown `plan_code` returns 400 `VALIDATION_FAILED` with `param = plan_code`.

### TLY-102 Tenant context and row-level security
Lane CORE · 8 pts · Slice — · Depends: TLY-004 · Traces: NFR-SEC-01, NFR-SEC-02, ADR-002, ADR-003, ADR-027

As the platform, I want every query scoped to the tenant from the credential, so that no tenant can ever read or write another tenant's data.

1. `TenantFilter` resolves the tenant from the JWT `tenant_id` claim or the API key and binds a `ScopedValue<TenantContext>`; any `tenant_id` in headers, query or body is ignored.
2. Every transaction runs `SET LOCAL app.tenant_id` before the first statement; `RlsCoverageIT` asserts that every table with a `tenant_id` column has RLS enabled and forced.
3. With the context unset, a repository query returns zero rows (fails closed) and logs WARN `tenant_context_missing`.
4. A composite-FK test shows that inserting a subscription for another tenant's customer id fails with a FK violation even if RLS were disabled.
5. An ArchUnit rule fails the build if a class in `..adapter.out..` executes SQL outside a transaction for a tenant table.

### TLY-103 Identity: resource server and /v1/me
Lane CORE · 5 pts · Slice S1 · Depends: TLY-004 · Traces: FR-TEN-05, NFR-SEC-02

As a tenant user, I want my identity and memberships resolved from my token, so that the dashboard knows who I am and which tenant is active.

1. Core validates JWTs from realm `tally-tenants` (issuer, audience, signature, expiry); invalid tokens get 401 `UNAUTHENTICATED`.
2. `GET /v1/me` returns the user, the active tenant (from the `tenant_id` claim) with role, and all memberships.
3. A token whose `tenant_id` is not among the user's active memberships is rejected with 401.
4. Role-based checks use a single `Permission` matrix (docs/02 §7.1) shared by controllers; a VIEWER calling a write endpoint gets 403 `FORBIDDEN_SCOPE`.

### TLY-104 BFF login, session and tenant switcher
Lane WEB · 5 pts · Slice S1 · Depends: TLY-006 · Traces: FR-TEN-05, NFR-FE-04

As a tenant user, I want to sign in once and switch between my tenants, so that I can work across businesses safely.
Design: canvas artboard(s) `Main / Home (topbar)`.

1. `/auth/login` → Keycloak → `/auth/callback` sets an httpOnly, Secure, SameSite=Lax encrypted session cookie; no token is readable from browser JavaScript.
2. The tenant switcher lists memberships from `GET /v1/me`; choosing one calls `/auth/switch-tenant`, which performs token exchange and reloads into the new tenant.
3. On switch, all TanStack Query caches and client stores are cleared before the first render of the new tenant (unit test asserts empty cache).
4. Session idle timeout 30 min, absolute 12 h; expiry redirects to login preserving the target URL.
5. Works in mock mode with two fixture tenants.

### TLY-105 API keys: create once, hash, scope, revoke
Lane CORE · 5 pts · Slice S2 · Depends: TLY-102, TLY-110 · Traces: FR-TEN-04, FR-KEY-01, FR-KEY-02, NFR-SEC-04

As a tenant developer, I want scoped API keys, so that my backend can call Tally with least privilege.

1. `POST /v1/api_keys` returns the secret exactly once; only prefix, last4 and an Argon2id hash are stored; a log scan test finds no secret in logs.
2. Requests with a key resolve tenant, mode (LIVE/TEST) and scopes; a missing scope returns 403 `FORBIDDEN_SCOPE`.
3. `POST /v1/api_keys/{id}/revoke` makes the key fail on every core instance within 5 s (cache TTL 5 s, integration test with two app instances).
4. `last_used_at` updates at most once per minute per key (no write per request).
5. A TEST key cannot read or write LIVE-mode objects and vice versa.

### TLY-106 API keys screen
Lane WEB · 3 pts · Slice S2 · Depends: TLY-006 · Traces: FR-KEY-01

As a tenant developer, I want to manage keys in the dashboard, so that I can rotate them without support.
Design: canvas artboard(s) `ApiKeys`.

1. The list shows name, mode, prefix•••last4, scopes, created and last used; secrets never appear in the list.
2. Create opens the modal; after success the secret is shown once with copy and a required 'I have stored it' confirmation before close.
3. Revoke asks for confirmation naming the key and shows the row as revoked without a page reload.
4. Empty, loading and error states match the canvas; keyboard-only flow passes axe.

### TLY-107 Team members and invitations
Lane CORE · 5 pts · Slice S3 · Depends: TLY-103 · Traces: FR-SET-01

As a tenant admin, I want to invite teammates with roles, so that the right people can work in Tally.

1. `POST /v1/team/invitations` creates an invitation that expires in 7 days and emails a one-time link (email sender port, fake in tests).
2. Accepting creates the membership with the invited role; an expired or reused link returns 410.
3. `PATCH /v1/team/members/{userId}` changes roles; the last OWNER cannot be demoted or removed (409 `INVALID_STATE`).
4. Only OWNER/ADMIN can manage the team; others get 403.
5. Each change writes an audit entry with actor and target.

### TLY-108 Settings screen: team, branding, plan and limits
Lane WEB · 5 pts · Slice S3 · Depends: TLY-006 · Traces: FR-SET-01, FR-SET-02, FR-SET-03

As a tenant admin, I want one settings screen for team, branding and plan, so that I can manage my account in minutes.
Design: canvas artboard(s) `Settings`.

1. Team tab lists members and pending invitations with role selects and invite form; role changes are optimistic with rollback on error.
2. Branding tab previews the accent color and logo live on a sample invoice before saving; invalid hex is rejected inline.
3. Plan tab shows plan, version and each limit with a usage bar; bars ≥ 80 % are amber, ≥ 100 % red with label (not color only).
4. Saving branding sends `If-Match`; a 412 shows 'Someone else changed this' with a reload action.

### TLY-109 Branding and account plan endpoints
Lane CORE · 3 pts · Slice S3 · Depends: TLY-102 · Traces: FR-SET-02, FR-SET-03

As a tenant admin, I want to set branding and see my plan limits, so that invoices look like my business and I know my headroom.

1. `GET/PATCH /v1/settings/branding` with ETag/If-Match; accent must match `^#[0-9A-Fa-f]{6}$`.
2. `POST /v1/settings/branding/logo` accepts SVG or PNG ≤ 512 KB; SVGs are sanitized (no scripts, no external refs).
3. `GET /v1/account/plan` returns limits from the plan version and current usage for each (customers, API rpm, webhook endpoints, usage events/month).
4. Invoice PDF rendering reads branding (verified in TLY-505).

### TLY-110 Idempotency filter and tenant audit log
Lane CORE · 5 pts · Slice — · Depends: TLY-102 · Traces: NFR-COR-03, FR-OPS-02, ADR-006

As a tenant developer, I want retries to be safe, so that a network error never creates a duplicate object or charge.

1. For endpoints marked idempotent in the spec, a missing `Idempotency-Key` returns 400.
2. Same key + same request hash returns the stored status and body without re-executing; the response carries `Idempotent-Replayed: true`.
3. Same key + different hash returns 422 `IDEMPOTENCY_KEY_REUSED`; a concurrent duplicate while the first is running returns 409 `IDEMPOTENCY_IN_PROGRESS`.
4. Keys expire after 24 h (cleanup job); keys are scoped per tenant.
5. Every state-changing use case writes `kernel.audit_log` (actor type, actor id, on_behalf_of, action, target) in the same transaction.

### TLY-111 Per-tenant rate limiting
Lane CORE · 5 pts · Slice — · Depends: TLY-102, TLY-101 · Traces: NFR-FAIR-03, NFR-FAIR-01

As the platform, I want each tenant limited to its plan's rate, so that one tenant cannot degrade the others.

1. A Redis token bucket per tenant uses `rate_limit_rpm` from the plan version, or an active `ops.rate_limit_overrides` row.
2. Exceeding the limit returns 429 `RATE_LIMITED` with `Retry-After` and `RateLimit-*` headers; other tenants' requests are unaffected (integration test with two tenants).
3. If Redis is unavailable the limiter fails open for ≤ 60 s with a WARN log and metric, then fails closed at 2× the plan rate using an in-memory limiter.
4. Usage ingest has its own bucket so API reads are not starved by ingest bursts.

## E2 — Ledger and money (Sprint 1–2)

### TLY-201 Money value object
Lane CORE · 3 pts · Slice — · Depends: TLY-004 · Traces: NFR-COR-04, ADR-004

As a CORE developer, I want one Money type, so that amounts cannot be mixed, rounded or overflowed by accident.

1. `Money(long minor, Currency currency)` with `plus`, `minus`, `times(long)`, `negate`, `isZero`; arithmetic uses `Math.*Exact` and throws on overflow.
2. Operations across currencies throw `CurrencyMismatch`.
3. Jackson 3 module serializes to `{amount, currency}` (lowercase) and rejects decimals on input with 400.
4. `Money.allocate(ratios)` splits without losing a minor unit (property test: sum of parts equals the whole for 10,000 random inputs).

### TLY-202 Double-entry ledger with enforced balance
Lane CORE · 8 pts · Slice — · Depends: TLY-201, TLY-102 · Traces: FR-LED-01, FR-LED-02, FR-LED-04, NFR-COR-01, NFR-PER-04, ADR-005

As the platform, I want every money movement recorded as a balanced journal entry, so that the books are always correct.

1. `LedgerService.post(JournalEntry)` persists an entry with ≥ 2 postings; the builder rejects entries that do not sum to zero per currency.
2. The deferred database trigger rejects an unbalanced entry at commit even when inserted with raw SQL (integration test).
3. Postings and entries cannot be updated or deleted by the app role (grant test); `reverse(entryId)` writes a mirror entry with `reverses_id`.
4. Each entry stores `source_type` and `source_id` (invoice, payment, refund, credit_note).
5. jqwik property test: 10⁶ random valid postings keep every account balance equal to the sum of its postings and the trial balance at zero.
6. Throughput test: 1,000 entries/s sustained for 60 s on the local stack with 0 violations.

### TLY-203 Balances as of a point in time
Lane CORE · 3 pts · Slice — · Depends: TLY-202 · Traces: FR-LED-03

As a finance user, I want account balances as of any time, so that I can answer 'what was owed on date X'.

1. `LedgerQueries.balance(account, asOf)` returns the signed balance from postings up to `asOf` using the `postings_account` index.
2. Balances for a chart of accounts are returned in one query (no N+1, asserted by statement count).
3. p95 < 50 ms for an account with 100k postings (JMH or integration timing).
4. Results match the property-test oracle from TLY-202.

## E3 — Events, outbox and webhooks (Sprint 1–5)

### TLY-301 Transactional outbox and relay
Lane CORE · 5 pts · Slice — · Depends: TLY-102, TLY-002 · Traces: NFR-COR-02, NFR-PER-06, NFR-AVL-03, ADR-008, ADR-010

As the platform, I want events written in the same transaction as state changes and relayed to Kafka, so that no event is lost or invented.

1. `OutboxPort.publish(event)` inserts into `kernel.outbox_events` in the caller's transaction; a rolled-back transaction leaves no outbox row.
2. The relay claims batches of 500 with `FOR UPDATE SKIP LOCKED`, produces with key = tenant_id and headers `event-type`, `tenant-id`, `schema-version`, `traceparent`, then marks rows published.
3. Killing the relay between produce and mark results in re-publication of the same event ids, never loss (integration test).
4. With Kafka stopped, API writes still succeed; after restart the backlog drains and `tally_outbox_unpublished` returns to 0.
5. Relay lag p99 < 1 s at 200 writes/s (metric `tally_outbox_lag_seconds`).

### TLY-302 Event log and Events API
Lane CORE · 5 pts · Slice S5 · Depends: TLY-301 · Traces: FR-EVT-01, ADR-009

As a tenant developer, I want to browse the events Tally emitted, so that I can debug my integration.

1. Every outbox event is also stored in `events.events` (same transaction) with a 30-day retention job.
2. `GET /v1/events` filters by `type` and paginates newest first; `GET /v1/events/{id}` returns the payload and per-endpoint delivery status (from the dispatcher proxy).
3. Payloads validate against `contracts/events/*.schema.json` in a contract test for every event type produced.
4. Another tenant's event id returns 404.

### TLY-303 Webhook endpoints, secrets and SSRF validation
Lane CORE · 5 pts · Slice S4 · Depends: TLY-301, TLY-110 · Traces: FR-WH-01, FR-WH-03, FR-WH-05, FR-WH-07

As a tenant developer, I want to register webhook endpoints, so that my systems react to billing events.

1. `POST /v1/webhook_endpoints` validates HTTPS and resolves the host; URLs resolving to private, loopback, link-local or metadata ranges are rejected with 400 `VALIDATION_FAILED`.
2. The secret (256-bit) is returned once; `rotate_secret` creates a new secret and keeps the old one valid for 24 h.
3. Every create/update/delete/rotate emits `webhook_endpoint.updated|deleted` on the control topic with encrypted secrets.
4. Consuming `webhook_endpoint.auto_disabled` sets status DISABLED, creates an attention item and emails the tenant; `enable` emits an update the dispatcher uses to send a test event.
5. Plan limit on endpoint count enforced (409 with limit in detail).

### TLY-304 Dispatcher consumer and delivery messages
Lane WORK · 5 pts · Slice — · Depends: TLY-005, TLY-003 · Traces: FR-WH-04, NFR-COR-03, ADR-011

As the platform, I want every event turned into durable delivery messages before offsets are committed, so that a crash never loses a webhook.

1. The consumer maintains `endpoint_projection` from control events and creates one `delivery_messages` row per matching endpoint in one transaction.
2. The Kafka offset is committed only after that transaction commits; killing the process after persist and before commit creates no duplicate rows (unique index).
3. Events for tenants with no matching endpoint are acknowledged without rows.
4. Consumer lag is exported as `tally_dispatcher_consumer_lag`; the consumer never waits on HTTP delivery.

### TLY-305 Fair scheduler, signing and SSRF-safe delivery
Lane WORK · 8 pts · Slice — · Depends: TLY-304 · Traces: NFR-FAIR-02, NFR-PER-05, FR-WH-02, NFR-SEC-06, ADR-011, ADR-013

As a tenant, I want my webhooks delivered promptly even when another tenant's endpoint is down, so that my integration stays real-time.

1. Due messages are queued per tenant in Redis and served weighted round-robin (weight from plan); ≤ 8 in flight per endpoint and 256 total.
2. Each attempt is signed per docs/03 §4; the signer passes every vector in `contracts/webhooks/signature-vectors.json`.
3. The dialer blocks private/loopback/link-local/metadata IPs after DNS resolution, including a DNS-rebinding test (first resolve public, second private).
4. Fairness test: tenant A has a dead endpoint and 100k backlog; tenants B–E keep p95 time-to-first-attempt within 10 % of baseline (NFR-FAIR-02).
5. Healthy-endpoint p95 from commit to first attempt < 5 s end to end (NFR-PER-05).
6. Timing (dns, connect, tls, wait, download) is recorded per attempt.

### TLY-306 Retries, circuit breaker, DLQ and auto-disable
Lane WORK · 5 pts · Slice — · Depends: TLY-305 · Traces: FR-WH-04, FR-WH-05, NFR-AVL-02, ADR-012

As a tenant, I want failed webhooks retried sensibly and parked when my endpoint is broken, so that nothing is lost and my endpoint is not flooded.

1. Failures follow the schedule in docs/03 §7 with full jitter; `Retry-After` is honored up to 1 h.
2. After 8 retries or 72 h the message becomes DEAD_LETTERED with the last error.
3. 10 consecutive failures open the circuit (new messages HELD, probe every 60 s); a success closes it and drains at ≤ 20/s.
4. 50 consecutive failures (or a 410) disable the endpoint and publish `webhook_endpoint.auto_disabled`.
5. A fake-clock test walks one message through every state without sleeping.

### TLY-307 Deliveries API, replay and live stream
Lane WORK · 5 pts · Slice S4 · Depends: TLY-304 · Traces: FR-WH-06, NFR-RT-01, ADR-031

As a tenant developer, I want to inspect and replay deliveries and watch them live, so that I can fix integration issues fast.

1. Dispatcher internal API serves deliveries (filters: endpoint, status), a delivery with attempts/headers/timing, 24 hourly health buckets per endpoint, and replay; core `events` module proxies them at the `/v1` paths with tenant auth.
2. Replay creates a new message with `replay_of`, same event id, and returns 202; replay is idempotent per Idempotency-Key.
3. `GET /v1/streams/webhook_deliveries` streams `delivery.created`, `delivery.updated`, `endpoint.health` and a 15 s heartbeat; reconnect with `Last-Event-ID` replays ≤ 5 min.
4. A tenant can never see another tenant's deliveries through the proxy (proxy passes tenant id from the core context; dispatcher filters by it; test with forged ids).
5. Responses validate against `contracts/openapi.yaml`.

### TLY-308 Webhook monitor screen
Lane WEB · 8 pts · Slice S4 · Depends: TLY-006 · Traces: FR-WH-06, NFR-RT-01, NFR-FE-01

As a tenant developer, I want a live webhook monitor, so that I can see delivery health at a glance and act on failures.
Design: canvas artboard(s) `Webhooks`.

1. Endpoint cards show status, 24-hour health bars (grow on load) and success rate; the live feed prepends rows with a slide-in without shifting the scroll position.
2. Selecting a delivery opens the detail panel with request headers, response, attempt list and the timing waterfall.
3. Replay and pause/resume actions send an Idempotency-Key and show pending then result states; failures show the problem detail.
4. SSE updates are batched per animation frame; the stream reconnects with backoff and shows a 'reconnecting' pill after 3 s.
5. Reduced motion replaces slide/grow with fades; status uses color + icon + label.

### TLY-309 Endpoint management UI
Lane WEB · 3 pts · Slice S4 · Depends: TLY-006 · Traces: FR-WH-01, FR-WH-03, FR-WH-05

As a tenant developer, I want to add endpoints, rotate secrets and re-enable disabled endpoints, so that I control my integration.
Design: canvas artboard(s) `Webhooks (endpoint drawer)`.

1. Add endpoint form validates URL and event types; server SSRF errors appear inline on the URL field.
2. The signing secret is revealed once after create/rotate with copy and a 24-hour overlap note.
3. Disabled endpoints show the reason and a 'Send test & re-enable' action.
4. Delete requires typing the endpoint host.

### TLY-310 Events and logs screen
Lane WEB · 5 pts · Slice S5 · Depends: TLY-006 · Traces: FR-EVT-01, FR-EVT-02

As a tenant developer, I want to search events and API requests, so that I can trace what happened.
Design: canvas artboard(s) `Events`.

1. Events tab lists type, id, time and delivery summary; selecting one shows the JSON payload with copy and per-endpoint status.
2. Requests tab lists method, path, status, latency and key name with a status filter; 4xx/5xx rows are visually distinct with a label.
3. Filters are reflected in the URL so views can be shared.
4. Long lists are virtualized and keep 60 fps scrolling over 10,000 rows (Playwright trace).

### TLY-311 Request log pipeline and API
Lane CORE · 3 pts · Slice S5 · Depends: TLY-002, TLY-102 · Traces: FR-EVT-02, NFR-LOG-01

As a tenant developer, I want my API request log for 14 days, so that I can debug failed calls.

1. Each `/v1` request emits one structured access record (tenant, key name, method, route template, status, latency, request id) to the OTel logs pipeline; bodies are never logged.
2. `GET /v1/request_logs` queries Loki with a mandatory tenant filter added server-side; retention 14 days.
3. Records never go to the billing Postgres (NFR-LOG-01).
4. Query p95 < 500 ms for a 24 h window at 1M records locally.

### TLY-312 Webhook signing golden vectors and verification guide
Lane PLAT · 2 pts · Slice — · Depends: TLY-003 · Traces: FR-WH-02, ADR-013

As a tenant developer, I want verified examples of signature checking, so that my receiver implementation is correct the first time.

1. `scripts/gen_webhook_vectors.py` generates the five vectors in docs/03 §4 and self-verifies them in `make contracts`.
2. Verification snippets for Node, Python, Java and Go in `docs/guides/verify-webhooks.md` pass all vectors in a CI job.
3. Changing the signing rule without regenerating vectors fails CI.

## E4 — Catalog and customers (Sprint 3–6)

### TLY-401 Products and immutable price versions
Lane CORE · 5 pts · Slice S6 · Depends: TLY-102, TLY-110 · Traces: FR-CAT-01, FR-CAT-02, FR-CAT-03, FR-CAT-04

As a tenant admin, I want products with versioned prices, so that price changes never alter what existing customers pay.

1. Create products (SEAT, FLAT, METERED) and prices (PER_UNIT, FLAT, GRADUATED with tiers, MONTH/YEAR, one currency).
2. `POST /v1/products/{id}/prices` creates version n+1; the previous version becomes LEGACY; a trigger rejects UPDATE of amount, model, currency or tiers.
3. `GET /v1/products/{id}/prices` returns `subscription_count` per version.
4. Archiving a price with active subscriptions returns 409 `INVALID_STATE` with the count.
5. Graduated tiers must be ascending with exactly one open-ended last tier (400 otherwise).

### TLY-402 Products and pricing screen
Lane WEB · 5 pts · Slice S6 · Depends: TLY-006 · Traces: FR-CAT-01, FR-CAT-04

As a tenant admin, I want to see and edit my pricing safely, so that I understand who is on which version.
Design: canvas artboard(s) `Products`.

1. Product cards show kind, current price and subscriptions; the versions timeline shows CURRENT/LEGACY/ARCHIVED with counts.
2. Editing a price explains that a new version is created and shows how many subscriptions stay on the old one before confirm.
3. Graduated tier editor validates ascending boundaries inline.
4. Archive action is disabled with a tooltip when subscriptions remain.

### TLY-403 Customers API and timeline
Lane CORE · 5 pts · Slice S7 · Depends: TLY-102, TLY-110 · Traces: FR-CUS-01, FR-CUS-03

As a tenant, I want to manage my customers, so that I can bill them.

1. Create/update/get/list customers; email stored encrypted with a keyed hash; duplicate email within a tenant returns 409, across tenants is allowed.
2. `GET /v1/customers?q=` searches name and exact email (via hash); `sort=-mrr` supported; p95 < 100 ms over 50k customers.
3. `PATCH` requires If-Match and returns 412 on a stale ETag.
4. `GET /v1/customers/{id}/timeline` merges subscription, invoice and payment events newest first.
5. `customer.created|updated` events are published via the outbox.

### TLY-404 Payment method setup via processor tokens
Lane CORE · 3 pts · Slice S7 · Depends: TLY-403 · Traces: FR-CUS-02, NFR-SEC-03

As a tenant, I want customers' cards stored only by the processor, so that I stay out of PCI scope.

1. `POST /v1/customers/{id}/payment_method_setups` returns a processor setup token (simulator or Stripe test); Tally never receives card numbers.
2. Completing setup stores token, brand, last4 and expiry only.
3. Detach marks `detached_at`; a detached method cannot be charged (409).
4. A test sends a request containing a 16-digit PAN-like string to every customer endpoint and asserts 400 and no persistence.

### TLY-405 Customers list and Customer 360 screens
Lane WEB · 5 pts · Slice S7 · Depends: TLY-006 · Traces: FR-CUS-01, FR-CUS-03

As a tenant admin, I want to find a customer and see everything about them on one page, so that I can answer questions quickly.
Design: canvas artboard(s) `CustomersList, Customers`.

1. List with search, status filter, MRR sort and virtualized rows; row click opens Customer 360.
2. Customer 360 shows profile, MRR, open balance, subscriptions, invoices, payment methods and the timeline, each section with its own loading/empty/error state.
3. Editing profile uses If-Match; a 412 shows a non-destructive reload prompt.
4. Emails in the list are shown in full to tenant users (their own data) but never logged by the client.

## E5 — Subscriptions and invoicing (Sprint 6–11)

### TLY-501 Subscription lifecycle state machine
Lane CORE · 8 pts · Slice S8 · Depends: TLY-401, TLY-403 · Traces: FR-SUB-01, FR-SUB-02, FR-SUB-04, ADR-014

As a tenant, I want subscriptions that move only through legal states, so that billing behaves predictably.

1. Create with items and optional trial (0–90 days); status TRIALING or ACTIVE; period bounds computed from the price interval in the tenant time zone.
2. `SubscriptionStateMachine` allows only TRIALING→ACTIVE, ACTIVE→PAST_DUE, PAST_DUE→ACTIVE, any→CANCELED; illegal transitions return 409 (table-driven test of all 16 pairs).
3. Cancel NOW ends the subscription and creates a final prorated credit per policy; PERIOD_END sets `cancel_at_period_end`.
4. Renewal job advances periods for due subscriptions idempotently (job key = subscription + period).
5. Events `subscription.created|renewed|updated|canceled|trial_ending` are published; trial_ending fires 3 days before trial end.

### TLY-502 Pricing engine, preview and proration
Lane CORE · 5 pts · Slice S8 · Depends: TLY-401 · Traces: FR-SUB-03, FR-SUB-06

As a tenant admin, I want to preview the exact charge before creating or changing a subscription, so that there are no surprises.

1. `POST /v1/subscriptions/preview` returns lines, recurring total, due today and a plain-language explanation, with no writes (asserted by statement log).
2. Preview and the real invoice use the same `PricingEngine`; a property test shows preview total = first invoice total for random item sets.
3. Mid-cycle quantity/price changes produce proration lines by seconds remaining, rounded HALF_EVEN once per line.
4. Graduated pricing computes each tier separately (worked example in the test: 12,000 units over tiers 0–10k @1¢, >10k @0.8¢ = $116.00).
5. `PATCH /v1/subscriptions/{id}` requires If-Match and applies the same proration.

### TLY-503 Subscription builder and subscriptions list
Lane WEB · 8 pts · Slice S8 · Depends: TLY-006 · Traces: FR-SUB-01, FR-SUB-06

As a tenant admin, I want a guided builder with a live price preview, so that I can create subscriptions correctly the first time.
Design: canvas artboard(s) `SubscriptionBuilder, SubscriptionsList`.

1. Builder steps: customer → items → trial → review; step transitions animate per tokens; back keeps entered data.
2. The preview panel calls the preview endpoint (debounced 300 ms) and shows lines, due today and the explanation; stale previews are dimmed while loading.
3. Create sends one Idempotency-Key per review-screen intent; double click creates one subscription (Playwright test).
4. List supports plan, status and 'renews this week' filters with counts, and cancel (now / period end) with confirmation.
5. The flow is keyboard-complete and passes axe.

### TLY-504 Billing run and draft invoices
Lane CORE · 8 pts · Slice S9 · Depends: TLY-501, TLY-502 · Traces: FR-INV-01, FR-INV-02

As a tenant, I want invoices drafted automatically at period end, so that I bill every customer on time.

1. The billing-run job selects due subscriptions in batches with `SKIP LOCKED` and creates one DRAFT invoice per subscription period (idempotent key = subscription + period).
2. Lines come from subscription items and closed usage rollups (metered lines reference `usage_rollup_id`).
3. Drafts can be edited (lines add/remove, due date, memo) with If-Match; once not DRAFT, edits return 409 and the DB trigger blocks line changes.
4. `GET /v1/invoices/summary` returns outstanding, overdue and paid-this-month from read models.
5. Drafts auto-finalize after 1 hour unless the tenant disabled auto-finalize (setting).

### TLY-505 Finalize, void and credit notes with ledger postings
Lane CORE · 5 pts · Slice S9 · Depends: TLY-504, TLY-202 · Traces: FR-INV-03, FR-INV-04, FR-INV-05, FR-INV-06

As a tenant, I want finalized invoices numbered without gaps and reflected in the ledger, so that my books and my customers' records agree.

1. Finalize in one transaction: lock the tenant sequence, assign `<prefix>-<n>`, post Dr Accounts receivable / Cr Revenue, insert `invoice.finalized` into the outbox.
2. 50 concurrent finalizes for one tenant yield 50 distinct consecutive numbers and no gaps even when 5 of them roll back after numbering (numbers assigned last).
3. Void posts the reversing entry (`reverses_id`), sets VOID and emits `invoice.voided`; voiding a PAID invoice returns 409.
4. Credit notes reduce amount due and post Dr Contra-revenue / Cr Accounts receivable.
5. The PDF renders with tenant branding; its totals equal the ledger entry (test compares).

### TLY-506 Invoices list and invoice detail screens
Lane WEB · 8 pts · Slice S9 · Depends: TLY-006 · Traces: FR-INV-02, FR-INV-07

As a finance user, I want to review, finalize and chase invoices, so that I get paid.
Design: canvas artboard(s) `InvoicesList, InvoiceDetail`.

1. List tabs (All, Draft, Open, Overdue, Paid) with counts and summary tiles; overdue rows show days overdue with an icon and label.
2. Detail shows lines, totals, customer, history and the PDF preview; draft lines are editable inline, finalized invoices hide all edit controls.
3. Finalize plays the seal animation after the 200 response (never before) and shows the assigned number.
4. Bulk select + 'Send reminders' reports accepted and skipped with reasons.
5. Money values never animate except the seal; reduced motion shows a static stamp.

### TLY-507 Invoice reminders
Lane CORE · 3 pts · Slice S9 · Depends: TLY-504 · Traces: FR-INV-07

As a finance user, I want to send reminders in bulk, so that overdue invoices get paid.

1. `POST /v1/invoices/reminders` accepts ≤ 100 ids and returns 202 with accepted and skipped (reasons: NOT_OPEN, REMINDED_WITHIN_24H, NOT_FOUND).
2. Reminders are sent asynchronously through the email port with tenant branding; each writes `invoice_reminders`.
3. The same invoice is never reminded twice within 24 h.
4. Idempotency-Key replays return the original result.

### TLY-508 Test clocks
Lane CORE · 3 pts · Slice — · Depends: TLY-501, TLY-504 · Traces: FR-SUB-05

As a tenant developer, I want to fast-forward time in test mode, so that I can test renewals and dunning without waiting.

1. Test clocks can be created only with TEST keys; LIVE returns 403.
2. Subscriptions attached to a clock use its frozen time for all period calculations.
3. `advance` runs due renewals, billing runs and dunning steps for attached subscriptions up to the new time, in order, idempotently.
4. Advancing backwards returns 400.

## E6 — Metering and usage (Sprint 7–8)

### TLY-601 Usage ingest API
Lane CORE · 5 pts · Slice S10 · Depends: TLY-102, TLY-301 · Traces: FR-MET-01, FR-MET-02, NFR-PER-03, NFR-COR-03

As a tenant developer, I want to send usage in batches, so that metered billing reflects real consumption.

1. `POST /v1/usage_events` accepts 1–1,000 events, validates meter keys and customers, and returns 202 with accepted and duplicates counts.
2. Duplicate `event_id` (same tenant) is counted in `duplicates` and not stored twice (`usage_event_ids` PK).
3. Each accepted event produces a `usage.recorded` outbox message on `tally.usage.raw.v1`.
4. Load test: 5,000 events/s accepted with p99 < 50 ms on the local stack.
5. Events older than the closed period follow the meter's late-event policy (REJECT → 400 per item in a `rejected` list).

### TLY-602 Aggregator windowed rollups
Lane WORK · 8 pts · Slice S10 · Depends: TLY-005, TLY-003 · Traces: FR-MET-03, NFR-COR-03, ADR-015, ADR-016

As the platform, I want raw usage rolled up exactly once per window, so that invoices charge the right quantity.

1. Tumbling 5-minute windows by event time with a 2-minute allowed lateness; SUM, MAX and UNIQUE_COUNT aggregations per meter.
2. Duplicates by `event_id` within the dedup horizon are counted once; state checkpoints to the compacted topic survive restart (kill -9 test: totals unchanged).
3. Each closed window emits `usage.rollup` with a deterministic `rollup_key`; a late event re-emits the same key with the corrected quantity.
4. Watermark lag metric `tally_aggregator_watermark_lag_seconds` exported; p95 < 10 s at 5,000 ev/s.
5. Aggregator never writes the core database.

### TLY-603 Rollup ingestion, meters and usage summary
Lane CORE · 5 pts · Slice S10 · Depends: TLY-601 · Traces: FR-MET-04, FR-MET-05

As a tenant, I want current-period usage and projections per customer, so that I and my customers can anticipate charges.

1. The metering consumer upserts `usage_rollups` by `rollup_key` via `kernel.inbox` dedup; replaying the topic from the beginning changes nothing.
2. `POST/GET /v1/meters` manage meters; meter keys are unique per tenant.
3. `GET /v1/usage/summary` returns actual, projected (linear on elapsed period), included and projected overage charge.
4. Crossing a configured threshold emits `usage.threshold_crossed` once per period.
5. The period-close job sets `closed_at` so billing uses only closed rollups.

### TLY-604 Usage explorer screen
Lane WEB · 5 pts · Slice S10 · Depends: TLY-006 · Traces: FR-MET-04

As a tenant admin, I want to see usage against what is included, so that I can spot overages early.
Design: canvas artboard(s) `UsageExplorer`.

1. Meter and customer pickers; cumulative chart with actual (solid) and projected (dashed) lines and the included-quota line.
2. Summary shows actual, projected, included and projected overage in money.
3. Recent events table with virtualized rows.
4. Chart has an accessible data table alternative.

## E7 — Payments and dashboard (Sprint 9–10)

### TLY-701 Payment processor port with Stripe test and simulator adapters
Lane CORE · 8 pts · Slice S11 · Depends: TLY-404 · Traces: FR-PAY-02, ADR-017

As the platform, I want processors behind one port, so that payment logic does not depend on a vendor.

1. `PaymentProcessor` port: `charge`, `refund`, `status`, `setupToken`; both adapters pass one shared contract test suite.
2. Simulator behavior is keyed by token suffix (`ok`, `decline_51`, `timeout`, `dup_settle`) as in `contracts/fixtures/tenants.json`.
3. Every call carries the processor idempotency key `<payment_id>-<attempt_no>`; retrying the same attempt never double-charges in the simulator.
4. Timeouts are 5 s with no retry inside the request; the adapter returns `Unknown`, never a guess.
5. Processor webhooks (Stripe test) are verified by signature and deduped by processor event id.

### TLY-702 Payments API and async outcome resolution
Lane CORE · 5 pts · Slice S11 · Depends: TLY-701, TLY-202 · Traces: FR-PAY-01, FR-PAY-03, NFR-COR-03

As a tenant, I want to charge customers safely, so that I get paid exactly once.

1. `POST /v1/payments` requires Idempotency-Key, writes PROCESSING, calls the processor outside the transaction, then records the attempt and outcome.
2. Success posts Dr Cash-clearing / Cr Accounts receivable and marks the invoice PAID when fully covered; emits `payment.succeeded`.
3. `Unknown` outcomes stay PROCESSING; the status poller (every 5 min) or processor webhook resolves them; no ledger entry until resolved.
4. `GET /v1/payments?status=` and `GET /v1/payments/summary` (collected, success rate, refunded, failed) are served from indexed queries/read models.
5. Chaos test: processor times out on 30 % of calls; after resolution, ledger totals equal processor totals.

### TLY-703 Dunning schedules
Lane CORE · 5 pts · Slice S11 · Depends: TLY-702, TLY-501 · Traces: FR-PAY-04

As a tenant, I want failed payments retried on a schedule, so that I recover revenue without manual work.

1. A failed payment creates a dunning schedule from the tenant policy (default day 1, 3, 5, 7).
2. Each step is a new attempt with a new processor idempotency key; success ends dunning and reactivates a PAST_DUE subscription.
3. Exhausted dunning sets the subscription PAST_DUE and emits `invoice.overdue`.
4. The dunning job is idempotent per (payment, step) and advanced correctly by test clocks.

### TLY-704 Refunds
Lane CORE · 3 pts · Slice S11 · Depends: TLY-702 · Traces: FR-PAY-05

As a finance user, I want full and partial refunds, so that I can make customers whole.

1. `POST /v1/refunds` with Idempotency-Key refunds up to the remaining refundable amount; over-refund returns 409 with the remaining amount.
2. Each refund posts the reversing entries and sets payment status REFUNDED or PARTIALLY_REFUNDED.
3. `payment.refunded` is emitted with the refund id.
4. Concurrent partial refunds cannot exceed the payment amount (lock test with 20 parallel requests).

### TLY-705 Payments screen
Lane WEB · 5 pts · Slice S11 · Depends: TLY-006 · Traces: FR-PAY-03, FR-PAY-05, NFR-FE-03

As a finance user, I want to see payments and act on them, so that I can resolve failures and refunds quickly.
Design: canvas artboard(s) `Payments`.

1. Summary tiles and status tabs; failed rows show the failure reason in words and next retry time.
2. Row opens the drawer with attempts timeline and payment method (brand + last4 only).
3. Refund requires typing the amount; partial refunds validate against the remaining amount before submit.
4. Refund button disables while pending and uses one Idempotency-Key per intent.

### TLY-706 Reports read models and attention items
Lane CORE · 5 pts · Slice S12 · Depends: TLY-702, TLY-504 · Traces: FR-DASH-01, NFR-API-01

As a tenant, I want fast dashboard numbers, so that Home loads instantly.

1. Projectors consume billing events into `revenue_daily` and `mrr_by_product` via `kernel.inbox`; a rebuild command replays events and produces identical totals.
2. `/v1/reports/summary`, `/revenue?range=`, `/mrr-by-plan` p95 < 300 ms with 2 years of data for one tenant.
3. Attention items are computed from failed payments, disabled endpoints, overdue invoices and trials ending, and can be dismissed per user.
4. Read-model staleness is ≤ 10 s under normal load (metric).

### TLY-707 Home dashboard screen
Lane WEB · 5 pts · Slice S12 · Depends: TLY-006 · Traces: FR-DASH-01, NFR-FE-02

As a tenant admin, I want a home page that tells me how the business is doing and what needs attention, so that I start each day in the right place.
Design: canvas artboard(s) `Main / Home`.

1. KPI cards (revenue this month, MRR, outstanding, failed payments) count up once on first load only; values are exact thereafter.
2. Revenue chart with 7D/30D/12M toggle; line draws on range change; tooltip shows exact money.
3. Attention list staggers in, supports dismiss and deep links; recent payments and upcoming renewals panels.
4. Test-mode banner visible whenever the tenant is in test mode.
5. LCP < 2.5 s and CLS < 0.1 on a throttled laptop profile.

### TLY-708 Daily reconciliation job
Lane CORE · 5 pts · Slice S17 · Depends: TLY-702 · Traces: FR-PAY-06, NFR-COR-05

As finance, I want the ledger reconciled against the processor daily, so that differences are caught within a day.

1. The job (02:00 UTC, idempotent per date) compares processor settlements with ledger cash-clearing postings per payment.
2. Mismatches are classified TIMING, DUPLICATE, MISSING_CAPTURE or AMOUNT and stored in `ops.recon_mismatches`.
3. The simulator's `dup_settle` token produces exactly one DUPLICATE mismatch in the integration test.
4. A run summary (matched, mismatched, difference) is stored in `ops.recon_runs` and exported as a metric.

## E8 — Operator console (Sprint 10–13)

### TLY-801 Operator realm, ops API security and hash-chained audit
Lane CORE · 8 pts · Slice S13 · Depends: TLY-102, TLY-110 · Traces: NFR-ADM-SEC-02, NFR-ADM-SEC-05, NFR-ADM-AUD-01, NFR-ADM-AUD-02, NFR-ADM-AUD-04, FR-ADM-GOV-03, ADR-022, ADR-023, ADR-026

As the platform, I want operator access fully separated and every access recorded tamper-evidently, so that insider risk is controlled.

1. `/ops/v1` on port 8081 accepts only `tally-operators` realm tokens with WebAuthn `amr`; tenant tokens get 401 and vice versa.
2. Tenant-data endpoints require `X-Access-Reason`; the request sets the tenant context for that request only (RLS still applies).
3. Every ops read of tenant data and every write appends to `ops.operator_audit_log` in the same transaction with `hash = sha256(prev_hash || canonical entry)`.
4. A daily verification job recomputes the chain; tampering with one row in a test makes `GET /ops/v1/audit/verification` report `first_broken_seq`.
5. RBAC matrix as code (deny by default) drives endpoint authorization; a generated test asserts every ops endpoint has an explicit rule.

### TLY-802 Four-eyes approval framework
Lane CORE · 5 pts · Slice S13 · Depends: TLY-801 · Traces: FR-ADM-GOV-01, FR-ADM-GOV-02, NFR-ADM-SEC-03, ADR-025

As a platform admin, I want high-risk actions to need a second operator, so that no single person can cause major harm.

1. Gated actions store `approval_requests` with payload, `payload_sha256`, impact text and 24 h expiry, and return 202 with the approval id.
2. Approve requires step-up (`auth_time` < 5 min) and a different operator (DB CHECK + service check); the requester gets 403.
3. `ApprovalExecutor` recomputes the hash and executes the stored payload, never a new request body; a hash mismatch aborts with 409.
4. Expired requests become EXPIRED by job and cannot be approved.
5. Approve, reject and execute are audited with both operator ids.

### TLY-803 Approvals, audit log and operators screens
Lane OPSW · 8 pts · Slice S13 · Depends: TLY-007 · Traces: FR-ADM-GOV-01, FR-ADM-GOV-03, FR-ADM-GOV-04

As an operator, I want to review requests, audit history and roles in the console, so that governance is visible and fast.
Design: canvas artboard(s) `AdminApprovals, AdminAudit, AdminOperators`.

1. Approvals queue shows action, requester, reason, impact and expiry; detail shows the exact stored payload.
2. Approve triggers WebAuthn step-up; own requests show a disabled approve button with explanation.
3. Audit log is filterable by operator, tenant, kind and text, shows the verification status badge, and exports CSV of the filtered range.
4. Operators screen lists roles and pending role requests; requesting a role creates an approval.

### TLY-804 Ops tenants API and overview
Lane CORE · 5 pts · Slice S14 · Depends: TLY-801, TLY-802 · Traces: FR-ADM-TEN-01, FR-ADM-TEN-02, FR-ADM-TEN-03, FR-ADM-TEN-04, FR-ADM-TEN-05, FR-TEN-03

As an operator, I want to find tenants that need attention and act safely, so that I resolve issues before customers notice.

1. `GET /ops/v1/tenants` supports views (NEEDS_ATTENTION, ALL, SCALE, SILO, SUSPENDED), search and health sort; p95 < 300 ms over 10k tenants on the replica.
2. Contact data is masked; `reveal=true` returns it and writes a reveal audit entry.
3. Suspend creates an approval; on execution the tenant becomes SUSPENDED: tenant API writes return 423, reads work, webhook deliveries pause (HELD).
4. `GET /ops/v1/overview` returns platform KPIs and the attention queue; acknowledging an item is audited.
5. Every tenant-data response is audited with the provided reason.

### TLY-805 Ops tenants and overview screens
Lane OPSW · 5 pts · Slice S14 · Depends: TLY-007 · Traces: FR-ADM-TEN-01, FR-ADM-TEN-02, FR-ADM-TEN-03, NFR-ADM-PER-02

As an operator, I want a dense tenant table and overview, so that I can scan the fleet quickly.
Design: canvas artboard(s) `AdminOverview, AdminTenants`.

1. Overview shows KPIs and the attention queue with deep links.
2. Tenants table: saved views, search, health bars, signals; virtualized at 60 fps for 10k rows.
3. Opening a tenant asks for an access reason once per tenant per session and sends it in `X-Access-Reason`.
4. Suspend shows exact impact (tenant, active subscriptions, endpoints) and creates an approval.

### TLY-806 Impersonation with token exchange
Lane CORE · 5 pts · Slice S15 · Depends: TLY-802, TLY-103 · Traces: FR-ADM-IMP-01, FR-ADM-IMP-02, FR-ADM-IMP-03, FR-ADM-IMP-04, FR-ADM-IMP-05, FR-OPS-01, ADR-024

As a support operator, I want to see what a tenant sees, time-boxed and visible to them, so that I can help without hidden access.

1. `POST /ops/v1/impersonations` (reason, 15/30/60 min) performs Keycloak token exchange; the token carries `act.sub = operator` and expires with the session.
2. READ_ONLY tokens cannot call write endpoints (403); WRITE requires an approved request id.
3. Every tenant audit entry during the session records actor = operator and on_behalf_of = tenant user.
4. `GET /v1/account/impersonations` shows the tenant all sessions with operator name, reason, mode and write count.
5. Ending or expiry revokes the token within 5 s.

### TLY-807 Impersonation console screens
Lane OPSW · 5 pts · Slice S15 · Depends: TLY-007 · Traces: FR-ADM-IMP-01, FR-ADM-IMP-03

As an operator, I want to start and end impersonation from the console, so that support sessions are deliberate and time-boxed.
Design: canvas artboard(s) `AdminImpersonation`.

1. Console form: tenant, reason, duration, mode; WRITE requires selecting an approved request.
2. Active sessions list with live countdown and an end action that confirms revocation.
3. Past sessions list per tenant with operator, reason, mode and write count.
4. Starting a session opens the tenant app in a new tab with the exchanged token; the console never stores the token.

### TLY-808 Platform health API and rate-limit overrides
Lane CORE · 5 pts · Slice S16 · Depends: TLY-801, TLY-111 · Traces: FR-ADM-OPS-01, FR-ADM-OPS-02, NFR-ADM-AVL-01

As an SRE, I want platform health and top tenants in one place, so that I can find the cause of an incident fast.

1. `/ops/v1/health/services` builds service tiles from Prometheus queries; data ≤ 15 s old.
2. `/ops/v1/health/top-tenants` returns the top 10 by API share, rpm, ingest rate and webhook backlog over 5 minutes.
3. `/ops/v1/alerts` mirrors Alertmanager; `/ops/v1/streams/health` pushes updates every 15 s.
4. Health endpoints keep working when the core's tenant API is down (they read Prometheus directly).
5. `POST /ops/v1/tenants/{id}/rate-limit` sets a temporary override with expiry, audited.

### TLY-809 Health, webhook fleet and jobs screens
Lane OPSW · 8 pts · Slice S16 · Depends: TLY-007 · Traces: FR-ADM-OPS-01, FR-ADM-OPS-03, FR-ADM-OPS-05

As an SRE, I want live health, fleet and job views, so that I can operate without Grafana for common tasks.
Design: canvas artboard(s) `AdminHealth, AdminFleet, AdminJobs`.

1. Health: service tiles with sparklines and thresholds, top-tenants table, alerts list; live via SSE.
2. Fleet: backlog and DLQ by tenant; bulk replay shows the dry-run (events, endpoints, estimated time) before starting, then progress.
3. Jobs: schedule, last status, duration and history dots; re-run form asks tenant/date and reason.
4. Stale data (> 30 s) shows a 'stale' label.

### TLY-810 Fleet, DLQ replay and job ops APIs
Lane WORK · 5 pts · Slice S16 · Depends: TLY-306, TLY-307 · Traces: FR-ADM-OPS-03, FR-ADM-OPS-04, FR-ADM-OPS-05, FR-ADM-OPS-06

As an SRE, I want to replay a tenant's DLQ safely and re-run jobs, so that I can recover from incidents without scripts.

1. Dispatcher internal API provides fleet backlog/DLQ counts per tenant and DLQ replay with `dry_run` (count, endpoints, estimate) and throttled execution (≤ 20/s per tenant).
2. Replay progress is queryable; replays are idempotent per Idempotency-Key.
3. Core proxies these at `/ops/v1/webhooks/*` and `/ops/v1/replays/{id}` with audit.
4. `/ops/v1/jobs` lists core jobs from `ops.job_runs`; re-run executes idempotently by (job, tenant, date) and returns the existing run if already succeeded.

### TLY-811 Reconciliation, refunds and ledger explorer ops APIs
Lane CORE · 5 pts · Slice S17 · Depends: TLY-708, TLY-802 · Traces: FR-ADM-MON-01, FR-ADM-MON-02, FR-ADM-MON-03, FR-ADM-MON-04

As a finance operator, I want to resolve mismatches, issue refunds and inspect ledgers, so that money issues are closed with a trail.

1. List runs and mismatches; resolve requires classification and a note (≥ 5 chars), audited.
2. Operator refunds ≤ $500 (configurable) execute immediately; above the threshold they create an approval.
3. Ledger accounts and journal entries are read-only per tenant with reason header; no write endpoint exists (asserted by route test).
4. All amounts in responses are Money objects.

### TLY-812 Recon, refunds and ledger explorer screens
Lane OPSW · 8 pts · Slice S17 · Depends: TLY-007 · Traces: FR-ADM-MON-01, FR-ADM-MON-02, FR-ADM-MON-03

As a finance operator, I want console screens for reconciliation, refunds and ledgers, so that I can work money issues end to end.
Design: canvas artboard(s) `AdminRecon, AdminRefunds, AdminLedger`.

1. Recon: run calendar, mismatch table, resolve drawer with classification and note.
2. Refunds: form with target, amount, reason; shows 'needs approval' when above the threshold before submit.
3. Ledger: account balances and journal entries with postings; debits and credits aligned in mono columns.
4. All three screens require an access reason for tenant data.

### TLY-813 Plans, limits and feature flags ops API
Lane CORE · 5 pts · Slice S18 · Depends: TLY-801 · Traces: FR-ADM-CFG-01, FR-ADM-CFG-02, FR-ADM-CFG-03

As a platform admin, I want versioned plans and targeted flags with a dry run, so that config changes never surprise tenants.

1. Creating a plan version with `dry_run=true` returns the diff and affected tenant count without writing.
2. Migrating tenants between versions is idempotent and audited; tenants keep limits until migrated.
3. Flags support targets ALL, PLAN, TIER, TENANT, COHORT and rollout 0/10/25/50/100 %; `dry_run` returns affected tenants.
4. Flag evaluation is deterministic per tenant (hash bucketing) and cached ≤ 30 s.

### TLY-814 Plans and flags screens
Lane OPSW · 5 pts · Slice S18 · Depends: TLY-007 · Traces: FR-ADM-CFG-01, FR-ADM-CFG-02, FR-ADM-CFG-03

As a platform admin, I want to edit plans and flags with a visible diff, so that I know the impact before saving.
Design: canvas artboard(s) `AdminPlans, AdminFlags`.

1. Plans editor shows the current version and a side-by-side diff with affected tenant count before save.
2. Flags list with targeting chips and rollout selector; save shows affected tenants from dry run.
3. Both require a reason and show the resulting audit entry after save.
4. Keyboard-complete.

### TLY-815 Impersonation banner and history in the tenant app
Lane WEB · 3 pts · Slice S15 · Depends: TLY-006 · Traces: FR-ADM-IMP-05, NFR-ADM-UX-02

As a tenant user, I want to see clearly when Tally staff are in my account, so that I can trust what happens there.
Design: canvas artboard(s) `Main / Home (impersonation banner)`.

1. When the session token carries an `act` claim, every page shows a non-dismissible banner with operator name, mode and remaining time.
2. The banner cannot be hidden by CSS overrides from branding and stays above the test-mode banner.
3. Settings lists impersonation history from `GET /v1/account/impersonations`.
4. Write controls are disabled with an explanation during READ_ONLY sessions.

## E9 — Hardening and enterprise readiness (Sprint 13–15)

### TLY-901 Observability: traces, dashboards, SLOs and runbooks
Lane PLAT · 5 pts · Slice — · Depends: TLY-002, TLY-301, TLY-305 · Traces: NFR-OBS-01, NFR-OBS-02, NFR-OBS-03, NFR-OBS-04

As an operator, I want one trace per request across services and per-tenant dashboards, so that incidents are diagnosed in minutes.

1. A request that finalizes an invoice produces one trace spanning core, Kafka, dispatcher and the endpoint call (Tempo query in the test).
2. Grafana dashboards: API RED, outbox, dispatcher (backlog, attempts, fairness), aggregator, payments, per-tenant view.
3. SLOs defined for API availability and webhook delivery with burn-rate alerts; each alert links a runbook in docs/runbooks/.
4. A CI check fails if a metric uses a `tenant_id` label outside the allow-list.

### TLY-902 Load suite with skewed tenants
Lane PLAT · 5 pts · Slice — · Depends: TLY-601, TLY-702, TLY-305 · Traces: NFR-PER-01, NFR-PER-02, NFR-PER-03, NFR-FAIR-01, NFR-FAIR-04

As the tech lead, I want repeatable load tests with a noisy neighbor, so that performance and fairness claims are measured.

1. k6 scenarios: writes 200 rps, reads 500 rps, ingest 5,000 ev/s, with 1,000 tenants where one tenant sends 50 % of traffic.
2. Thresholds from docs/02 §2.2 are k6 thresholds; the run fails when any is missed.
3. Results (p50/p95/p99 per tenant class) are published as a Markdown report artifact.
4. `make load` runs a 5-minute smoke version locally.

### TLY-903 Chaos and resilience suite
Lane PLAT · 5 pts · Slice — · Depends: TLY-306, TLY-702, TLY-602 · Traces: NFR-COR-02, NFR-COR-03, NFR-AVL-03, NFR-AVL-02

As the tech lead, I want failure injected on purpose, so that correctness under failure is proven, not assumed.

1. Scenarios in docs/08 §7 run with Toxiproxy and container kills; each asserts its invariant automatically.
2. Broker kill during load: 0 lost events, 0 duplicate deliveries per (event, endpoint).
3. Processor timeout storm: ledger = processor after resolution.
4. Aggregator kill -9: rollup totals unchanged.
5. `make chaos` runs the suite in < 20 minutes.

### TLY-904 Security suite: cross-tenant, SSRF, secrets, DAST
Lane PLAT · 5 pts · Slice — · Depends: TLY-102, TLY-303 · Traces: NFR-SEC-01, NFR-SEC-02, NFR-SEC-04, NFR-SEC-06

As the tech lead, I want isolation and common attack paths tested automatically, so that regressions cannot ship.

1. `CrossTenantAccessIT` is generated from `contracts/openapi.yaml`: for every path with an id, tenant B using tenant A's ids gets 404; for every list, B sees none of A's objects.
2. SSRF suite covers private ranges, IPv6, decimal/octal IP encodings, redirects and DNS rebinding.
3. Log scan asserts no API key, webhook secret, token or email in plain text across a full E2E run.
4. OWASP ZAP baseline against web and core has 0 high findings.

### TLY-905 Tenant data export and crypto-shred offboarding
Lane CORE · 8 pts · Slice — · Depends: TLY-802, TLY-403 · Traces: FR-TEN-06, FR-TEN-07, NFR-SEC-05, NFR-CMP-01, ADR-020

As a tenant, I want to export my data and have it destroyed when I leave, so that my customers' data is not kept after I go.

1. `POST /v1/account/exports` produces a ZIP of JSON Lines for every tenant table within 1 h for 1M rows; the link expires after 7 days.
2. Offboarding (ops, four-eyes) exports, then destroys the tenant DEK; afterwards encrypted fields are unreadable and the tenant cannot authenticate.
3. Ledger entries remain (legal record) but reference only pseudonymous ids.
4. The full flow completes within 30 days by policy and is audited.

### TLY-906 Pool to silo live migration
Lane CORE · 8 pts · Slice — · Depends: TLY-905, TLY-801 · Traces: FR-OPS-03, FR-ADM-TEN-08, ADR-021

As a platform admin, I want to move a large tenant to its own database without downtime, so that enterprise tenants get stronger isolation.

1. Migration (four-eyes) copies the tenant's rows to a dedicated database via logical replication filtered by tenant_id.
2. Writes pause ≤ 5 s during cutover; the tenant routing table switches the tenant's datasource; no request fails with 5xx during the drill.
3. Row counts and ledger balances match between source and target before cutover.
4. Rollback within 15 minutes is rehearsed and documented in a runbook.

### TLY-909 Accessibility and motion audit
Lane WEB · 3 pts · Slice — · Depends: TLY-707, TLY-506 · Traces: NFR-FE-01, NFR-ADM-UX-04

As a user with assistive technology, I want every screen usable by keyboard and screen reader, so that I can do my job.

1. axe reports 0 violations on all 16 tenant and 14 operator screens in CI.
2. Every screen has a keyboard-only Playwright journey.
3. Reduced-motion snapshots exist for every animated component.
4. Status indicators are verified to include text or icon, not color alone.

### TLY-910 Backup, restore and zero-downtime migration drill
Lane PLAT · 3 pts · Slice — · Depends: TLY-002 · Traces: NFR-AVL-04, NFR-AVL-05

As the tech lead, I want restore and migration procedures rehearsed, so that data loss and downtime stay within targets.

1. WAL archiving + nightly base backup configured; a restore drill to a point in time completes in < 1 h with ≤ 5 min data loss.
2. An expand-migrate-contract column rename runs under k6 load with 0 failed requests.
3. Runbooks for both are in docs/runbooks/ and linked from alerts.
4. Staging on k3d with the Helm chart comes up with `make staging-up`.
