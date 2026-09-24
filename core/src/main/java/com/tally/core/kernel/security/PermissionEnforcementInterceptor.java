package com.tally.core.kernel.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Enforces {@link RequireScope} against {@link PermissionMatrix} before a handler runs (TLY-103
 * AC4). Runs only for handler methods; static resources etc. are left alone.
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
