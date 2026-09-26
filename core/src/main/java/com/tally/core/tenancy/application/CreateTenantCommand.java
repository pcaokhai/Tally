package com.tally.core.tenancy.application;

/** Input to {@link CreateTenantUseCase} (docs/06 TLY-101 AC1). */
public record CreateTenantCommand(
        String name, String planCode, String ownerEmail, String operatorActor, String idempotencyKey) {}
