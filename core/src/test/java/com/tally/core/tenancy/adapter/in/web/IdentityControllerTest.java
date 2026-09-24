package com.tally.core.tenancy.adapter.in.web;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tally.core.kernel.security.AuthenticatedUser;
import com.tally.core.kernel.security.AuthenticatedUserAuthenticationToken;
import com.tally.core.kernel.security.Membership;
import com.tally.core.kernel.security.Role;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Web-layer proof of AC2: {@code GET /v1/me} maps an already-resolved {@link AuthenticatedUser} to
 * the contract's {@code Me} shape. AC1/AC3 (invalid token, tenant not an active membership) are
 * unit-tested directly against {@code TenantClaimAuthenticationConverter} and {@code
 * JwtValidatorFactory} — this slice disables the security filter chain (filters=false) so it can
 * assert on the controller's mapping alone, by pushing an already-authenticated principal.
 */
@WebMvcTest(controllers = IdentityController.class)
@AutoConfigureMockMvc(addFilters = false)
class IdentityControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void should_return_user_active_tenant_and_memberships_when_token_is_valid__TLY_103_AC2() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID activeTenant = UUID.randomUUID();
        UUID otherTenant = UUID.randomUUID();
        AuthenticatedUser user = new AuthenticatedUser(
                userId,
                "owner@example.invalid",
                activeTenant,
                "Acme",
                Role.OWNER,
                List.of(
                        new Membership(activeTenant, "Acme", Role.OWNER),
                        new Membership(otherTenant, "Beta", Role.VIEWER)));

        SecurityContextHolder.getContext().setAuthentication(authenticationToken(user));

        mockMvc.perform(get("/v1/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.id", is(userId.toString())))
                .andExpect(jsonPath("$.user.email", is("owner@example.invalid")))
                .andExpect(jsonPath("$.active_tenant.id", is(activeTenant.toString())))
                .andExpect(jsonPath("$.active_tenant.role", is("OWNER")))
                .andExpect(jsonPath("$.memberships.length()", is(2)));
    }

    private static AuthenticatedUserAuthenticationToken authenticationToken(AuthenticatedUser user) {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .claim("sub", user.userId().toString())
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(60))
                .build();
        return new AuthenticatedUserAuthenticationToken(user, jwt);
    }
}
