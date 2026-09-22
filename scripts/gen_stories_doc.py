#!/usr/bin/env python3
"""Renders docs/06-user-stories.md and scripts/.sprint-table.md from scripts/stories.py."""
import sys
from collections import defaultdict
from pathlib import Path
sys.path.insert(0, str(Path(__file__).parent))
from stories import S
ROOT = Path(__file__).resolve().parents[1]
EPICS = {0: "Platform, contracts and skeletons", 1: "Tenancy, identity and access", 2: "Ledger and money", 3: "Events, outbox and webhooks",
         4: "Catalog and customers", 5: "Subscriptions and invoicing", 6: "Metering and usage", 7: "Payments and dashboard",
         8: "Operator console", 9: "Hardening and enterprise readiness"}
by_epic = defaultdict(list)
for s in S: by_epic[int(s["id"].split("-")[1][0]) if len(s["id"].split("-")[1]) == 3 else 0].append(s)
out = ["# User Stories", "", "Version 1.0 · 2026-09-22 · Owner: Tech lead", "",
 "Stories follow the 3 C's (card, conversation, confirmation) and INVEST. Acceptance criteria are numbered; tests reference them as `TLY-nnn-ACn`. Stories are ≤ 8 points. Frontend stories depend on the contract (merged day 1), never on backend stories. Generated from `scripts/stories.py`; edit there and run `python scripts/gen_stories_doc.py`.", "",
 "Design canvas: https://claude.ai/artifact/2qhb5nQT1s9m9SCuUoqHJu (artboard names are given per UI story).", "",
 "Legend: **Lane** PLAT · CORE · WORK · WEB · OPSW (docs/07 §1) · **pts** story points · **Slice** feature slice or — (docs/07 §2) · **Depends** stories that must be merged first · **Traces** requirement ids (docs/02 §2) and ADRs.", "",
 "Roles: tenant admin (OWNER/ADMIN), tenant developer, finance user, viewer, operator (SUPPORT, FINANCE, SRE, PLATFORM_ADMIN), the platform (system behavior).", ""]
for e in range(10):
    stories = by_epic[e]
    sprints = sorted({s["sprint"] for s in stories})
    rng = f"Sprint {sprints[0]}" if sprints[0] == sprints[-1] else f"Sprint {sprints[0]}–{sprints[-1]}"
    out += [f"## E{e} — {EPICS[e]} ({rng})", ""]
    for s in stories:
        deps = ", ".join(s["deps"]) or "—"
        out += [f"### {s['id']} {s['title']}", f"Lane {s['lane']} · {s['pts']} pts · Slice {s['slice']} · Depends: {deps} · Traces: {s['traces']}", "", s["story"]]
        if s["design"]: out.append(f"Design: canvas artboard(s) `{s['design']}`.")
        out.append("")
        for i, a in enumerate(s["ac"], 1): out.append(f"{i}. {a}")
        out.append("")
(ROOT / "docs" / "06-user-stories.md").write_text("\n".join(out))

# Sprint table with waves
ids = {s["id"]: s for s in S}
rows = []
GOALS = {0: "Walking skeleton: stack up, contracts pipeline, five skeletons", 1: "Tenants exist and are isolated; users sign in", 2: "Safe writes: idempotency, API keys, ledger, outbox",
 3: "Events flow; endpoints registered; team and catalog", 4: "Fair, signed webhook delivery with retries; request logs; settings", 5: "Webhook monitor, events & logs, products UI — R1",
 6: "Customers and the subscription engine", 7: "Invoices drafted, finalized, reminded; usage ingest", 8: "Invoice UI, usage rollups and explorer — R2",
 9: "Payments, dunning, refunds and payments UI", 10: "Home dashboard, reconciliation, operator governance core — R3", 11: "Operator tenants, approvals UI, impersonation API, test clocks",
 12: "Impersonation UI, health API, fleet ops, money ops API, plans/flags API", 13: "Operator health/fleet/jobs, money and config screens, observability — R4",
 14: "Load, chaos and security suites, a11y audit, offboarding", 15: "Silo migration, restore drill, buffer — R5"}
REL = {5: "R1", 8: "R2", 10: "R3", 13: "R4", 15: "R5", 0: "R0"}
for sp in range(16):
    items = [s for s in S if s["sprint"] == sp]
    wave = {}
    def w(s):
        if s["id"] in wave: return wave[s["id"]]
        same = [ids[d] for d in s["deps"] if ids[d]["sprint"] == sp]
        wave[s["id"]] = 1 + max([w(d) for d in same], default=0)
        return wave[s["id"]]
    for s in items: w(s)
    waves = defaultdict(list)
    for s in items: waves[wave[s["id"]]].append(f"{s['lane']} {s['id'][4:]}")
    w1 = " ∥ ".join(waves[1]); rest = " → ".join(" ∥ ".join(waves[k]) for k in sorted(waves) if k > 1) or "—"
    rows.append(f"| {sp} | {GOALS[sp]} | {w1} | {rest} | {sum(s['pts'] for s in items)} | {REL.get(sp, '—')} |")
(ROOT / "scripts" / ".sprint-table.md").write_text("| Sprint | Goal | Wave 1 ∥ | Wave 2+ | Points | Release |\n| --- | --- | --- | --- | --- | --- |\n" + "\n".join(rows))

# Slice table
sl = defaultdict(lambda: {"BE": [], "FE": []})
for s in S:
    if s["slice"] != "—":
        sl[s["slice"]]["FE" if s["lane"] in ("WEB", "OPSW") else "BE"].append(s["id"])
(ROOT / "scripts" / ".slices.json").write_text(__import__("json").dumps(sl, indent=1))
print("06 written;", len(S), "stories; slices:", len(sl))
