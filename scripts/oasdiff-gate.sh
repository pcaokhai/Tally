#!/usr/bin/env bash
# Fails the build on breaking OpenAPI changes vs the main branch baseline.
# Escape hatch: BREAKING_APPROVED=1 (or true) still prints the report but exits 0.
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
OASDIFF_VERSION="v1.32.1"
BIN_DIR="$ROOT/.bin"
SPECS=(contracts/openapi.yaml contracts/ops-openapi.yaml)

OASDIFF=""
if command -v oasdiff >/dev/null 2>&1; then
  OASDIFF="$(command -v oasdiff)"
elif [ -x "$BIN_DIR/oasdiff" ]; then
  OASDIFF="$BIN_DIR/oasdiff"
else
  echo "oasdiff not found; installing ${OASDIFF_VERSION} into $BIN_DIR"
  GOBIN="$BIN_DIR" go install "github.com/oasdiff/oasdiff@${OASDIFF_VERSION}"
  OASDIFF="$BIN_DIR/oasdiff"
fi

BASE_REF=""
if git rev-parse --verify -q origin/main >/dev/null; then
  BASE_REF="origin/main"
elif git rev-parse --verify -q main >/dev/null; then
  BASE_REF="main"
else
  echo "no baseline ref (origin/main or main) found; skipping breaking-change gate"
  exit 0
fi
echo "oasdiff-gate: baseline is $BASE_REF"

TMP_DIR="$(mktemp -d)"
trap 'rm -rf "$TMP_DIR"' EXIT

BREAKING_APPROVED="${BREAKING_APPROVED:-0}"
FAILED=0

for spec in "${SPECS[@]}"; do
  if ! git show "$BASE_REF:$spec" > "$TMP_DIR/base.yaml" 2>/dev/null; then
    echo "oasdiff-gate: $spec does not exist at $BASE_REF; nothing to break, skipping"
    continue
  fi
  echo "oasdiff-gate: checking $spec against $BASE_REF"
  if ! "$OASDIFF" breaking "$TMP_DIR/base.yaml" "$ROOT/$spec" --fail-on ERR; then
    FAILED=1
  fi
done

if [ "$FAILED" -ne 0 ]; then
  if [ "$BREAKING_APPROVED" = "1" ] || [ "$BREAKING_APPROVED" = "true" ]; then
    echo "oasdiff-gate: breaking changes found, but breaking-approved label honoured; passing"
    exit 0
  fi
  echo "oasdiff-gate: breaking API changes detected; add the breaking-approved label or fix the change"
  exit 1
fi

echo "oasdiff-gate: no breaking changes"
