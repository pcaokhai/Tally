package com.tally.core.kernel.tenant;

import java.util.Optional;

/**
 * Bounded, per-request tenant binding (ADR-003). A {@link ScopedValue} is used instead of a {@code
 * ThreadLocal}: its lifetime is exactly the dynamic extent of {@link
 * ScopedValue.Carrier#call(ScopedValue.CallableOp)}, so it cannot leak onto a pooled virtual thread
 * once the request that bound it finishes.
 */
public final class TenantContextHolder {

    static final ScopedValue<TenantContext> TENANT = ScopedValue.newInstance();

    private TenantContextHolder() {}

    /** Empty when no tenant is bound: background jobs and unauthenticated requests must check this. */
    public static Optional<TenantContext> current() {
        return TENANT.isBound() ? Optional.of(TENANT.get()) : Optional.empty();
    }
}
