#!/usr/bin/env bash
# TLY-002 AC4 — load contracts/fixtures/tenants.json into Keycloak and (when it is running) the core API.
# Test data only: fixture emails use the reserved .example TLD; the password is a local placeholder.
set -euo pipefail

cd "$(dirname "$0")/.."
FIXTURE=contracts/fixtures/tenants.json
REALM=tally-tenants
SEED_PASSWORD=${SEED_PASSWORD:-tally}
CORE_URL=${TALLY_CORE_URL:-http://localhost:8080}
KC=(docker compose -f deploy/compose.yaml exec -T keycloak /opt/keycloak/bin/kcadm.sh)

[[ -f $FIXTURE ]] || { echo "missing $FIXTURE" >&2; exit 1; }

echo "== Keycloak ($REALM) =="
"${KC[@]}" config credentials --server http://localhost:8180 --realm master \
  --user "${KC_ADMIN:-admin}" --password "${KC_ADMIN_PASSWORD:-admin}" >/dev/null

# tenant_id<TAB>owner_email<TAB>name — read up front: kcadm runs through `docker compose exec`,
# which would otherwise swallow the rest of a piped stdin after the first iteration.
TENANT_ROWS=()   # no mapfile: macOS still ships bash 3.2
while IFS= read -r line; do TENANT_ROWS+=("$line"); done < <(python3 -c "
import json
d=json.load(open('$FIXTURE'))
for t in d['tenants']:
    print('\t'.join([t['id'], t['owner_email'], t['name']]))
")

user_id() { "${KC[@]}" get users -r "$REALM" -q "username=$1" --fields id --format csv --noquotes 2>/dev/null | tr -d '\r\n'; }

for row in "${TENANT_ROWS[@]}"; do
  IFS=$'\t' read -r tid email name <<<"$row"
  id=$(user_id "$email")
  if [[ -z $id ]]; then
    "${KC[@]}" create users -r "$REALM" \
      -s "username=$email" -s "email=$email" -s enabled=true -s emailVerified=true \
      -s "firstName=${name%% *}" -s "lastName=Owner" 2>/dev/null >/dev/null
    id=$(user_id "$email")
    "${KC[@]}" set-password -r "$REALM" --username "$email" --new-password "$SEED_PASSWORD" >/dev/null
    "${KC[@]}" add-roles -r "$REALM" --uusername "$email" --rolename tenant-owner >/dev/null
    action=created
  else
    action=updated
  fi
  # Always (re)apply the mapping so the seed converges instead of skipping. tenant_id is declared
  # in the realm's user profile — Keycloak 26 drops attributes that are not.
  "${KC[@]}" update "users/$id" -r "$REALM" -s "attributes.tenant_id=$tid" >/dev/null
  echo "  $action  $email -> tenant_id=$tid"
done

echo "== core API ($CORE_URL) =="
if curl -fsS --max-time 3 "$CORE_URL/v1/health" >/dev/null 2>&1; then
  # Ruling R1: no tenant-provisioning endpoint exists in contracts/ops-openapi.yaml yet —
  # inventing one here would breach root CLAUDE.md §9. TLY-101 adds provisioning and the
  # POST loop over the fixture's tenants/customers lands with it.
  echo "  core is up, but tenant provisioning arrives with TLY-101 — nothing to POST yet"
else
  echo "  skipped — core not reachable at $CORE_URL (arrives with TLY-004)"
fi
