package com.tally.core.kernel.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.reflect.Method;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.method.HandlerMethod;

class PermissionEnforcementInterceptorTest {

    private final PermissionEnforcementInterceptor interceptor = new PermissionEnforcementInterceptor();

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void should_return_403_forbidden_scope_when_viewer_calls_a_write_endpoint__TLY_103_AC4()
            throws NoSuchMethodException {
        authenticateAs(Role.VIEWER);

        assertThatThrownBy(() -> interceptor.preHandle(
                        new MockHttpServletRequest(), new MockHttpServletResponse(), writeHandlerMethod()))
                .isInstanceOf(ForbiddenScopeException.class);
    }

    @Test
    void should_allow_admin_to_call_a_write_endpoint__TLY_103_AC4() throws NoSuchMethodException {
        authenticateAs(Role.ADMIN);

        boolean proceed = interceptor.preHandle(
                new MockHttpServletRequest(), new MockHttpServletResponse(), writeHandlerMethod());

        assertThat(proceed).isTrue();
    }

    @Test
    void should_skip_handlers_without_the_annotation() throws NoSuchMethodException {
        authenticateAs(Role.VIEWER);
        Method plain = RequireScopeFixture.class.getMethod("readOnly");

        boolean proceed = interceptor.preHandle(
                new MockHttpServletRequest(),
                new MockHttpServletResponse(),
                new HandlerMethod(new RequireScopeFixture(), plain));

        assertThat(proceed).isTrue();
    }

    private static HandlerMethod writeHandlerMethod() throws NoSuchMethodException {
        return new HandlerMethod(new RequireScopeFixture(), RequireScopeFixture.class.getMethod("write"));
    }

    private static void authenticateAs(Role role) {
        AuthenticatedUser user = new AuthenticatedUser(
                UUID.randomUUID(), "user@example.invalid", UUID.randomUUID(), "Acme", role, List.of());
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .claim("sub", "user")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(60))
                .build();
        SecurityContextHolder.getContext().setAuthentication(new AuthenticatedUserAuthenticationToken(user, jwt));
    }

    /** ponytail: a two-method fixture is all `preHandle`'s annotation lookup needs to be proven. */
    static final class RequireScopeFixture {
        @RequireScope(Scope.WRITE)
        public void write() {}

        public void readOnly() {}
    }
}
