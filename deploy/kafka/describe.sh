#!/usr/bin/env bash
# TLY-002 AC3 — show partitions, retention and cleanup policy of every Tally topic.
set -euo pipefail
cd "$(dirname "$0")/../.."
docker compose -f deploy/compose.yaml exec -T kafka \
  /opt/kafka/bin/kafka-topics.sh --bootstrap-server localhost:29092 --describe | grep PartitionCount
