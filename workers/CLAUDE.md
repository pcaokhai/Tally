# workers — CLAUDE.md

Two Go 1.27 binaries that consume the core's Kafka topics. **dispatcher** delivers webhooks with per-tenant fairness, signing, retries, circuit breaking and a DLQ; **aggregator** turns raw usage events into windowed rollups and publishes them back to Kafka. Lane: **WORK**. Owns: `workers/**`, database `dispatcher` (Postgres), Redis keyspace `wh:*`, migrations `workers/migrations/`.

## Commands

```
go build ./...                          build both binaries
go test ./...                           unit tests
go test -tags=integration ./...         Testcontainers (Postgres, Kafka, Redis)
go test -race ./...                     race detector (required in CI)
golangci-lint run                       errcheck, govet, staticcheck, revive, gocyclo, depguard, gosec, bodyclose, contextcheck
sqlc generate / oapi-codegen …          regenerate (never edit output); via `make gen` at repo root
go run ./cmd/dispatcher  |  go run ./cmd/aggregator
```

## Layout

```
cmd/dispatcher/main.go      wiring only: config → deps → run → graceful shutdown
cmd/aggregator/main.go
internal/
  config/        typed env config, validated at startup (fail fast)
  events/        generated types from contracts/events (read-only)
  consumer/      franz-go consumer: persist DeliveryMessage, then commit offset (ADR-011)
  scheduler/     per-tenant queues (Redis), weighted round-robin, lease + heartbeat
  delivery/      bounded worker pool, HTTP client with per-attempt deadline, timing capture
  guard/         SSRF dialer (blocks private/link-local/metadata ranges at dial time), circuit breaker per endpoint
  signing/       HMAC-SHA256 signer; verified against contracts/webhooks/signature-vectors.json
  retry/         backoff schedule + full jitter, DLQ transition, auto-disable after 50 consecutive failures
  store/         pgx + sqlc repositories for dispatcher DB (delivery_messages, delivery_attempts, endpoint projection)
  api/           internal REST for the core proxy (deliveries, replay, health buckets, fleet, SSE fan-out)
  rollup/        tumbling 5-min windows, watermark, late-event policy, dedup by event_id (aggregator)
  telemetry/     OTel tracer/meter setup, slog JSON handler
```

## Go 1.27 rules

- `ctx context.Context` first on anything that blocks; never stored in structs.
- Every goroutine has an owner and exits on `ctx.Done()`; channels closed only by the sender; `goleak.VerifyNone` in tests of long-running components; use the `goroutineleak` pprof profile when debugging stuck workers.
- Errors wrapped with `%w`; typed/sentinel errors with `errors.Is/As`; no `panic` outside `main`.
- Small consumer-side interfaces; constructors `NewX(deps) (*X, error)` validate dependencies.
- Generic methods (new in 1.27) are allowed where they remove duplication in `store/`; do not use them to build frameworks.
- `log/slog` JSON only; fields `tenant.id`, `endpoint.id`, `event.id`, `attempt`, `trace_id`.

## Domain rules

- **Offset commit after persist, never after delivery.** A slow tenant endpoint must never block a partition.
- One `delivery_messages` row per (`event_id`, `endpoint_id`), unique index; redelivered Kafka messages are no-ops.
- Retry schedule: 30 s, 2 m, 8 m, 30 m, 2 h, 6 h, 12 h, 24 h (8 attempts, full jitter), then DLQ (ADR-012).
- Circuit breaker per endpoint: open after 10 consecutive failures, half-open probe every 60 s; messages stay HELD, not dropped.
- Workers never write the core database; state changes the core needs (auto-disable, circuit changes) go out as `webhook-control` events.
- Aggregator: counts are exactly-once per `event_id` using the dedup set in the window state; a rollup is re-emitted with the same `rollup_key` if recomputed, and the core upserts idempotently.

## Tests

- Table-driven tests; names `Test<Unit>_<Behavior>__TLY_nnn_ACn`.
- `signing` must pass every vector in `contracts/webhooks/signature-vectors.json`.
- Fairness test (TLY-305): one tenant with a dead endpoint and 100k backlog; other tenants' p95 time-to-first-attempt stays within 10 % of baseline.
- Chaos (TLY-903): kill Kafka broker / Redis mid-run; assert zero lost and zero duplicated deliveries per `(event_id, endpoint_id)`.
