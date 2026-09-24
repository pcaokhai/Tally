package com.tally.core.tenancy.domain;

/** Whether a tenant shares the pooled database or runs in its own silo (docs/06 TLY-101 AC2). */
public enum IsolationTier {
    POOL,
    SILO
}
