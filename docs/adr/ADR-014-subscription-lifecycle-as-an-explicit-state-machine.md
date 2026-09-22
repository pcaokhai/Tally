# ADR-014: Subscription lifecycle as an explicit state machine

- **Status:** Accepted
- **Date:** 2026-09-22
- **Deciders:** Tech lead (Phan Cao Khai)
- **Related:** FR-SUB-02, TLY-501

## Context
Status flags scattered in code produce impossible states (canceled and past due).

## Options considered
1. Boolean flags — quick / illegal combinations
2. Explicit state machine with a transition table — testable / slightly more code

## Decision
`SubscriptionStateMachine` owns all transitions (TRIALING, ACTIVE, PAST_DUE, CANCELED); illegal transitions raise `IllegalTransition` (409); each transition emits an event.

## Consequences
Exhaustive table-driven tests; new states require an ADR amendment.
