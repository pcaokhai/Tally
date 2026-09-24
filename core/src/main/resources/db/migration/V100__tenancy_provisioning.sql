-- TLY-101: tenant provisioning and plan versions (docs/05 §1-§5, ADR-028, TLY-102's RLS convention).
-- Reserves V100, the first real number in the V100-V199 range docs/05 §5 sets aside for epic 1.

-- Platform data (ADR-028): owned by operators, not tenant-scoped, no RLS.
CREATE TABLE tenancy.plan_versions (
  id uuid NOT NULL PRIMARY KEY,
  plan_code text NOT NULL,
  version int NOT NULL,
  limits jsonb NOT NULL DEFAULT '{}'::jsonb,
  created_at timestamptz NOT NULL DEFAULT now(),
  UNIQUE (plan_code, version));

GRANT USAGE ON SCHEMA tenancy TO tally_app;
GRANT SELECT ON tenancy.plan_versions TO tally_app;

INSERT INTO tenancy.plan_versions (id, plan_code, version, limits) VALUES
  (gen_random_uuid(), 'starter', 1, '{"rate_limit_rpm": 60}'::jsonb),
  (gen_random_uuid(), 'growth', 1, '{"rate_limit_rpm": 300}'::jsonb),
  (gen_random_uuid(), 'scale', 1, '{"rate_limit_rpm": 1000, "isolation_tier": "SILO"}'::jsonb);

-- The tenant registry itself: it *is* the tenant root, so it carries no tenant_id and no RLS.
CREATE TABLE tenancy.tenants (
  id uuid NOT NULL PRIMARY KEY,
  name text NOT NULL,
  plan_version_id uuid NOT NULL REFERENCES tenancy.plan_versions(id),
  isolation_tier text NOT NULL CHECK (isolation_tier IN ('POOL', 'SILO')),
  status text NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'PAST_DUE', 'CANCELING', 'SUSPENDED', 'SUSPEND_PENDING')),
  created_at timestamptz NOT NULL DEFAULT now(),
  UNIQUE (id));

GRANT SELECT, INSERT, UPDATE ON tenancy.tenants TO tally_app;

-- Tenant-scoped tables from here: follow TLY-102's convention verbatim.
CREATE TABLE tenancy.invitations (
  tenant_id uuid NOT NULL,
  id uuid NOT NULL,
  email text NOT NULL,
  role text NOT NULL,
  status text NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'ACCEPTED', 'EXPIRED')),
  created_at timestamptz NOT NULL DEFAULT now(),
  PRIMARY KEY (tenant_id, id));

ALTER TABLE tenancy.invitations
  ADD FOREIGN KEY (tenant_id) REFERENCES tenancy.tenants(id);

ALTER TABLE tenancy.invitations ENABLE ROW LEVEL SECURITY;
ALTER TABLE tenancy.invitations FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON tenancy.invitations
  USING (tenant_id = kernel.current_tenant())
  WITH CHECK (tenant_id = kernel.current_tenant());
GRANT SELECT, INSERT, UPDATE, DELETE ON tenancy.invitations TO tally_app;

CREATE TABLE billing.invoice_number_seqs (
  tenant_id uuid NOT NULL PRIMARY KEY,
  next_number bigint NOT NULL DEFAULT 1);

ALTER TABLE billing.invoice_number_seqs
  ADD FOREIGN KEY (tenant_id) REFERENCES tenancy.tenants(id);

ALTER TABLE billing.invoice_number_seqs ENABLE ROW LEVEL SECURITY;
ALTER TABLE billing.invoice_number_seqs FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON billing.invoice_number_seqs
  USING (tenant_id = kernel.current_tenant())
  WITH CHECK (tenant_id = kernel.current_tenant());
GRANT USAGE ON SCHEMA billing TO tally_app;
GRANT SELECT, INSERT, UPDATE, DELETE ON billing.invoice_number_seqs TO tally_app;

-- Default chart of accounts (TLY-202 owns postings/journal_entries; this story only needs the
-- per-tenant account rows to exist so provisioning has something to create).
CREATE TABLE ledger.accounts (
  tenant_id uuid NOT NULL,
  id uuid NOT NULL,
  code text NOT NULL,
  name text NOT NULL,
  type text NOT NULL CHECK (type IN ('ASSET', 'LIABILITY', 'EQUITY', 'REVENUE', 'EXPENSE')),
  created_at timestamptz NOT NULL DEFAULT now(),
  PRIMARY KEY (tenant_id, id),
  UNIQUE (tenant_id, code));

ALTER TABLE ledger.accounts
  ADD FOREIGN KEY (tenant_id) REFERENCES tenancy.tenants(id);

ALTER TABLE ledger.accounts ENABLE ROW LEVEL SECURITY;
ALTER TABLE ledger.accounts FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON ledger.accounts
  USING (tenant_id = kernel.current_tenant())
  WITH CHECK (tenant_id = kernel.current_tenant());
GRANT USAGE ON SCHEMA ledger TO tally_app;
GRANT SELECT, INSERT, UPDATE, DELETE ON ledger.accounts TO tally_app;

-- Minimal domain-event audit for AC4 (ponytail: kernel.audit_log, not the full ops audit writer —
-- see docs/plans/TLY-101.md Ruling 2). tenant_id is nullable for pure platform actions, so the
-- policy also lets those rows through (TenantRlsConventionIT's generic coverage query has no
-- carve-out for "operator-only" tables — every table with a tenant_id column gets RLS, no
-- exceptions).
CREATE TABLE kernel.audit_log (
  id uuid NOT NULL PRIMARY KEY,
  tenant_id uuid NULL REFERENCES tenancy.tenants(id),
  actor text NOT NULL,
  action text NOT NULL,
  payload jsonb NOT NULL DEFAULT '{}'::jsonb,
  created_at timestamptz NOT NULL DEFAULT now());

ALTER TABLE kernel.audit_log ENABLE ROW LEVEL SECURITY;
ALTER TABLE kernel.audit_log FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON kernel.audit_log
  USING (tenant_id = kernel.current_tenant() OR tenant_id IS NULL)
  WITH CHECK (tenant_id = kernel.current_tenant() OR tenant_id IS NULL);
GRANT SELECT, INSERT ON kernel.audit_log TO tally_app;

-- Minimal idempotency store for this endpoint only (ponytail: see docs/plans/TLY-101.md Ruling 1;
-- TLY-110 generalizes this into a shared filter for every money-moving/creating write).
CREATE TABLE kernel.idempotency_keys (
  key text NOT NULL PRIMARY KEY,
  response jsonb NOT NULL,
  created_at timestamptz NOT NULL DEFAULT now());

GRANT SELECT, INSERT ON kernel.idempotency_keys TO tally_app;
