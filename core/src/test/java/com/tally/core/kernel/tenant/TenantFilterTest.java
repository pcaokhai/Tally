package com.tally.core.kernel.tenant;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.servlet.FilterChain;
import java.util.Base64;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class TenantFilterTest {

    private final TenantFilter filter = new TenantFilter();

    @Test
    void should_bind_tenant_from_jwt_claim_when_bearer_token_carries_it__TLY_102_AC1() throws Exception {
        UUID tenantId = UUID.randomUUID();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + fakeJwt(tenantId));
        AtomicReference<UUID> seen = new AtomicReference<>();

        filter.doFilter(request, new MockHttpServletResponse(), captureTenant(seen));

        assertThat(seen.get()).isEqualTo(tenantId);
    }

    @Test
    void should_bind_tenant_from_api_key_when_no_bearer_token_present__TLY_102_AC1() throws Exception {
        UUID tenantId = UUID.randomUUID();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Tally-Api-Key", "tly_test_" + tenantId);
        AtomicReference<UUID> seen = new AtomicReference<>();

        filter.doFilter(request, new MockHttpServletResponse(), captureTenant(seen));

        assertThat(seen.get()).isEqualTo(tenantId);
    }

    @Test
    void should_ignore_tenant_id_supplied_in_headers_query_or_body__TLY_102_AC1() throws Exception {
        UUID claimedTenant = UUID.randomUUID();
        UUID spoofedTenant = UUID.randomUUID();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + fakeJwt(claimedTenant));
        request.addHeader("X-Tenant-Id", spoofedTenant.toString());
        request.setParameter("tenant_id", spoofedTenant.toString());
        AtomicReference<UUID> seen = new AtomicReference<>();

        filter.doFilter(request, new MockHttpServletResponse(), captureTenant(seen));

        assertThat(seen.get()).isEqualTo(claimedTenant).isNotEqualTo(spoofedTenant);
    }

    @Test
    void should_leave_no_tenant_bound_when_no_credential_is_present__TLY_102_AC1() throws Exception {
        AtomicReference<Boolean> boundInsideRequest = new AtomicReference<>();

        filter.doFilter(
                new MockHttpServletRequest(),
                new MockHttpServletResponse(),
                (req, res) ->
                        boundInsideRequest.set(TenantContextHolder.current().isPresent()));

        assertThat(boundInsideRequest.get()).isFalse();
    }

    private static FilterChain captureTenant(AtomicReference<UUID> seen) {
        return (req, res) -> seen.set(
                TenantContextHolder.current().map(TenantContext::tenantId).orElse(null));
    }

    private static String fakeJwt(UUID tenantId) {
        String header = encode("{\"alg\":\"none\"}");
        String payload = encode("{\"tenant_id\":\"" + tenantId + "\"}");
        return header + "." + payload + "." + "sig";
    }

    private static String encode(String json) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(json.getBytes());
    }
}
