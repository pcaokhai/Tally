-- Tally core database baseline (docs/05 §1, TLY-004 AC3).
-- Pre-epic baseline: extension, roles and schemas only. Tables, RLS policies and indexes
-- arrive from V100 onwards (docs/05 §5 reserves V100-V199 for epic 1).
-- Copied from the header of docs/assets/ddl/tally-baseline.sql.

CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- CREATE ROLE has no IF NOT EXISTS; guard so a reused container cannot break the migration.
DO $$ BEGIN
  IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'tally_app') THEN
    CREATE ROLE tally_app NOLOGIN NOBYPASSRLS;          -- application role; never BYPASSRLS (ADR-002)
  END IF;
  IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'tally_readonly') THEN
    CREATE ROLE tally_readonly NOLOGIN NOBYPASSRLS;     -- replica reads for reporting/ops aggregates
  END IF;
END $$;

CREATE SCHEMA tenancy; CREATE SCHEMA catalog; CREATE SCHEMA customers; CREATE SCHEMA billing;
CREATE SCHEMA metering; CREATE SCHEMA payments; CREATE SCHEMA ledger; CREATE SCHEMA kernel;
CREATE SCHEMA reporting; CREATE SCHEMA ops; CREATE SCHEMA events;

-- Helper: current tenant from SET LOCAL app.tenant_id (fails closed when unset, ADR-003)
CREATE FUNCTION kernel.current_tenant() RETURNS uuid LANGUAGE sql STABLE AS
$$ SELECT nullif(current_setting('app.tenant_id', true), '')::uuid $$;
