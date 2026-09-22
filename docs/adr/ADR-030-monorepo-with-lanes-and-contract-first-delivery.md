# ADR-030: Monorepo with lanes and contract-first delivery

- **Status:** Accepted
- **Date:** 2026-09-22
- **Deciders:** Tech lead (Phan Cao Khai)
- **Related:** NFR-MNT-04, TLY-001, TLY-003

## Context
Parallel Claude Code sessions need conflict-free ownership and a stable interface to build against.

## Options considered
1. Polyrepo per service — hard isolation / cross-repo changes painful
2. Monorepo, lanes own directories, contracts merged first, slices behind flags — parallel and coherent

## Decision
One repository; lanes PLAT, CORE, WORK, WEB, OPSW own directories (CODEOWNERS); boundary changes start with a contracts PR merged on day 1; slices ship behind `FF_<SLICE>` flags.

## Consequences
Frontends never wait for backends; the tech lead is the bottleneck by design (docs/07 §1).
