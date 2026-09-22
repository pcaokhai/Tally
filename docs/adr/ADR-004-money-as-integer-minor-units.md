# ADR-004: Money as integer minor units

- **Status:** Accepted
- **Date:** 2026-09-22
- **Deciders:** Tech lead (Phan Cao Khai)
- **Related:** NFR-COR-04, TLY-201

## Context
Floating point cannot represent cents exactly; BigDecimal invites scale mistakes and is slower; JSON numbers are often parsed as doubles by clients.

## Options considered
1. double — fast / wrong
2. BigDecimal — exact / scale/rounding mistakes, heavier
3. long minor units + ISO currency — exact, fast / needs a currency exponent table

## Decision
`Money(long minor, Currency)` everywhere; JSON `{amount:int, currency}`; database `bigint` + `char(3)`; rounding only inside the pricing engine with HALF_EVEN.

## Consequences
No rounding drift; ArchUnit forbids double/float/BigDecimal in money packages; zero-decimal and three-decimal currencies handled by the exponent table.
