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
2. Prerequisites: Docker, JDK 25, Go 1.27, Node LTS + pnpm, Python 3.12 (contract generators).
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

Design canvas: https://claude.ai/artifact/2qhb5nQT1s9m9SCuUoqHJu
