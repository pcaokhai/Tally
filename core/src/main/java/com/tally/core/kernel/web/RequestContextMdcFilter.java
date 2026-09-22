package com.tally.core.kernel.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HexFormat;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Puts {@code request_id}, {@code trace_id} and {@code span_id} in the MDC so every line logged
 * while serving a request carries them (docs/02 §7.6), continuing an inbound W3C {@code traceparent}
 * when the caller sent one (ADR-018).
 *
 * <p>This is a stand-in: TLY-901 replaces it with the real OpenTelemetry bridge, which also
 * propagates the context outbound over HTTP and Kafka headers. Until then no tracer exists, so the
 * ids are read or minted here.
 *
 * <p>The MDC is cleared in a {@code finally} block. Virtual threads are on and Tomcat pools its
 * threads, so context left behind would surface on someone else's request.
 */
public final class RequestContextMdcFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RequestContextMdcFilter.class);

    /**
     * {@code version-traceid-spanid-flags}, lowercase hex, per W3C Trace Context. The header is
     * untrusted input: anything that does not match exactly is dropped in favour of generated ids and
     * never echoed into a log field.
     */
    private static final Pattern TRACEPARENT = Pattern.compile("[0-9a-f]{2}-([0-9a-f]{32})-([0-9a-f]{16})-[0-9a-f]{2}");

    private static final String REQUEST_ID = "request_id";
    private static final String TRACE_ID = "trace_id";
    private static final String SPAN_ID = "span_id";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        Matcher traceparent = parse(request.getHeader("traceparent"));
        MDC.put(REQUEST_ID, UUID.randomUUID().toString());
        MDC.put(TRACE_ID, traceparent != null ? traceparent.group(1) : randomHex(2));
        MDC.put(SPAN_ID, traceparent != null ? traceparent.group(2) : randomHex(1));
        try {
            chain.doFilter(request, response);
        } finally {
            logRequest(request, response);
            MDC.remove(REQUEST_ID);
            MDC.remove(TRACE_ID);
            MDC.remove(SPAN_ID);
        }
    }

    /** One INFO line per request; the detail goes in MDC fields so the message stays constant. */
    private static void logRequest(HttpServletRequest request, HttpServletResponse response) {
        MDC.put("http.method", request.getMethod());
        MDC.put("http.path", request.getRequestURI());
        MDC.put("http.status", String.valueOf(response.getStatus()));
        try {
            log.info("http request served");
        } finally {
            MDC.remove("http.method");
            MDC.remove("http.path");
            MDC.remove("http.status");
        }
    }

    private static @Nullable Matcher parse(@Nullable String traceparent) {
        if (traceparent == null) {
            return null;
        }
        Matcher matcher = TRACEPARENT.matcher(traceparent);
        return matcher.matches() ? matcher : null;
    }

    private static String randomHex(int longs) {
        StringBuilder hex = new StringBuilder();
        for (int i = 0; i < longs; i++) {
            hex.append(HexFormat.of().toHexDigits(ThreadLocalRandom.current().nextLong()));
        }
        return hex.toString();
    }
}
