# deploy/ — local stack (TLY-002)

```bash
make up              # start everything, wait for health, print resident memory
make seed            # load contracts/fixtures/tenants.json into Keycloak
make down            # stop
make down ARGS=-v    # stop and drop all volumes (clean slate)
```

`make up` blocks until every container health check is green (`--wait --wait-timeout 120`), then
`deploy/stack-report.sh` probes the observability services over HTTP and prints per-container memory,
failing if the total passes 16 GB (NFR-COST-01 / A-01). A cold boot takes ~40 s.

## Services and ports (docs/02 §Ports)

| Service | Host port | Notes |
| --- | --- | --- |
| Postgres 18 | 5432 | databases `tally`, `dispatcher`, `keycloak`; user/password `tally`/`tally` |
| Kafka 4.1 (KRaft) | 9092 | in-network bootstrap is `kafka:29092` |
| Redis 8 | 6379 | |
| Keycloak 26 | 8180 | realms `tally-tenants`, `tally-operators`; admin `admin`/`admin` |
| OTel collector | 4317 / 4318 / 13133 | OTLP gRPC / HTTP / health |
| Prometheus | 9095 | |
| Tempo | 3200 | |
| Loki | 3101 | |
| Grafana | 3300 | anonymous admin, datasources pre-provisioned |

All credentials here are local-only placeholders. Nothing in this directory is a real secret.

**Port already taken?** Every host port is overridable, defaults unchanged:

```bash
TALLY_KAFKA_PORT=19092 TALLY_PG_PORT=15432 make up
```

Variables: `TALLY_PG_PORT`, `TALLY_KAFKA_PORT`, `TALLY_REDIS_PORT`, `TALLY_KEYCLOAK_PORT`,
`TALLY_OTLP_GRPC_PORT`, `TALLY_OTLP_HTTP_PORT`, `TALLY_OTELCOL_METRICS_PORT`,
`TALLY_OTELCOL_HEALTH_PORT`, `TALLY_PROMETHEUS_PORT`, `TALLY_TEMPO_PORT`, `TALLY_LOKI_PORT`,
`TALLY_GRAFANA_PORT`.

## Kafka topics

`deploy/kafka/create-topics.sh` runs as a one-shot container on every `make up` and reconciles the
five topics from docs/04 §7 (24 partitions each; 7 d / 30 d retention; compacted control and
agg-state). `./deploy/kafka/describe.sh` prints what the broker actually has.

## Known gaps

- `make down -v` cannot work: GNU Make consumes `-v` as its own version flag. Use `make down ARGS=-v`
  (plan TLY-002 ruling R2; docs/06 AC5 wording needs the doc fix).
- `make seed` seeds Keycloak only. Tenant rows land in the core once TLY-004 (core) and TLY-101
  (provisioning endpoint) exist; the script already probes the core and reports the skip (ruling R1).
- The OTel collector image is distroless, so it has no container health check — `stack-report.sh`
  probes its `/` health endpoint from the host instead (ruling R3).
- Toxiproxy, k6 and the k3d/Helm staging chart arrive with TLY-902/903/910.
