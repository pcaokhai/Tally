/**
 * Identity and authorization (TLY-103, ADR-007): JWT resource server config, the {@link
 * com.tally.core.kernel.security.AuthenticatedUser} resolved from the token, and the shared {@link
 * com.tally.core.kernel.security.PermissionMatrix} controllers enforce role checks against.
 *
 * <p>{@code @NamedInterface}: every controller module reads {@code AuthenticatedUser} off {@link
 * com.tally.core.kernel.security.AuthenticatedUserContext} to enforce its own role checks, so this
 * subpackage of {@code kernel} is deliberately exposed, not internal.
 */
@NullMarked
@org.springframework.modulith.NamedInterface("security")
package com.tally.core.kernel.security;

import org.jspecify.annotations.NullMarked;
