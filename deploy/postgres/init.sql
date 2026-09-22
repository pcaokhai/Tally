-- TLY-002 AC1: the local Postgres serves three databases (docs/02 §Ports).
-- `tally` is created by POSTGRES_DB; the other two are created here, owned by the same local dev role.
CREATE DATABASE dispatcher OWNER tally;
CREATE DATABASE keycloak OWNER tally;
