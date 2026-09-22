-- Tally core database baseline (docs/05). Becomes Flyway V1..V9 in TLY-004/102/202 etc.
-- Rules: tenant_id on every tenant table, RLS FORCE, composite FKs (tenant_id, x_id) -> (tenant_id, id),
-- money = bigint minor units + char(3) currency, statuses = text + CHECK, timestamptz everywhere.

CREATE EXTENSION IF NOT EXISTS pgcrypto;
CREATE ROLE tally_app NOLOGIN NOBYPASSRLS;          -- application role; never BYPASSRLS (ADR-002)
CREATE ROLE tally_readonly NOLOGIN NOBYPASSRLS;     -- replica reads for reporting/ops aggregates

CREATE SCHEMA tenancy; CREATE SCHEMA catalog; CREATE SCHEMA customers; CREATE SCHEMA billing;
CREATE SCHEMA metering; CREATE SCHEMA payments; CREATE SCHEMA ledger; CREATE SCHEMA kernel;
CREATE SCHEMA reporting; CREATE SCHEMA ops; CREATE SCHEMA events;

-- Helper: current tenant from SET LOCAL app.tenant_id (fails closed when unset)
CREATE FUNCTION kernel.current_tenant() RETURNS uuid LANGUAGE sql STABLE AS
$$ SELECT nullif(current_setting('app.tenant_id', true), '')::uuid $$;

-- ===== tenancy =====
CREATE TABLE tenancy.plan_versions (
  id uuid PRIMARY KEY, plan_code text NOT NULL, version int NOT NULL, limits jsonb NOT NULL,
  created_at timestamptz NOT NULL DEFAULT now(), UNIQUE (plan_code, version));
CREATE TABLE tenancy.tenants (
  id uuid PRIMARY KEY, plan_version_id uuid NOT NULL REFERENCES tenancy.plan_versions(id),
  name text NOT NULL, status text NOT NULL CHECK (status IN ('ACTIVE','SUSPENDED','OFFBOARDING','DELETED')),
  isolation_tier text NOT NULL DEFAULT 'POOL' CHECK (isolation_tier IN ('POOL','SILO')),
  region text NOT NULL DEFAULT 'local', dek_wrapped bytea, created_at timestamptz NOT NULL DEFAULT now());
CREATE TABLE tenancy.users (id uuid PRIMARY KEY, email text NOT NULL UNIQUE, name text, mfa_type text);
CREATE TABLE tenancy.memberships (
  tenant_id uuid NOT NULL REFERENCES tenancy.tenants(id), user_id uuid NOT NULL REFERENCES tenancy.users(id),
  role text NOT NULL CHECK (role IN ('OWNER','ADMIN','DEVELOPER','FINANCE','VIEWER')),
  status text NOT NULL CHECK (status IN ('ACTIVE','REMOVED')), joined_at timestamptz NOT NULL DEFAULT now(),
  PRIMARY KEY (tenant_id, user_id));
CREATE TABLE tenancy.invitations (
  id uuid PRIMARY KEY, tenant_id uuid NOT NULL REFERENCES tenancy.tenants(id), email text NOT NULL,
  role text NOT NULL, token_hash text NOT NULL, expires_at timestamptz NOT NULL, accepted_at timestamptz,
  UNIQUE (tenant_id, id));
CREATE TABLE tenancy.api_keys (
  id uuid PRIMARY KEY, tenant_id uuid NOT NULL REFERENCES tenancy.tenants(id), name text NOT NULL,
  mode text NOT NULL CHECK (mode IN ('LIVE','TEST')), prefix text NOT NULL UNIQUE, last4 char(4) NOT NULL,
  secret_hash text NOT NULL, scopes text[] NOT NULL, last_used_at timestamptz, revoked_at timestamptz,
  created_at timestamptz NOT NULL DEFAULT now(), UNIQUE (tenant_id, id));
CREATE TABLE tenancy.tenant_branding (
  tenant_id uuid PRIMARY KEY REFERENCES tenancy.tenants(id), accent_hex char(7) NOT NULL DEFAULT '#4B3FD1',
  logo_asset_key text, version int NOT NULL DEFAULT 1);
CREATE TABLE tenancy.attention_dismissals (
  tenant_id uuid NOT NULL, user_id uuid NOT NULL, item_key text NOT NULL, dismissed_at timestamptz NOT NULL DEFAULT now(),
  PRIMARY KEY (tenant_id, user_id, item_key));

-- ===== customers =====
CREATE TABLE customers.customers (
  id uuid PRIMARY KEY, tenant_id uuid NOT NULL REFERENCES tenancy.tenants(id),
  email_enc bytea NOT NULL, email_hash text NOT NULL, name text, currency char(3) NOT NULL,
  metadata jsonb NOT NULL DEFAULT '{}', created_at timestamptz NOT NULL DEFAULT now(), version int NOT NULL DEFAULT 1,
  UNIQUE (tenant_id, id), UNIQUE (tenant_id, email_hash));
CREATE TABLE customers.payment_methods (
  id uuid PRIMARY KEY, tenant_id uuid NOT NULL, customer_id uuid NOT NULL,
  processor text NOT NULL CHECK (processor IN ('STRIPE_TEST','SIMULATOR')), processor_token text NOT NULL,
  type text NOT NULL CHECK (type IN ('CARD','ACH')), brand text, last4 char(4), exp_month smallint, exp_year smallint,
  detached_at timestamptz, UNIQUE (tenant_id, id),
  FOREIGN KEY (tenant_id, customer_id) REFERENCES customers.customers(tenant_id, id));

-- ===== catalog =====
CREATE TABLE catalog.products (
  id uuid PRIMARY KEY, tenant_id uuid NOT NULL REFERENCES tenancy.tenants(id), name text NOT NULL,
  kind text NOT NULL CHECK (kind IN ('SEAT','FLAT','METERED')), active boolean NOT NULL DEFAULT true,
  UNIQUE (tenant_id, id));
CREATE TABLE catalog.prices (
  id uuid PRIMARY KEY, tenant_id uuid NOT NULL, product_id uuid NOT NULL, version int NOT NULL,
  model text NOT NULL CHECK (model IN ('PER_UNIT','FLAT','GRADUATED')), unit_amount_minor bigint CHECK (unit_amount_minor >= 0),
  currency char(3) NOT NULL, billing_interval text NOT NULL CHECK (billing_interval IN ('MONTH','YEAR')),
  meter_id uuid, created_at timestamptz NOT NULL DEFAULT now(), archived_at timestamptz,
  UNIQUE (tenant_id, id), UNIQUE (tenant_id, product_id, version),
  FOREIGN KEY (tenant_id, product_id) REFERENCES catalog.products(tenant_id, id));
CREATE TABLE catalog.price_tiers (
  tenant_id uuid NOT NULL, price_id uuid NOT NULL, up_to bigint, unit_amount_minor bigint NOT NULL, flat_amount_minor bigint NOT NULL DEFAULT 0,
  PRIMARY KEY (price_id, up_to), FOREIGN KEY (tenant_id, price_id) REFERENCES catalog.prices(tenant_id, id));
-- prices are immutable once referenced: trigger catalog.prices_immutable blocks UPDATE of amount/model/currency (TLY-401)

-- ===== billing =====
CREATE TABLE billing.subscriptions (
  id uuid PRIMARY KEY, tenant_id uuid NOT NULL, customer_id uuid NOT NULL,
  status text NOT NULL CHECK (status IN ('TRIALING','ACTIVE','PAST_DUE','CANCELED')),
  current_period_start timestamptz NOT NULL, current_period_end timestamptz NOT NULL, trial_end timestamptz,
  cancel_at_period_end boolean NOT NULL DEFAULT false, canceled_at timestamptz, test_clock_id uuid,
  version int NOT NULL DEFAULT 1, UNIQUE (tenant_id, id),
  FOREIGN KEY (tenant_id, customer_id) REFERENCES customers.customers(tenant_id, id));
CREATE TABLE billing.subscription_items (
  id uuid PRIMARY KEY, tenant_id uuid NOT NULL, subscription_id uuid NOT NULL, price_id uuid NOT NULL,
  quantity int NOT NULL CHECK (quantity >= 1), UNIQUE (tenant_id, id),
  FOREIGN KEY (tenant_id, subscription_id) REFERENCES billing.subscriptions(tenant_id, id),
  FOREIGN KEY (tenant_id, price_id) REFERENCES catalog.prices(tenant_id, id));
CREATE TABLE billing.invoice_number_seqs (tenant_id uuid PRIMARY KEY, prefix text NOT NULL, next_value bigint NOT NULL DEFAULT 1);
CREATE TABLE billing.invoices (
  id uuid PRIMARY KEY, tenant_id uuid NOT NULL, number text, customer_id uuid NOT NULL, subscription_id uuid,
  status text NOT NULL CHECK (status IN ('DRAFT','OPEN','PAID','VOID','UNCOLLECTIBLE')),
  currency char(3) NOT NULL, total_minor bigint NOT NULL DEFAULT 0, amount_due_minor bigint NOT NULL DEFAULT 0,
  period_start timestamptz, period_end timestamptz, due_date date, finalized_at timestamptz, memo text,
  version int NOT NULL DEFAULT 1, UNIQUE (tenant_id, id), UNIQUE (tenant_id, number),
  FOREIGN KEY (tenant_id, customer_id) REFERENCES customers.customers(tenant_id, id),
  FOREIGN KEY (tenant_id, subscription_id) REFERENCES billing.subscriptions(tenant_id, id),
  CHECK ((status = 'DRAFT') = (finalized_at IS NULL)));
CREATE TABLE billing.invoice_lines (
  id uuid PRIMARY KEY, tenant_id uuid NOT NULL, invoice_id uuid NOT NULL, price_id uuid, usage_rollup_id uuid,
  description text NOT NULL, quantity bigint NOT NULL, unit_amount_minor bigint NOT NULL, amount_minor bigint NOT NULL,
  proration boolean NOT NULL DEFAULT false, UNIQUE (tenant_id, id),
  FOREIGN KEY (tenant_id, invoice_id) REFERENCES billing.invoices(tenant_id, id));
-- trigger billing.invoice_lines_frozen: no INSERT/UPDATE/DELETE when parent invoice is not DRAFT (FR-INV-02)
CREATE TABLE billing.credit_notes (
  id uuid PRIMARY KEY, tenant_id uuid NOT NULL, invoice_id uuid NOT NULL, amount_minor bigint NOT NULL CHECK (amount_minor > 0),
  reason text NOT NULL, issued_at timestamptz NOT NULL DEFAULT now(),
  FOREIGN KEY (tenant_id, invoice_id) REFERENCES billing.invoices(tenant_id, id));
CREATE TABLE billing.invoice_reminders (
  id uuid PRIMARY KEY, tenant_id uuid NOT NULL, invoice_id uuid NOT NULL, channel text NOT NULL DEFAULT 'EMAIL',
  sent_at timestamptz NOT NULL DEFAULT now(), FOREIGN KEY (tenant_id, invoice_id) REFERENCES billing.invoices(tenant_id, id));
CREATE TABLE billing.test_clocks (id uuid PRIMARY KEY, tenant_id uuid NOT NULL, frozen_time timestamptz NOT NULL, UNIQUE (tenant_id, id));

-- ===== metering =====
CREATE TABLE metering.meters (
  id uuid PRIMARY KEY, tenant_id uuid NOT NULL, key text NOT NULL, aggregation text NOT NULL CHECK (aggregation IN ('SUM','MAX','UNIQUE_COUNT')),
  unit text NOT NULL, late_event_policy text NOT NULL DEFAULT 'REJECT' CHECK (late_event_policy IN ('REJECT','NEXT_PERIOD')),
  UNIQUE (tenant_id, id), UNIQUE (tenant_id, key));
CREATE TABLE metering.usage_events (
  tenant_id uuid NOT NULL, event_id text NOT NULL, meter_id uuid NOT NULL, customer_id uuid NOT NULL,
  quantity numeric(20,6) NOT NULL CHECK (quantity >= 0), occurred_at timestamptz NOT NULL, received_at timestamptz NOT NULL DEFAULT now(),
  PRIMARY KEY (tenant_id, event_id, received_at)) PARTITION BY RANGE (received_at);
-- monthly partitions created by the partition-maintenance job; dedup enforced via metering.usage_event_ids (tenant_id, event_id) PK
CREATE TABLE metering.usage_event_ids (tenant_id uuid NOT NULL, event_id text NOT NULL, PRIMARY KEY (tenant_id, event_id));
CREATE TABLE metering.usage_rollups (
  id uuid PRIMARY KEY, tenant_id uuid NOT NULL, meter_id uuid NOT NULL, customer_id uuid NOT NULL,
  period_start timestamptz NOT NULL, period_end timestamptz NOT NULL, quantity numeric(20,6) NOT NULL,
  rollup_key text NOT NULL, closed_at timestamptz, UNIQUE (tenant_id, id), UNIQUE (rollup_key),
  FOREIGN KEY (tenant_id, meter_id) REFERENCES metering.meters(tenant_id, id));
CREATE TABLE metering.usage_thresholds (
  id uuid PRIMARY KEY, tenant_id uuid NOT NULL, meter_id uuid NOT NULL, customer_id uuid, quantity numeric(20,6) NOT NULL,
  last_fired_period timestamptz, FOREIGN KEY (tenant_id, meter_id) REFERENCES metering.meters(tenant_id, id));

-- ===== payments =====
CREATE TABLE payments.payments (
  id uuid PRIMARY KEY, tenant_id uuid NOT NULL, customer_id uuid NOT NULL, payment_method_id uuid NOT NULL, invoice_id uuid,
  amount_minor bigint NOT NULL CHECK (amount_minor > 0), currency char(3) NOT NULL,
  status text NOT NULL CHECK (status IN ('PROCESSING','SUCCEEDED','FAILED','REFUNDED','PARTIALLY_REFUNDED')),
  processor_ref text, created_at timestamptz NOT NULL DEFAULT now(), UNIQUE (tenant_id, id),
  FOREIGN KEY (tenant_id, customer_id) REFERENCES customers.customers(tenant_id, id),
  FOREIGN KEY (tenant_id, payment_method_id) REFERENCES customers.payment_methods(tenant_id, id),
  FOREIGN KEY (tenant_id, invoice_id) REFERENCES billing.invoices(tenant_id, id));
CREATE TABLE payments.payment_attempts (
  id uuid PRIMARY KEY, tenant_id uuid NOT NULL, payment_id uuid NOT NULL, attempt_no int NOT NULL,
  outcome text NOT NULL CHECK (outcome IN ('SUCCEEDED','DECLINED','ERROR','TIMEOUT')), failure_code text,
  attempted_at timestamptz NOT NULL DEFAULT now(), UNIQUE (payment_id, attempt_no),
  FOREIGN KEY (tenant_id, payment_id) REFERENCES payments.payments(tenant_id, id));
CREATE TABLE payments.dunning_schedules (
  payment_id uuid PRIMARY KEY, tenant_id uuid NOT NULL, step int NOT NULL, next_attempt_at timestamptz NOT NULL, policy text NOT NULL,
  FOREIGN KEY (tenant_id, payment_id) REFERENCES payments.payments(tenant_id, id));
CREATE TABLE payments.refunds (
  id uuid PRIMARY KEY, tenant_id uuid NOT NULL, payment_id uuid NOT NULL, amount_minor bigint NOT NULL CHECK (amount_minor > 0),
  reason text NOT NULL, status text NOT NULL CHECK (status IN ('PENDING','SUCCEEDED','FAILED')), approval_id uuid,
  created_at timestamptz NOT NULL DEFAULT now(), FOREIGN KEY (tenant_id, payment_id) REFERENCES payments.payments(tenant_id, id));

-- ===== ledger (jOOQ; append-only) =====
CREATE TABLE ledger.accounts (
  id uuid PRIMARY KEY, tenant_id uuid NOT NULL, code text NOT NULL,
  type text NOT NULL CHECK (type IN ('ASSET','LIABILITY','INCOME','EXPENSE','CONTRA_REVENUE')), currency char(3) NOT NULL,
  UNIQUE (tenant_id, id), UNIQUE (tenant_id, code, currency));
CREATE TABLE ledger.journal_entries (
  id uuid PRIMARY KEY, tenant_id uuid NOT NULL, source_type text NOT NULL, source_id uuid NOT NULL,
  description text NOT NULL, reverses_id uuid, posted_at timestamptz NOT NULL DEFAULT now(), UNIQUE (tenant_id, id));
CREATE TABLE ledger.postings (
  id uuid PRIMARY KEY, tenant_id uuid NOT NULL, journal_entry_id uuid NOT NULL, account_id uuid NOT NULL,
  direction text NOT NULL CHECK (direction IN ('DEBIT','CREDIT')), amount_minor bigint NOT NULL CHECK (amount_minor > 0), currency char(3) NOT NULL,
  FOREIGN KEY (tenant_id, journal_entry_id) REFERENCES ledger.journal_entries(tenant_id, id),
  FOREIGN KEY (tenant_id, account_id) REFERENCES ledger.accounts(tenant_id, id));
CREATE FUNCTION ledger.assert_balanced() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
  IF EXISTS (SELECT 1 FROM ledger.postings p WHERE p.journal_entry_id = NEW.journal_entry_id
             GROUP BY p.currency HAVING sum(CASE p.direction WHEN 'DEBIT' THEN p.amount_minor ELSE -p.amount_minor END) <> 0)
     OR (SELECT count(*) FROM ledger.postings p WHERE p.journal_entry_id = NEW.journal_entry_id) < 2 THEN
    RAISE EXCEPTION 'unbalanced journal entry %', NEW.journal_entry_id USING ERRCODE = 'check_violation';
  END IF; RETURN NULL;
END $$;
CREATE CONSTRAINT TRIGGER postings_balanced AFTER INSERT ON ledger.postings
  DEFERRABLE INITIALLY DEFERRED FOR EACH ROW EXECUTE FUNCTION ledger.assert_balanced();
REVOKE UPDATE, DELETE ON ledger.postings, ledger.journal_entries FROM PUBLIC;

-- ===== kernel =====
CREATE TABLE kernel.outbox_events (
  id uuid PRIMARY KEY, tenant_id uuid NOT NULL, topic text NOT NULL, aggregate_type text NOT NULL, aggregate_id text NOT NULL,
  event_type text NOT NULL, payload jsonb NOT NULL, headers jsonb NOT NULL DEFAULT '{}',
  created_at timestamptz NOT NULL DEFAULT now(), published_at timestamptz);
CREATE INDEX outbox_unpublished ON kernel.outbox_events (created_at) WHERE published_at IS NULL;
CREATE TABLE kernel.inbox (consumer text NOT NULL, event_id text NOT NULL, processed_at timestamptz NOT NULL DEFAULT now(), PRIMARY KEY (consumer, event_id));
CREATE TABLE kernel.idempotency_keys (
  tenant_id uuid NOT NULL, key text NOT NULL, request_hash text NOT NULL, status text NOT NULL CHECK (status IN ('IN_PROGRESS','DONE')),
  response_status int, response_body jsonb, expires_at timestamptz NOT NULL, PRIMARY KEY (tenant_id, key));
-- ===== events (core module `events`) =====
CREATE TABLE events.events (
  id text PRIMARY KEY, tenant_id uuid NOT NULL, type text NOT NULL, api_version text NOT NULL, data jsonb NOT NULL,
  created_at timestamptz NOT NULL DEFAULT now());   -- 30-day retention job (FR-EVT-01)
CREATE TABLE kernel.audit_log (
  id uuid PRIMARY KEY, tenant_id uuid NOT NULL, actor_type text NOT NULL CHECK (actor_type IN ('USER','API_KEY','OPERATOR','SYSTEM')),
  actor_id text NOT NULL, on_behalf_of text, action text NOT NULL, target text NOT NULL, at timestamptz NOT NULL DEFAULT now());
CREATE TABLE events.webhook_endpoints (
  id uuid PRIMARY KEY, tenant_id uuid NOT NULL, url text NOT NULL, event_types text[] NOT NULL,
  status text NOT NULL CHECK (status IN ('ENABLED','DEGRADED','CIRCUIT_OPEN','DISABLED')), failure_streak int NOT NULL DEFAULT 0,
  disabled_at timestamptz, version int NOT NULL DEFAULT 1, UNIQUE (tenant_id, id));
CREATE TABLE events.endpoint_secrets (
  id uuid PRIMARY KEY, tenant_id uuid NOT NULL, endpoint_id uuid NOT NULL, secret_enc bytea NOT NULL,
  created_at timestamptz NOT NULL DEFAULT now(), expires_at timestamptz,
  FOREIGN KEY (tenant_id, endpoint_id) REFERENCES events.webhook_endpoints(tenant_id, id));

-- ===== reporting (read models, rebuilt from events) =====
CREATE TABLE reporting.revenue_daily (tenant_id uuid NOT NULL, day date NOT NULL, currency char(3) NOT NULL, amount_minor bigint NOT NULL, PRIMARY KEY (tenant_id, day, currency));
CREATE TABLE reporting.mrr_by_product (tenant_id uuid NOT NULL, product_id uuid NOT NULL, currency char(3) NOT NULL, mrr_minor bigint NOT NULL, subscription_count int NOT NULL, PRIMARY KEY (tenant_id, product_id, currency));
CREATE TABLE reporting.tenant_health (tenant_id uuid PRIMARY KEY, score int NOT NULL CHECK (score BETWEEN 0 AND 100), signals text[] NOT NULL, computed_at timestamptz NOT NULL);

-- ===== ops (operator console; not tenant-scoped by RLS; access via operations module only) =====
CREATE TABLE ops.operators (id uuid PRIMARY KEY, email text NOT NULL UNIQUE, name text NOT NULL, mfa_type text NOT NULL, disabled_at timestamptz);
CREATE TABLE ops.approval_requests (
  id uuid PRIMARY KEY, action text NOT NULL, target_tenant_id uuid, payload jsonb NOT NULL, payload_sha256 char(64) NOT NULL,
  reason text NOT NULL, impact text NOT NULL, requested_by uuid NOT NULL REFERENCES ops.operators(id), decided_by uuid REFERENCES ops.operators(id),
  status text NOT NULL CHECK (status IN ('PENDING','APPROVED','REJECTED','EXPIRED')), expires_at timestamptz NOT NULL,
  created_at timestamptz NOT NULL DEFAULT now(), CHECK (decided_by IS NULL OR decided_by <> requested_by));
CREATE TABLE ops.operator_roles (
  operator_id uuid NOT NULL REFERENCES ops.operators(id), role text NOT NULL CHECK (role IN ('VIEWER','SUPPORT','FINANCE','SRE','PLATFORM_ADMIN')),
  granted_via uuid REFERENCES ops.approval_requests(id), expires_at timestamptz, PRIMARY KEY (operator_id, role));
CREATE TABLE ops.impersonations (
  id uuid PRIMARY KEY, operator_id uuid NOT NULL REFERENCES ops.operators(id), tenant_id uuid NOT NULL, mode text NOT NULL CHECK (mode IN ('READ_ONLY','WRITE')),
  reason text NOT NULL, approval_id uuid REFERENCES ops.approval_requests(id), started_at timestamptz NOT NULL DEFAULT now(),
  expires_at timestamptz NOT NULL, ended_at timestamptz, CHECK (mode = 'READ_ONLY' OR approval_id IS NOT NULL));
CREATE TABLE ops.operator_audit_log (
  seq bigserial PRIMARY KEY, operator_id uuid NOT NULL, action text NOT NULL, kind text NOT NULL CHECK (kind IN ('READ','WRITE','APPROVAL','IMPERSONATION')),
  tenant_id uuid, reason text, request_id text NOT NULL, at timestamptz NOT NULL DEFAULT now(), prev_hash char(64) NOT NULL, hash char(64) NOT NULL);
REVOKE UPDATE, DELETE ON ops.operator_audit_log FROM PUBLIC;
CREATE INDEX operator_audit_tenant ON ops.operator_audit_log (tenant_id, at DESC);
CREATE TABLE ops.recon_runs (id uuid PRIMARY KEY, run_date date NOT NULL UNIQUE, matched_count int NOT NULL, mismatch_count int NOT NULL, status text NOT NULL);
CREATE TABLE ops.recon_mismatches (
  id uuid PRIMARY KEY, run_id uuid NOT NULL REFERENCES ops.recon_runs(id), tenant_id uuid NOT NULL, payment_id uuid,
  kind text NOT NULL CHECK (kind IN ('TIMING','DUPLICATE','MISSING_CAPTURE','AMOUNT')), ledger_amount_minor bigint, processor_amount_minor bigint,
  resolved_by uuid REFERENCES ops.operators(id), note text, resolved_at timestamptz);
CREATE TABLE ops.feature_flags (key text PRIMARY KEY, description text NOT NULL, enabled boolean NOT NULL DEFAULT false, rollout_pct int NOT NULL DEFAULT 0 CHECK (rollout_pct IN (0,10,25,50,100)));
CREATE TABLE ops.flag_targets (flag_key text NOT NULL REFERENCES ops.feature_flags(key), target_type text NOT NULL CHECK (target_type IN ('ALL','PLAN','TIER','TENANT','COHORT')), target_value text NOT NULL, PRIMARY KEY (flag_key, target_type, target_value));
CREATE TABLE ops.rate_limit_overrides (tenant_id uuid PRIMARY KEY, rpm int NOT NULL CHECK (rpm > 0), reason text NOT NULL, set_by uuid NOT NULL REFERENCES ops.operators(id), expires_at timestamptz NOT NULL);
CREATE TABLE ops.job_runs (
  id uuid PRIMARY KEY, job_name text NOT NULL, tenant_id uuid, run_date date NOT NULL, status text NOT NULL CHECK (status IN ('RUNNING','SUCCEEDED','FAILED')),
  duration_ms int, error text, idempotency_key text NOT NULL UNIQUE, started_at timestamptz NOT NULL DEFAULT now());
CREATE TABLE ops.data_exports (id uuid PRIMARY KEY, tenant_id uuid NOT NULL, status text NOT NULL, object_key text, expires_at timestamptz);

-- ===== RLS on every tenant-scoped table (generated in V2 by TLY-102) =====
DO $$ DECLARE t record; BEGIN
  FOR t IN SELECT table_schema, table_name FROM information_schema.columns
           WHERE column_name = 'tenant_id' AND table_schema IN ('tenancy','customers','catalog','billing','metering','payments','ledger','kernel','events','reporting')
             AND table_name NOT IN ('tenants') LOOP
    EXECUTE format('ALTER TABLE %I.%I ENABLE ROW LEVEL SECURITY', t.table_schema, t.table_name);
    EXECUTE format('ALTER TABLE %I.%I FORCE ROW LEVEL SECURITY', t.table_schema, t.table_name);
    EXECUTE format('CREATE POLICY tenant_isolation ON %I.%I USING (tenant_id = kernel.current_tenant()) WITH CHECK (tenant_id = kernel.current_tenant())', t.table_schema, t.table_name);
  END LOOP; END $$;
-- kernel.outbox_events: relay reads through a SECURITY DEFINER function kernel.claim_outbox(batch int) owned by a role
-- with a relay-only policy; documented exception, covered by ADR-008. No other exception exists.

-- ===== hot-path indexes (docs/05 §6) =====
CREATE INDEX invoices_status_due ON billing.invoices (tenant_id, status, due_date);
CREATE INDEX payments_status_created ON payments.payments (tenant_id, status, created_at DESC);
CREATE INDEX subscriptions_status_period ON billing.subscriptions (tenant_id, status, current_period_end);
CREATE INDEX usage_rollups_lookup ON metering.usage_rollups (tenant_id, meter_id, customer_id, period_start);
CREATE INDEX events_type_created ON events.events (tenant_id, type, created_at DESC);
CREATE INDEX postings_account ON ledger.postings (tenant_id, account_id, journal_entry_id);
CREATE INDEX customers_created ON customers.customers (tenant_id, created_at DESC);
