// Package store will hold sqlc-generated dispatcher repositories once
// migrations exist (ADR-016: dispatcher schema arrives with the
// webhook-delivery stories, not with PLAT/TLY-003).
package store

//go:generate sh -c "if find ../../migrations/schema -name '*.sql' 2>/dev/null | grep -q .; then go run github.com/sqlc-dev/sqlc/cmd/sqlc@v1.30.0 generate -f ../../sqlc.yaml; else echo 'sqlc: no schema files in workers/migrations/schema yet — dispatcher migrations arrive with the webhook-delivery stories; skipping'; fi"
