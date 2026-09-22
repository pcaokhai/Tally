# Engineering Standards

Version 1.0 · 2026-09-22 · Owner: Tech lead

These are the rules an experienced engineer applies on a production multi-tenant billing system. Reviewers (human or requesting-code-review) check against this document. **MUST** items block merge.

## 0. Domain non-negotiables

1. **Tenant isolation (MUST).** Tenant id only from the credential; `ScopedValue<TenantContext>` → `SET LOCAL app.tenant_id` in every transaction; RLS `FORCE` on every tenant table; composite FKs. No BYPASSRLS role, no `SET row_security = off`, no "admin" query path in the tenant API. The operator API reads tenant data by setting the same context per request (ADR-023).
2. **Money (MUST).** `Money(long minor, Currency)`; `Math.addExact/subtractExact/multiplyExact`; rounding only in the pricing engine with `RoundingMode.HALF_EVEN` documented per rule; cross-currency arithmetic throws.
3. **Ledger (MUST).** Every money movement is one `JournalEntry` with ≥ 2 postings summing to zero per currency, written by `LedgerService.post` in the same transaction as the business change. Corrections are reversals (`reverses_id`).
4. **Idempotency (MUST).** Every creating or money-moving endpoint requires `Idempotency-Key`; every consumer dedupes; every job run has an idempotency key.
5. **Unknown outcome (MUST).** A processor timeout never becomes success or failure by guessing; the payment stays `PROCESSING` until the processor says otherwise.
6. **Events (MUST).** Integration events only via the outbox in the same transaction; envelopes validate against `contracts/events`.
7. **PCI / PII (MUST).** No PAN, CVV or bank account numbers anywhere; processor tokens only. Customer emails encrypted at rest; masked in operator views unless revealed through the audited endpoint.
8. **Audit (MUST).** Every state change writes `kernel.audit_log` (actor + on_behalf_of); every operator read of tenant data or write writes `ops.operator_audit_log` in the same transaction.

## 1. Principles

| Principle | What it means here | Example |
| --- | --- | --- |
| Single Responsibility | One reason to change | `InvoiceFinalizer` finalizes; it does not send emails (reminders module listens to the event) |
| Open/Closed | Extend by adding | New price model = new `PriceModelStrategy` (`PerUnit`, `Flat`, `Graduated`) |
| Liskov Substitution | Implementations honor the port contract, errors and timing included | `StripeTestProcessor` and `SimulatorProcessor` pass the same `PaymentProcessorContractTest` |
| Interface Segregation | Small consumer-owned interfaces | Go `delivery` depends on `Signer` (one method), not the whole `signing` package |
| Dependency Inversion | Application depends on ports | `FinalizeInvoice` depends on `InvoiceRepository`, `LedgerPort`, `OutboxPort` |
| KISS / YAGNI | Build what the story needs | No plugin system for price models until a fourth model exists |
| DRY, rule of three | Abstract on the third occurrence of the same concept | Tenant `Invoice` DTO and ops `OpsInvoice` stay separate |
| Fail fast | Validate config and inputs at the edge | Missing KEK ⇒ core does not start |
| Explicit over implicit | No hidden wiring or global state | Constructor injection; typed `@ConfigurationProperties` records |
| Illegal states unrepresentable | Types encode invariants | Sealed `PaymentOutcome`; `Money` cannot mix currencies; DB CHECK draft ⇔ `finalized_at IS NULL` |

## 2. Architecture rules

- **Hexagonal layering** in core modules and Go packages: `domain` → `application` (ports + use cases) → `adapter` (in: REST, Kafka; out: DB, Kafka, HTTP). Dependencies point inward. Enforced by ArchUnit + Spring Modulith `verify()` (Java) and depguard (Go).
- Domain code has no Spring, Jackson, Hibernate, jOOQ, Kafka, franz-go, pgx or net/http imports and performs no I/O.
- One use case = one application service method with a command/query record; the transaction boundary is that method.
- Modules communicate through `api` packages or application events; no cross-module repositories, no cross-schema FKs.
- Services communicate only via REST contracts and Kafka events; no shared databases; shared code limited to generated contract types.
- Every external dependency sits behind a port with a fake (in-memory repository, `SimulatorProcessor`, fake clock).

## 3. Design patterns (use for the named problem)

| Pattern | Problem | Where |
| --- | --- | --- |
| State | Legal lifecycle transitions | `SubscriptionStateMachine`, invoice status, delivery message status |
| Strategy | Interchangeable algorithms | Price models, proration policy, dunning policy, rate-limit resolution |
| Specification | Composable predicates | Invoice reminder eligibility, flag targeting |
| Builder | Complex construction with validation | `JournalEntry.builder()` (rejects unbalanced), `InvoiceDraftBuilder` |
| Repository + Unit of Work | Persistence with transaction boundaries | JPA/jOOQ repositories, Go `store.Tx` |
| Transactional Outbox / Inbox | Reliable publish / idempotent consume | kernel outbox, `kernel.inbox`, dispatcher dedup |
| Idempotency Key | Safe retries | `IdempotencyFilter`, processor calls, job runs |
| Circuit Breaker | Protect callers | dispatcher per-endpoint breaker, processor adapter |
| Retry with backoff + jitter | Transient failures | dispatcher schedule, dunning |
| Bulkhead | Isolate tenants/endpoints | per-endpoint in-flight cap, per-tenant queues |
| Fair queuing (WRR) | Noisy neighbor | dispatcher scheduler |
| CQRS read model | Fast aggregates | `reporting` projectors |
| Command + stored payload | Four-eyes | `ApprovalExecutor` |
| Adapter / Anti-corruption layer | External models | Stripe adapter, dispatcher proxy in `events` |
| Observer / Pub-Sub | Fan-out | SSE hubs, Spring application events |
| Value Object | Invariants on small values | `Money`, `TenantId`, `PublicId`, `IdempotencyKey`, `EventId` |

Anti-patterns that fail review: god services, anemic `*Manager` classes, boolean flag parameters that switch behavior, `util`/`common` dumping grounds, deep inheritance, swallowed errors, stringly-typed ids/statuses, mutable singletons, temporal coupling without enforcement, JPA entities leaking to controllers.

## 4. Code conventions

### 4.1 All languages

- Names from the glossary (root CLAUDE.md §8). Functions are verbs, booleans predicates, collections plural.
- Functions ≤ ~30 lines, ≤ 4 parameters, cyclomatic complexity ≤ 10; files ≤ ~300 lines.
- No magic numbers/strings: `WEBHOOK_SIGNATURE_TOLERANCE`, `ProblemCode.IDEMPOTENCY_KEY_REUSED`.
- Comments explain why and cite specs ("docs/03 §7"); TODOs carry a story id.
- Formatting is automatic: Spotless (palantir-java-format), gofmt/goimports, Prettier.

### 4.2 Java 25 (core)

- Records for value objects, commands, DTOs; sealed interfaces + pattern-matching `switch` (exhaustive, no `default`) for closed sets.
- `final` fields, constructor injection, `List.copyOf`; no Lombok.
- JSpecify `@NullMarked`; no `null` returns; `Optional` only as return type.
- `ScopedValue` for request context; never `ThreadLocal`. Virtual threads for request handling and blocking adapters; no `synchronized` around I/O; `ReentrantLock` where a lock is unavoidable.
- No preview features (structured concurrency, primitive patterns) until they are final.
- Checked exceptions only in adapters; domain throws typed unchecked exceptions carrying a `ProblemCode`.
- Streams for transformations, loops for side effects; ≤ 2 levels of nested lambdas.
- Package-private by default; `public` only for module `api` packages and ports.

### 4.3 Go 1.27 (workers)

- Effective Go + Go Code Review Comments; golangci-lint set in workers/CLAUDE.md.
- `ctx` first; every goroutine owned and cancellable; sender closes channels; `goleak` in tests.
- Errors: `%w`, typed/sentinel, `errors.Is/As`; no `panic` outside `main`.
- Constructors validate deps; accept interfaces, return structs; interfaces at the consumer.
- Generic methods (1.27) only where they remove real duplication.
- Package names short and singular; no `util`, `common`, `helpers`.

### 4.4 TypeScript / React (web, ops-web)

- `strict`, `noUncheckedIndexedAccess`, `exactOptionalPropertyTypes`; no `any`; Zod at trust boundaries.
- Server Components by default; client components small leaves; TanStack Query for client data, keys include tenant id.
- Pure components; side effects in `useX` hooks; one component per file; named exports; stable keys.
- Tailwind tokens only; motion only via `lib/motion` tokens; reduced-motion variants required.
- Accessibility is part of done (semantic elements, labels, focus, live regions).

### 4.5 SQL

- Explicit column lists; no `SELECT *` in application code.
- Parameterized only (jOOQ, JPA, sqlc); never string concatenation.
- Lock intentionally (`FOR UPDATE`, `SKIP LOCKED`) with a comment citing the story.
- Every hot query has an index in docs/05 §6; `EXPLAIN (ANALYZE)` output in the PR for new hot queries.

## 5. Error handling

- Classes: **validation** (400), **business outcome** (a state, not an error), **conflict/state** (409/412), **transient** (retry with backoff), **fatal** (bug/config, 500 + alert).
- One `ProblemMapper` per service; never leak stack traces, SQL or internal ids of other tenants; unknown ids of other tenants return 404.
- Retry only transient errors, with backoff + jitter and a cap; never retry non-idempotent calls without a key.
- Every caught exception is handled with a logged decision or wrapped and rethrown.

## 6. Observability

- Levels: ERROR = human action needed; WARN = degraded but handled; INFO = one line per business outcome; DEBUG off by default.
- JSON logs, constant messages, data in fields (docs/02 §7.6). Never log secrets, tokens, emails in clear, request bodies of money endpoints.
- RED metrics for every inbound interface, USE for pools/queues, business metrics with `tally_` prefix; `tenant_id` label only on the allow-list (NFR-OBS-03).
- Spans for inbound requests, transactions, outbound calls, Kafka produce/consume, webhook attempts.

## 7. Security

- Secrets from env/secret files; `.env` git-ignored; gitleaks in CI.
- Validate at the edge (OpenAPI request validation); reject unknown fields on ops APIs.
- Least privilege: app DB role without BYPASSRLS; ledger and audit tables without UPDATE/DELETE grants; each Keycloak client has minimal scopes.
- Dependencies pinned, Renovate weekly, no critical CVEs, permissive licenses only.
- Constant-time comparison for signatures, API key hashes and tokens.
- SSRF: outbound HTTP to tenant-supplied URLs only through the guarded dialer.

## 8. Concurrency and consistency

- One DB transaction per use case; no network calls inside it.
- Pessimistic lock for invoice numbering; optimistic `version` for editable resources; both covered by concurrent tests (e.g. 50 parallel finalizes produce 50 distinct gap-free numbers).
- Timeouts on every I/O (docs/02 §7.4); bounded pools and queues.
- Graceful shutdown: stop intake → drain in-flight (30 s) → flush outbox/leases → close.

## 9. Performance

- Measure before optimizing; perf PRs include a k6 or JMH result.
- No N+1 (Hibernate statistics assertion in tests); batch relay and billing run.
- Web: route-level splitting, virtualized long lists, SSE updates batched per frame, Lighthouse ≥ 90 on Home.

## 10. Testing

See docs/08. Arrange-act-assert; one behavior per test; fakes by default; no shared mutable test state; fixed `Clock` in all domain tests.

## 11. Git and pull requests

- Conventional Commits with scope = lane (`core`, `work`, `web`, `opsw`, `plat`, `contracts`) and story id: `feat(core): finalize invoice with gap-free numbers (TLY-505)`.
- One story per PR, ≤ 400 changed lines excluding generated code; draft PR early for 8-point stories.
- PR template sections all filled; screenshots/clip for UI in light, dark (ops) and reduced-motion.
- No force-push to `main`; squash merges only.

## 12. Code review checklist

- [ ] Each AC proven by a named test that fails without the change
- [ ] Layering and module boundaries respected; no framework in domain
- [ ] Tenant isolation: context set, RLS on, composite FKs, no cross-tenant leak in errors
- [ ] Money, ledger, idempotency, unknown-outcome rules honored
- [ ] Errors classified and mapped once; nothing swallowed
- [ ] Concurrency: ownership, cancellation, locks, timeouts
- [ ] Logs/metrics/traces added; no sensitive data; bounded cardinality
- [ ] Contracts respected; generated code untouched; migrations forward-only, number reserved
- [ ] Names, function size, comments explain why
- [ ] Docs updated in the same PR

## 13. Documentation

- ADR for decisions that are hard to reverse, cross lanes or change a contract (`docs/adr/ADR-<nnn>-<slug>.md`).
- Each service README: purpose, run, config table, ports, troubleshooting.
- A runbook per alert in `docs/runbooks/`.
- Plans in `docs/plans/` are kept after merge.
