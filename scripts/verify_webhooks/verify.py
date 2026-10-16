#!/usr/bin/env python3
"""Verifies Tally-Signature per docs/03 §4 against every golden vector.
Usage: python verify.py [path/to/signature-vectors.json]"""
import hmac
import hashlib
import json
import sys
from pathlib import Path


def sign(secret: str, t: int, body: str) -> str:
    return hmac.new(secret.encode(), f"{t}.{body}".encode(), hashlib.sha256).hexdigest()


def classify(header: str, raw_body: str, secrets: list[str], now: int, tolerance_seconds: int) -> str:
    parts = header.split(",")
    t = int(parts[0][2:])
    sigs = [p[3:] for p in parts[1:]]
    if now - t > tolerance_seconds:
        return "REJECT_TIMESTAMP"
    ok = any(hmac.compare_digest(sign(secret, t, raw_body), sig) for secret in secrets for sig in sigs)
    return "VALID" if ok else "REJECT_SIGNATURE"


def main() -> None:
    default = Path(__file__).resolve().parents[2] / "contracts" / "webhooks" / "signature-vectors.json"
    file = Path(sys.argv[1]) if len(sys.argv) > 1 else default
    doc = json.loads(file.read_text())
    for v in doc["vectors"]:
        got = classify(v["header"], v["raw_body"], v["secrets_valid_for_receiver"], v["now"], doc["tolerance_seconds"])
        if got != v["expected"]:
            print(f"FAIL {v['name']}: got {got}, expected {v['expected']}", file=sys.stderr)
            sys.exit(1)
        print(f"ok {v['name']} -> {got}")
    print(f"{len(doc['vectors'])} vectors verified (python)")


if __name__ == "__main__":
    main()
