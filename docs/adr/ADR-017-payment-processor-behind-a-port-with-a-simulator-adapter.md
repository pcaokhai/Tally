# ADR-017: Payment processor behind a port with a simulator adapter

- **Status:** Accepted
- **Date:** 2026-09-22
- **Deciders:** Tech lead (Phan Cao Khai)
- **Related:** FR-PAY-02, TLY-701

## Context
Real processors cannot produce every failure on demand; vendor lock-in hurts tests and design.

## Options considered
1. Call Stripe SDK directly — fast start / untestable failures
2. Port with Stripe test and simulator adapters sharing a contract test — testable / adapter upkeep

## Decision
`PaymentProcessor` port with `StripeTestProcessor` and `SimulatorProcessor`; simulator behaviors keyed by token; unknown outcomes are first-class.

## Consequences
Every failure path is testable offline; adapters must stay contract-compatible.
