#!/usr/bin/env bash
# TLY-002 AC3 — topics from docs/04 §7. Idempotent: creates what is missing, reconciles configs.
set -euo pipefail

BOOTSTRAP="${BOOTSTRAP:-kafka:29092}"
TOPICS_SH=/opt/kafka/bin/kafka-topics.sh
CONFIGS_SH=/opt/kafka/bin/kafka-configs.sh
PARTITIONS=24            # docs/04 §7: 24 partitions per topic locally
WEEK_MS=604800000        # 7 days
MONTH_MS=2592000000      # 30 days

# topic|config
TOPICS=(
  "tally.billing.events.v1|cleanup.policy=delete,retention.ms=${WEEK_MS}"
  "tally.usage.raw.v1|cleanup.policy=delete,retention.ms=${WEEK_MS}"
  "tally.usage.rollups.v1|cleanup.policy=delete,retention.ms=${MONTH_MS}"
  "tally.webhooks.control.v1|cleanup.policy=compact"
  "tally.usage.agg-state.v1|cleanup.policy=compact"
)

for entry in "${TOPICS[@]}"; do
  topic="${entry%%|*}"
  configs="${entry#*|}"
  create_args=()
  IFS=',' read -ra kvs <<<"$configs"
  for kv in "${kvs[@]}"; do create_args+=(--config "$kv"); done

  "$TOPICS_SH" --bootstrap-server "$BOOTSTRAP" --create --if-not-exists \
    --topic "$topic" --partitions "$PARTITIONS" --replication-factor 1 "${create_args[@]}"

  # Reconcile an existing topic that was created with different settings.
  "$CONFIGS_SH" --bootstrap-server "$BOOTSTRAP" --entity-type topics --entity-name "$topic" \
    --alter --add-config "$configs" >/dev/null
  echo "ok $topic ($PARTITIONS partitions, $configs)"
done

echo "topics ready on $BOOTSTRAP"
