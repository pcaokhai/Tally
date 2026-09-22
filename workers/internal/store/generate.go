// Package store will hold sqlc-generated dispatcher repositories once
// migrations exist (ADR-016: dispatcher schema arrives with the
// webhook-delivery stories, not with PLAT/TLY-003).
package store

//go:generate sh -c "find ../../migrations/schema -name '*.sql' 2>/dev/null | grep -q . && sqlc generate -f ../../sqlc.yaml || echo 'sqlc: no schema files in workers/migrations/schema yet — dispatcher migrations arrive with the webhook-delivery stories; skipping'"
