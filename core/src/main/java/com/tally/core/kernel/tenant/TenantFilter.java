package com.tally.core.kernel.tenant;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Resolves the tenant for the request from the credential only (ADR-003, TLY-102 AC1): the {@code
 * tenant_id} claim of a bearer JWT, or a test-mode API key. Any {@code tenant_id} supplied by the
 * caller in a header, query parameter or body is never read here and therefore has no effect.
 *
 * <p>The JWT path decodes the payload claim without verifying the signature: TLY-103 replaces this
 * with real Keycloak-issued token validation. Until then this filter only establishes the
 * propagation mechanism (ScopedValue bind -> SET LOCAL -> RLS), not authentication itself.
 *
 * <p>The API key path recognizes the placeholder format {@code tly_test_<tenantId>} (ponytail: no
 * `api_keys` table exists yet; TLY-101 replaces this branch with a hashed lookup against it).
 */
public final class TenantFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(TenantFilter.class);

    private static final Pattern TEST_API_KEY = Pattern.compile("tly_test_([0-9a-fA-F-]{36})");

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        Optional<UUID> tenantId = resolveFromBearerJwt(request.getHeader("Authorization"))
                .or(() -> resolveFromApiKey(request.getHeader("X-Tally-Api-Key")));

        if (tenantId.isEmpty()) {
            chain.doFilter(request, response);
            return;
        }

        TenantContext context = new TenantContext(tenantId.get());
        try {
            ScopedValue.where(TenantContextHolder.TENANT, context).call(() -> {
                chain.doFilter(request, response);
                return null;
            });
        } catch (IOException | ServletException | RuntimeException e) {
            throw e;
        } catch (Exception e) {
            // ScopedValue.Carrier#call declares Exception; doFilter only ever throws the two above.
            throw new ServletException(e);
        }
    }

    private Optional<UUID> resolveFromBearerJwt(@Nullable String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.regionMatches(true, 0, "Bearer ", 0, 7)) {
            return Optional.empty();
        }
        String[] parts = authorizationHeader.substring(7).trim().split("\\.");
        if (parts.length != 3) {
            return Optional.empty();
        }
        try {
            byte[] payload = Base64.getUrlDecoder().decode(parts[1]);
            JsonNode claims = objectMapper.readTree(new String(payload, StandardCharsets.UTF_8));
            JsonNode claim = claims.get("tenant_id");
            return claim == null ? Optional.empty() : Optional.of(UUID.fromString(claim.asString()));
        } catch (RuntimeException malformed) {
            log.warn("tenant_claim_unparseable");
            return Optional.empty();
        }
    }

    private Optional<UUID> resolveFromApiKey(@Nullable String apiKeyHeader) {
        if (apiKeyHeader == null) {
            return Optional.empty();
        }
        var matcher = TEST_API_KEY.matcher(apiKeyHeader);
        return matcher.matches() ? Optional.of(UUID.fromString(matcher.group(1))) : Optional.empty();
    }
}
