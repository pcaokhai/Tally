# Tally

Multi-tenant payments and billing platform: subscriptions, usage-based pricing, invoices, payments, a double-entry ledger, fair signed webhooks, and an operator console, built to production standards as a learning project.

| Part | Tech | Docs |
| --- | --- | --- |
| `core/` | Java 25 · Spring Boot 4 · Spring Modulith · PostgreSQL (RLS) · Kafka | core/CLAUDE.md |
| `workers/` | Go 1.27 · dispatcher (webhooks) · aggregator (usage) | workers/CLAUDE.md |
| `web/` | Next.js tenant dashboard + BFF | web/CLAUDE.md |
| `ops-web/` | Next.js operator console | ops-web/CLAUDE.md |
| `contracts/` | OpenAPI 3.1, JSON Schema events, webhook vectors, fixtures | docs/04 |

## Start here

1. Read `CLAUDE.md`, then `docs/README.md` for the reading order.
2. Prerequisites: Docker, JDK 25, Go 1.27, Node LTS + pnpm, Python 3.12 (contract generators). Optional: golangci-lint (`make lint` skips the `workers/` check with a message if it's absent; CI always runs it).
3. `make up && make seed` (available after TLY-001..003, Sprint 0).
4. Sprint plan: `docs/07-delivery-plan.md`. Stories: `docs/06-user-stories.md`.

## Regenerating contracts and generated docs

```
python scripts/gen_openapi.py          # contracts/openapi.yaml, contracts/ops-openapi.yaml
python scripts/gen_events.py           # contracts/events/*.schema.json + validated fixtures
python scripts/gen_webhook_vectors.py  # contracts/webhooks/signature-vectors.json
python scripts/gen_stories_doc.py      # docs/06 + sprint/slice tables
python scripts/gen_adrs.py             # docs/adr/
python scripts/validate_pack.py .      # consistency check (make docs-check)
```

## Contract pipeline (`make contracts` / `make gen`)

`contracts/` (OpenAPI 3.1 + JSON Schema events + webhook vectors) is the single source of truth for every service boundary. Never hand-edit it or any generated code.

- `make contracts` regenerates `contracts/` from the generator scripts, lints it with Spectral, and runs an oasdiff breaking-change gate against `origin/main`. If the gate finds a breaking change it fails the build unless `BREAKING_APPROVED=1` is set (CI sets this only when the PR carries the `breaking-approved` label) — that is the escape hatch for an intentional breaking contract change, and it still prints the full breaking-change report so reviewers see what was approved. It also fails if the regenerated `contracts/` files differ from what's committed, so a contract change and its regenerated output always land in the same commit.
- `make gen` regenerates the per-language artifacts from `contracts/`: Spring interfaces + models in `core` (via `openApiGenerate`, which `compileJava` depends on — so a contract change that a handler doesn't yet match fails the Java build, not just a lint), Go types in `workers`, and typed clients + MSW mocks in `web`/`ops-web`.

See `docs/plans/TLY-003.md` for a worked example of a breaking rename walking through both gates.

Design canvas: https://claude.ai/artifact/2qhb5nQT1s9m9SCuUoqHJu
