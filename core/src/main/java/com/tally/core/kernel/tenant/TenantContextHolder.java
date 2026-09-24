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

    /**
     * Binds {@code context} for the dynamic extent of {@code body} (ADR-003). For requests this
     * happens in {@link TenantFilter} from the credential; provisioning a brand-new tenant
     * (TLY-101) is the one case where a use case binds the id itself, because the caller (an
     * operator) carries no tenant claim but the insert still needs {@code kernel.current_tenant()}
     * to satisfy the new rows' RLS {@code WITH CHECK}.
     */
    public static <T> T runWith(TenantContext context, java.util.concurrent.Callable<T> body) {
        try {
            return ScopedValue.where(TENANT, context).call(body::call);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
