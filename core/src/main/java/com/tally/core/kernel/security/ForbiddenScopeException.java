package com.tally.core.kernel.security;

/** Thrown by {@link PermissionEnforcementInterceptor} when a role lacks a required scope. */
public final class ForbiddenScopeException extends RuntimeException {

    public ForbiddenScopeException(Role role, Scope scope) {
        super("role " + role + " does not have scope " + scope);
    }
}
