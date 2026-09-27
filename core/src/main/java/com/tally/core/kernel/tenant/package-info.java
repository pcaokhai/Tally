/**
 * Tenant context resolution and propagation (ADR-002, ADR-003, TLY-102): {@link
 * com.tally.core.kernel.tenant.TenantFilter} resolves the tenant from the credential and binds
 * {@link com.tally.core.kernel.tenant.TenantContextHolder}; {@link
 * com.tally.core.kernel.tenant.TenantAwareDataSource} issues {@code SET LOCAL app.tenant_id} so
 * row-level security can enforce isolation per transaction.
 *
 * <p>{@code @NamedInterface}: TLY-101's provisioning use case binds a tenant id itself (see
 * {@code TenantContextHolder#runWith}), so this package is part of kernel's exposed API, not
 * an internal implementation detail of a single other module.
 */
@org.springframework.modulith.NamedInterface("tenant")
package com.tally.core.kernel.tenant;
