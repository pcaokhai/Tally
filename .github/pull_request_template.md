## Story

`TLY-nnn` · Lane: PLAT | CORE | WORK | WEB | OPSW · Slice: S<n> or — · Flag: `FF_<SLICE>_<NAME>` or —
Plan: `docs/plans/TLY-nnn.md`

## What and why

<!-- Two or three sentences. Link the doc sections this implements. -->

## Acceptance criteria → tests

| AC | Test(s) |
| --- | --- |
| TLY-nnn-AC1 | `should_…__TLY_nnn_AC1` |

## Checklist

- [ ] Tests written first and failing before the change; `make lint test contracts` output pasted below
- [ ] Tenant isolation: context from credential, RLS on, composite FKs, other-tenant ids return 404
- [ ] Money/ledger/idempotency/unknown-outcome rules honored (docs/10 §0)
- [ ] No contract changes (or this is a `contract/*` PR approved by the tech lead)
- [ ] Logs, metrics, traces added; no secrets, tokens, emails or card-like data in them
- [ ] Docs updated in this PR (behavior, schema, contract, ADR)
- [ ] UI: screenshots/clip in light (web) or dark (ops-web) and with reduced motion; axe clean
- [ ] Migration number reserved and forward-only
- [ ] ≤ 400 changed lines excluding generated code

## Risk and rollback

<!-- What could break, how the flag or a revert limits it. -->

## Verification output

```
<paste real command output>
```
