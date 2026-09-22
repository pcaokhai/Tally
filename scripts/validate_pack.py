#!/usr/bin/env python3
"""Consistency checker for a gen-tech-docs pack.

Usage: python validate_pack.py <project-dir>

Errors (exit 1): missing required files, unparsable YAML/JSON, stories referenced but not defined,
ADRs referenced but missing, duplicate OpenAPI operationIds.
Warnings: stories not scheduled in the delivery plan, AC numbering gaps, catalogue paths missing
from openapi.yaml, no per-service CLAUDE.md.
"""
import json
import re
import sys
from pathlib import Path

try:
    import yaml
except ImportError:  # pragma: no cover
    yaml = None

REQUIRED = [
    "CLAUDE.md", "README.md", "docs/README.md", "docs/01-prd.md", "docs/02-software-architecture.md",
    "docs/04-api-contract.md", "docs/05-data-model.md", "docs/06-user-stories.md", "docs/07-delivery-plan.md",
    "docs/08-test-strategy.md", "docs/09-risk-register.md", "docs/10-engineering-standards.md",
    "contracts/openapi.yaml", "docs/adr/ADR-000-template.md", "docs/plans/README.md",
    ".github/pull_request_template.md",
]


def main(root: Path) -> int:
    errors, warnings = [], []

    for rel in REQUIRED:
        if not (root / rel).exists():
            errors.append(f"missing required file: {rel}")

    service_claude = [p for p in root.glob("*/CLAUDE.md")]
    if not service_claude:
        warnings.append("no per-service CLAUDE.md found (expected <service>/CLAUDE.md)")

    for p in list(root.rglob("*.yaml")) + list(root.rglob("*.yml")):
        if yaml is None:
            warnings.append("PyYAML not installed; YAML files not parsed")
            break
        try:
            yaml.safe_load(p.read_text())
        except Exception as e:  # noqa: BLE001
            errors.append(f"invalid YAML {p.relative_to(root)}: {e}")
    for p in root.rglob("*.json"):
        try:
            json.loads(p.read_text())
        except Exception as e:  # noqa: BLE001
            errors.append(f"invalid JSON {p.relative_to(root)}: {e}")

    stories_doc = root / "docs/06-user-stories.md"
    plan_doc = root / "docs/07-delivery-plan.md"
    story_ids = {}
    if stories_doc.exists():
        text = stories_doc.read_text()
        blocks = re.split(r"^### ", text, flags=re.M)[1:]
        for block in blocks:
            m = re.match(r"([A-Z][A-Z0-9]+-\d+[a-z]?)\b", block)
            if not m:
                continue
            sid = m.group(1)
            if sid in story_ids:
                errors.append(f"duplicate story id {sid}")
            story_ids[sid] = block
            nums = [int(n) for n in re.findall(r"^(\d+)\. ", block, flags=re.M)]
            if not nums:
                warnings.append(f"{sid}: no numbered acceptance criteria")
            elif nums != list(range(1, len(nums) + 1)):
                warnings.append(f"{sid}: acceptance criteria not numbered 1..n ({nums})")
    key = None
    if story_ids:
        key = next(iter(story_ids)).split("-")[0]

    if key and plan_doc.exists():
        plan = plan_doc.read_text()
        base_ids = {s.rstrip("abcdefghijklmnopqrstuvwxyz") for s in story_ids}
        referenced = set(re.findall(rf"\b{key}-(\d+[a-z]?)\b", plan))
        for num in referenced:
            if f"{key}-{num}" not in story_ids and f"{key}-{num.rstrip('abcdefghijklmnopqrstuvwxyz')}" not in base_ids:
                errors.append(f"delivery plan references undefined story {key}-{num}")
        scheduled_nums = set(re.findall(r"\b(\d{3})[a-z]?\b", plan))
        for sid in story_ids:
            num = sid.split("-")[1].rstrip("abcdefghijklmnopqrstuvwxyz")
            if num not in scheduled_nums:
                warnings.append(f"story {sid} does not appear in the delivery plan")

    adr_files = {p.name[:7] for p in (root / "docs/adr").glob("ADR-*.md")} if (root / "docs/adr").exists() else set()
    for p in root.rglob("*.md"):
        for ref in set(re.findall(r"\bADR-(\d{3})\b", p.read_text())):
            if f"ADR-{ref}" not in adr_files and ref != "000":
                errors.append(f"{p.relative_to(root)} references missing ADR-{ref}")

    oa = root / "contracts/openapi.yaml"
    if yaml and oa.exists():
        try:
            spec = yaml.safe_load(oa.read_text()) or {}
            ops = []
            for path, item in (spec.get("paths") or {}).items():
                for method, op in (item or {}).items():
                    if isinstance(op, dict) and "operationId" in op:
                        ops.append(op["operationId"])
            dups = {o for o in ops if ops.count(o) > 1}
            if dups:
                errors.append(f"duplicate operationIds: {sorted(dups)}")
            api_doc = root / "docs/04-api-contract.md"
            if api_doc.exists():
                paths = set((spec.get("paths") or {}).keys())
                for m in re.finditer(r"^\|\s*[A-Z, ]+\|\s*`([^`]+)`", api_doc.read_text(), flags=re.M):
                    p = m.group(1)
                    if p.startswith("/") and p not in paths:
                        warnings.append(f"API catalogue path not in openapi.yaml: {p}")
        except Exception as e:  # noqa: BLE001
            errors.append(f"cannot analyse openapi.yaml: {e}")

    for w in warnings:
        print(f"WARN  {w}")
    for e in errors:
        print(f"ERROR {e}")
    print(f"\n{len(story_ids)} stories, {len(adr_files)} ADR files, {len(errors)} errors, {len(warnings)} warnings")
    return 1 if errors else 0


if __name__ == "__main__":
    if len(sys.argv) != 2:
        print(__doc__)
        sys.exit(2)
    sys.exit(main(Path(sys.argv[1]).resolve()))
