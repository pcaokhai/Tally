package com.tally.core.tenancy.application;

import com.tally.core.tenancy.domain.IsolationTier;
import java.util.UUID;

/** Output of {@link CreateTenantUseCase}; also the shape stored for idempotent replay. */
public record CreateTenantResult(UUID id, String name, String planCode, IsolationTier tier, String status) {}
