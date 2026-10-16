#!/usr/bin/env node
// Verifies Tally-Signature per docs/03 §4 against every golden vector.
// Usage: node verify.js [path/to/signature-vectors.json]
const fs = require("fs");
const path = require("path");
const crypto = require("crypto");

function sign(secret, t, body) {
  return crypto.createHmac("sha256", secret).update(`${t}.${body}`).digest("hex");
}

// Real receiver logic: parse `t=<ts>,v1=<hex>[,v1=...]`, reject if now-t > tolerance,
// accept if ANY v1 matches ANY secret currently valid for this receiver (constant-time compare).
function classify(header, rawBody, secrets, now, toleranceSeconds) {
  const parts = header.split(",");
  const t = parseInt(parts[0].slice(2), 10);
  const sigs = parts.slice(1).map((p) => p.slice(3));
  if (now - t > toleranceSeconds) return "REJECT_TIMESTAMP";
  const ok = secrets.some((secret) => {
    const expected = Buffer.from(sign(secret, t, rawBody));
    return sigs.some((sig) => {
      const got = Buffer.from(sig);
      return got.length === expected.length && crypto.timingSafeEqual(got, expected);
    });
  });
  return ok ? "VALID" : "REJECT_SIGNATURE";
}

function main() {
  const file = process.argv[2] || path.join(__dirname, "..", "..", "contracts", "webhooks", "signature-vectors.json");
  const doc = JSON.parse(fs.readFileSync(file, "utf8"));
  for (const v of doc.vectors) {
    const got = classify(v.header, v.raw_body, v.secrets_valid_for_receiver, v.now, doc.tolerance_seconds);
    if (got !== v.expected) {
      console.error(`FAIL ${v.name}: got ${got}, expected ${v.expected}`);
      process.exit(1);
    }
    console.log(`ok ${v.name} -> ${got}`);
  }
  console.log(`${doc.vectors.length} vectors verified (node)`);
}

main();
