package com.tally.core.kernel.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Keeps the tenant API and the operator API on separate ports (ADR-022): {@code /ops/v1} is only
 * reachable on the operator port, and everything else is only reachable off it. A wrong-port request
 * is answered 404, never 403, so the split leaks nothing about what the other port serves.
 *
 * <p>TLY-103 replaces this with two real {@code SecurityFilterChain}s bound to the tenant and
 * operator realms; until authentication exists there is nothing for a security chain to enforce.
 */
public final class PortScopeFilter extends OncePerRequestFilter {

    private static final String OPS_PREFIX = "/ops/";

    private final int opsPort;

    public PortScopeFilter(int opsPort) {
        this.opsPort = opsPort;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        boolean operatorPath = request.getRequestURI().startsWith(OPS_PREFIX);
        boolean operatorPort = request.getLocalPort() == opsPort;
        if (operatorPath != operatorPort) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        chain.doFilter(request, response);
    }
}
