/**
 * Tenant context resolution and propagation (ADR-002, ADR-003, TLY-102): {@link
 * com.tally.core.kernel.tenant.TenantFilter} resolves the tenant from the credential and binds
 * {@link com.tally.core.kernel.tenant.TenantContextHolder}; {@link
 * com.tally.core.kernel.tenant.TenantAwareDataSource} issues {@code SET LOCAL app.tenant_id} so
 * row-level security can enforce isolation per transaction.
 */
package com.tally.core.kernel.tenant;
