package com.tally.core.kernel.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

/**
 * 401 body for every authentication failure on {@code /v1/**} (missing, malformed, unsigned,
 * wrong-issuer/audience or expired token; TLY-103 AC1, AC3) as {@code application/problem+json}
 * with {@code code: UNAUTHENTICATED} (docs/04-api-contract.md §3). Scoped to the two codes this
 * story needs, not the full error mapper docs/02 §7.5 describes — see docs/plans/TLY-103.md.
 */
public final class ProblemJsonAuthenticationEntryPoint implements AuthenticationEntryPoint {

    @Override
    public void commence(
            HttpServletRequest request, HttpServletResponse response, AuthenticationException authException)
            throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/problem+json");
        response.getWriter().write("""
                        {"type":"https://docs.tally.example/problems/unauthenticated",\
                        "title":"Missing or invalid credential","status":401,"code":"UNAUTHENTICATED"}""");
    }
}
