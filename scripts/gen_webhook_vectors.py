#!/usr/bin/env python3
"""Golden vectors for webhook signing (docs/03 §4). Never hand-compute signatures; run this and commit the output.
Signature: v1 = hex(HMAC_SHA256(key = UTF-8 bytes of the secret string, msg = f"{t}.{raw_body}"))."""
import hmac, hashlib, json
from pathlib import Path
ROOT = Path(__file__).resolve().parents[1]
def sign(secret, t, body): return hmac.new(secret.encode(), f"{t}.{body}".encode(), hashlib.sha256).hexdigest()
body1 = '{"id":"evt_1Qf7a9Kd2","type":"invoice.paid","api_version":"2026-09-01","data":{"invoice_id":"inv_2058"}}'
body2 = '{"id":"evt_1Qf6b2Pz1","type":"payment.failed","api_version":"2026-09-01","data":{"payment_id":"pay_3Qa91","amount":{"amount":125000,"currency":"usd"}}}'
s_old, s_new = "whsec_test_4f1c2b9a7e6d5c3b", "whsec_test_9a8b7c6d5e4f3a2b"
V = []
def vec(name, secrets, t, body, now, expect, note):
    header = f"t={t}," + ",".join(f"v1={sign(s, t, body)}" for s in secrets)
    V.append({"name": name, "secrets_valid_for_receiver": [s_old] if name != "rotation-overlap" else [s_new], "timestamp": t, "now": now,
              "raw_body": body, "header": header, "expected": expect, "note": note})
vec("basic", [s_old], 1790012727, body1, 1790012730, "VALID", "Single signature, within tolerance")
vec("rotation-overlap", [s_new, s_old], 1790012727, body2, 1790012730, "VALID", "During overlap the dispatcher sends one v1 per active secret; receiver with only the new secret still verifies")
vec("replay-too-old", [s_old], 1790012727, body1, 1790013100, "REJECT_TIMESTAMP", "now - t = 373 s > 300 s tolerance")
V.append({**V[0], "name": "tampered-body", "raw_body": body1.replace("inv_2058", "inv_9999"), "expected": "REJECT_SIGNATURE", "note": "Body changed after signing"})
V.append({**V[0], "name": "whitespace-changed", "raw_body": body1.replace(",", ", "), "expected": "REJECT_SIGNATURE", "note": "Receivers must verify the raw bytes, never re-serialized JSON"})
out = {"algorithm": "HMAC-SHA256", "header": "Tally-Signature", "tolerance_seconds": 300, "vectors": V}
(ROOT / "contracts" / "webhooks" / "signature-vectors.json").write_text(json.dumps(out, indent=2) + "\n")
# self-check
for v in V:
    t = v["timestamp"]; sigs = [p[3:] for p in v["header"].split(",") if p.startswith("v1=")]
    ok_sig = any(hmac.compare_digest(sign(s, t, v["raw_body"]), x) for s in v["secrets_valid_for_receiver"] for x in sigs)
    got = "REJECT_TIMESTAMP" if v["now"] - t > 300 else ("VALID" if ok_sig else "REJECT_SIGNATURE")
    assert got == v["expected"], (v["name"], got)
print(len(V), "webhook vectors generated and self-verified")
