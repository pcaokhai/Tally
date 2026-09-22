-- Dispatcher database (owned by WORK lane, goose migrations in workers/migrations). Never written by core.
CREATE TABLE endpoint_projection (
  endpoint_id uuid PRIMARY KEY, tenant_id uuid NOT NULL, url text NOT NULL, event_types text[] NOT NULL,
  status text NOT NULL, secrets_enc jsonb NOT NULL, weight int NOT NULL DEFAULT 1, updated_at timestamptz NOT NULL);
CREATE TABLE delivery_messages (
  id uuid PRIMARY KEY, tenant_id uuid NOT NULL, endpoint_id uuid NOT NULL, event_id text NOT NULL, event_type text NOT NULL,
  payload bytea NOT NULL, status text NOT NULL CHECK (status IN ('PENDING','INFLIGHT','DELIVERED','RETRYING','HELD','DEAD_LETTERED')),
  attempts int NOT NULL DEFAULT 0, next_attempt_at timestamptz NOT NULL DEFAULT now(), replay_of uuid,
  created_at timestamptz NOT NULL DEFAULT now(), dead_lettered_at timestamptz);
CREATE UNIQUE INDEX delivery_dedup ON delivery_messages (event_id, endpoint_id) WHERE replay_of IS NULL;
CREATE INDEX delivery_due ON delivery_messages (tenant_id, status, next_attempt_at);
CREATE TABLE delivery_attempts (
  id uuid PRIMARY KEY, message_id uuid NOT NULL REFERENCES delivery_messages(id), attempt_no int NOT NULL,
  status_code int, latency_ms int, dns_ms int, connect_ms int, tls_ms int, wait_ms int, download_ms int,
  error text, attempted_at timestamptz NOT NULL DEFAULT now(), UNIQUE (message_id, attempt_no));
CREATE TABLE endpoint_health_hourly (
  endpoint_id uuid NOT NULL, hour timestamptz NOT NULL, delivered int NOT NULL DEFAULT 0, failed int NOT NULL DEFAULT 0,
  p95_ms int, PRIMARY KEY (endpoint_id, hour));
CREATE TABLE replays (id uuid PRIMARY KEY, tenant_id uuid NOT NULL, requested_by text NOT NULL, total int NOT NULL, done int NOT NULL DEFAULT 0, status text NOT NULL);
-- Redis keyspace: wh:q:<tenant_id> (list of message ids), wh:tenants (zset of tenants with work), wh:lease:<message_id> (TTL 30 s),
-- wh:cb:<endpoint_id> (circuit state). Redis is a cache of scheduling state; Postgres is the source of truth (ADR-011).
