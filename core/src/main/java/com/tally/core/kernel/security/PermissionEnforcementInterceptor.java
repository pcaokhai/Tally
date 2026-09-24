package com.tally.core.kernel.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Enforces {@link RequireScope} against {@link PermissionMatrix} before a handler runs (TLY-103
 * AC4). Runs only for handler methods; static resources etc. are left alone.
 *
 * <p>Relies on the invariant that {@code SecurityConfig} authenticates every {@code /v1/**}
 * request before MVC dispatch, so {@link AuthenticatedUserContext#require()} never sees an
 * unauthenticated request here in practice. If that invariant is ever broken (e.g. a future
 * unauthenticated route carries {@code @RequireScope}), this throws {@code IllegalStateException}
 * (500) rather than silently allowing the call through — fail-closed, just not yet a 401.
 */
public final class PermissionEnforcementInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }
        RequireScope required = handlerMethod.getMethodAnnotation(RequireScope.class);
        if (required == null) {
            return true;
        }
        Role role = AuthenticatedUserContext.require().activeRole();
        if (!PermissionMatrix.allows(role, required.value())) {
            throw new ForbiddenScopeException(role, required.value());
        }
        return true;
    }
}
