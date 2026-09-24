package com.tally.core.kernel.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a controller handler method as requiring {@link Scope#WRITE} (or, rarely, an explicit
 * {@link Scope#READ}) under {@link PermissionMatrix}. Enforced by {@link
 * PermissionEnforcementInterceptor} against the caller's {@link AuthenticatedUser#activeRole()}.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface RequireScope {
    Scope value();
}
