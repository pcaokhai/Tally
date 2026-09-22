# ADR-029: Runtime stack: Java 25 LTS, Spring Boot 4, Go 1.27

- **Status:** Accepted
- **Date:** 2026-09-22
- **Deciders:** Tech lead (Phan Cao Khai)
- **Related:** Constraint in docs/02 §2.3, TLY-004, TLY-005

## Context
The owner requires Java 25 and the latest Go. Java 25 is the current LTS; Go has no LTS and supports the two latest releases, of which 1.27 (August 2026) is the newest.

## Options considered
1. Java 21 + Spring Boot 3 — mature / older, Boot 3 OSS support ending
2. Java 25 + Spring Boot 4 + Go 1.27 — current LTS, final ScopedValue, virtual threads without pinning, Go generic methods / newer ecosystem

## Decision
Core on Java 25 with Spring Boot 4.x (Framework 7, Jackson 3, Spring Modulith); no preview features. Workers on Go 1.27. Exact library versions pinned in TLY-001/004/005 and upgraded by Renovate.

## Consequences
Modern concurrency primitives available; some libraries may lag Jackson 3/Boot 4 (risk R-10); Go 1.28 upgrade planned when released.
