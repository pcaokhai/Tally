# Verifying Tally webhook signatures

Traces: FR-WH-02, ADR-013, TLY-312. See docs/03-webhook-delivery-spec.md §4 for the wire
format. Golden vectors live in `contracts/webhooks/signature-vectors.json`, generated and
self-verified by `scripts/gen_webhook_vectors.py` as part of `make contracts`. **Any change
to the signing rule must regenerate that file first** — a stale checked-in vectors file fails
`make contracts` (and CI).

## The rule

```
Tally-Signature: t=<unix_seconds>,v1=<hex>[,v1=<hex>...]
v1 = hex(HMAC_SHA256(key = UTF-8 bytes of the endpoint secret, message = "<t>.<raw body bytes>"))
```

A receiver MUST:
1. Parse `t` and every `v1` from the header.
2. Reject if `|now - t| > 300` seconds (replay/staleness).
3. Recompute HMAC-SHA256 over `"<t>.<raw body>"` using **the exact raw bytes received**
   (never a re-serialized copy of the parsed JSON) for each secret currently valid for that
   endpoint (there are two during a 24h secret rotation overlap).
4. Accept if any recomputed value matches any `v1` using a constant-time comparison.

The runnable snippets below implement exactly this and are checked against every vector in
`contracts/webhooks/signature-vectors.json` by CI (`webhook-verify-guide` job). Full source:
`scripts/verify_webhooks/`.

## Node.js

```js
const crypto = require("crypto");

function sign(secret, t, body) {
  return crypto.createHmac("sha256", secret).update(`${t}.${body}`).digest("hex");
}

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
```

Run: `node scripts/verify_webhooks/verify.js` (see full file for the CLI wrapper).

## Python

```python
import hmac, hashlib

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
```

Run: `python3 scripts/verify_webhooks/verify.py`.

## Java

```java
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

static String sign(String secret, long t, String body) throws Exception {
    Mac mac = Mac.getInstance("HmacSHA256");
    mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
    byte[] out = mac.doFinal((t + "." + body).getBytes(StandardCharsets.UTF_8));
    StringBuilder sb = new StringBuilder();
    for (byte b : out) sb.append(String.format("%02x", b));
    return sb.toString();
}

static String classify(String header, String rawBody, List<String> secrets, long now, long toleranceSeconds) throws Exception {
    String[] parts = header.split(",");
    long t = Long.parseLong(parts[0].substring(2));
    List<String> sigs = new ArrayList<>();
    for (int i = 1; i < parts.length; i++) sigs.add(parts[i].substring(3));
    if (now - t > toleranceSeconds) return "REJECT_TIMESTAMP";
    for (String secret : secrets) {
        String expected = sign(secret, t, rawBody);
        for (String sig : sigs)
            if (MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8), sig.getBytes(StandardCharsets.UTF_8)))
                return "VALID";
    }
    return "REJECT_SIGNATURE";
}
```

Run: `cd scripts/verify_webhooks && java Verify.java` (single-file execution, JDK 25, no build).

## Go

```go
import (
    "crypto/hmac"
    "crypto/sha256"
    "encoding/hex"
    "fmt"
    "strconv"
    "strings"
)

func sign(secret string, t int64, body string) string {
    mac := hmac.New(sha256.New, []byte(secret))
    mac.Write([]byte(fmt.Sprintf("%d.%s", t, body)))
    return hex.EncodeToString(mac.Sum(nil))
}

func classify(header, rawBody string, secrets []string, now, tolerance int64) (string, error) {
    parts := strings.Split(header, ",")
    t, err := strconv.ParseInt(strings.TrimPrefix(parts[0], "t="), 10, 64)
    if err != nil {
        return "", err
    }
    if now-t > tolerance {
        return "REJECT_TIMESTAMP", nil
    }
    for _, part := range parts[1:] {
        sig := strings.TrimPrefix(part, "v1=")
        for _, secret := range secrets {
            if hmac.Equal([]byte(sign(secret, t, rawBody)), []byte(sig)) {
                return "VALID", nil
            }
        }
    }
    return "REJECT_SIGNATURE", nil
}
```

Run: `cd scripts/verify_webhooks && go run verify.go`.

## Common mistakes

- Re-serializing the parsed JSON before hashing (breaks on whitespace/key-order changes —
  see the `whitespace-changed` vector). Always hash the exact bytes received.
- Comparing signatures with `==`/`.equals()` instead of a constant-time comparison.
- Forgetting the rotation overlap: check **every** `v1` in the header against **every**
  secret currently valid for the endpoint, not just the first pair.
