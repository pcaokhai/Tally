# Tally — top-level commands (root CLAUDE.md §3). Owned by PLAT.
# Targets marked (TLY-xxx) are completed by that story; until then they print what is missing.
SHELL := /bin/bash
FLAGS ?=
.PHONY: up down seed contracts gen test lint fmt e2e load chaos docs-check staging-up ddl-snapshot

up:            ## start the local stack (TLY-002)
	@test -f deploy/compose.yaml || { echo "deploy/compose.yaml not yet created (TLY-002)"; exit 1; }
	$(FLAGS) docker compose -f deploy/compose.yaml up -d --wait
down:
	docker compose -f deploy/compose.yaml down $(ARGS)
seed:          ## load contracts/fixtures (TLY-002)
	@./deploy/seed.sh
contracts:     ## regenerate + validate contracts (TLY-003 adds Spectral/oasdiff)
	python scripts/gen_openapi.py
	python scripts/gen_events.py
	python scripts/gen_webhook_vectors.py
	npx -y @stoplight/spectral-cli@6.16.3 lint --ruleset .spectral.yaml --fail-severity=warn contracts/openapi.yaml contracts/ops-openapi.yaml
	@git diff --quiet -- contracts/ || { echo "contracts/ changed: commit the regenerated files"; exit 1; }
gen:           ## server stubs, clients, mocks from contracts (TLY-003)
	cd core && ./gradlew openApiGenerate
	cd workers && go generate ./...
	cd web && pnpm gen
	cd ops-web && pnpm gen
test:
	cd core && ./gradlew test integrationTest
	cd workers && go test -race ./... && go test -tags=integration ./...
	cd web && pnpm test
	cd ops-web && pnpm test
lint:
	cd core && ./gradlew spotlessCheck archTest
	cd workers && (command -v golangci-lint >/dev/null 2>&1 && golangci-lint run || echo "golangci-lint not installed locally, skipping workers lint (CI always runs it via golangci-lint-action)")
	cd web && pnpm lint && pnpm typecheck
	cd ops-web && pnpm lint && pnpm typecheck
fmt:
	cd core && ./gradlew spotlessApply
	cd workers && gofmt -w . && goimports -w .
	cd web && pnpm prettier --write .
	cd ops-web && pnpm prettier --write .
e2e:           ## Playwright journeys (docs/08 §5)
	cd web && pnpm test:e2e $(ARGS)
load:          ## k6 smoke (TLY-902)
	k6 run deploy/k6/smoke.js
chaos:         ## chaos suite (TLY-903)
	./deploy/chaos/run.sh
staging-up:    ## k3d + Helm (TLY-910)
	./deploy/k3d/up.sh
ddl-snapshot:  ## regenerate docs/assets/ddl from a migrated database
	./deploy/ddl-snapshot.sh
docs-check:
	python scripts/validate_pack.py .
