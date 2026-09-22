#!/usr/bin/env bash
# TLY-002 AC1 + AC2 — prove the stack is healthy and report its resident memory.
# Container health checks cover Postgres/Kafka/Redis/Keycloak; the observability images are
# distroless or shell-less, so they are probed from the host here (plan ruling R3).
set -euo pipefail

cd "$(dirname "$0")/.."
COMPOSE=(docker compose -f deploy/compose.yaml)
DEADLINE=$((SECONDS + ${HEALTH_TIMEOUT:-120}))
MEM_LIMIT_GB=${MEM_LIMIT_GB:-16}   # NFR-COST-01 / A-01

probes=(
  "postgres|container"
  "kafka|container"
  "redis|container"
  "keycloak|container"
  "otel-collector|http://localhost:${TALLY_OTELCOL_HEALTH_PORT:-13133}/"
  "prometheus|http://localhost:${TALLY_PROMETHEUS_PORT:-9095}/-/healthy"
  "tempo|http://localhost:${TALLY_TEMPO_PORT:-3200}/ready"
  "loki|http://localhost:${TALLY_LOKI_PORT:-3101}/ready"
  "grafana|http://localhost:${TALLY_GRAFANA_PORT:-3300}/api/health"
)

check() { # name url -> 0 when healthy
  local name=$1 url=$2
  if [[ $url == container ]]; then
    [[ "$("${COMPOSE[@]}" ps --format '{{.Health}}' "$name" 2>/dev/null)" == healthy ]]
  else
    curl -fsS --max-time 3 "$url" >/dev/null 2>&1
  fi
}

echo "== health =="
pending=("${probes[@]}")
while ((${#pending[@]})); do
  remaining=()
  for p in "${pending[@]}"; do
    if check "${p%%|*}" "${p#*|}"; then printf '  %-16s healthy\n' "${p%%|*}"; else remaining+=("$p"); fi
  done
  pending=("${remaining[@]+"${remaining[@]}"}")
  ((${#pending[@]} == 0)) && break
  if ((SECONDS > DEADLINE)); then
    printf '  %-16s NOT HEALTHY\n' "${pending[@]%%|*}"
    echo "FAILED: stack not healthy within ${HEALTH_TIMEOUT:-120}s (AC1)" >&2
    exit 1
  fi
  sleep 3
done

echo "== memory (AC2: total <= ${MEM_LIMIT_GB} GB) =="
ids=$("${COMPOSE[@]}" ps -q)
[[ -n $ids ]] || { echo "no running containers" >&2; exit 1; }
# shellcheck disable=SC2086
docker stats --no-stream --format '{{.Name}}\t{{.MemUsage}}' $ids | sed 's/^/  /'
total_bytes=$(docker stats --no-stream --format '{{.MemUsage}}' $ids | awk -F' / ' '
  {
    v = $1; u = v; sub(/[0-9.]+/, "", u); sub(/[A-Za-z]+$/, "", v)
    m = (u == "GiB") ? 1073741824 : (u == "MiB") ? 1048576 : (u == "KiB") ? 1024 : 1
    s += v * m
  } END {printf "%d", s}')
awk -v b="$total_bytes" -v lim="$MEM_LIMIT_GB" '
  BEGIN {
    gb = b / 1073741824
    printf "  TOTAL            %.2f GiB (limit %d GB)\n", gb, lim
    if (gb > lim) {printf "FAILED: stack uses %.2f GiB > %d GB (AC2, NFR-COST-01)\n", gb, lim > "/dev/stderr"; exit 1}
  }'
echo "stack ready"
