package com.tally.core.support;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

/**
 * Minimal HTTP helper for tests that must talk to a specific port. Boot 4 moved
 * {@code TestRestTemplate} into {@code spring-boot-resttestclient}, which the core does not depend
 * on; the JDK client needs no dependency and does not throw on 4xx.
 */
public final class Http {

    private static final Duration TIMEOUT = Duration.ofSeconds(10);

    private static final HttpClient CLIENT =
            HttpClient.newBuilder().connectTimeout(TIMEOUT).build();

    private Http() {}

    public static HttpResponse<String> get(int port, String path) {
        return get(port, path, Map.of());
    }

    public static HttpResponse<String> get(int port, String path, Map<String, String> headers) {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .timeout(TIMEOUT)
                .GET();
        headers.forEach(builder::header);
        HttpRequest request = builder.build();
        try {
            return CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException e) {
            throw new IllegalStateException("GET " + path + " on port " + port + " failed", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("interrupted during GET " + path, e);
        }
    }

    public static int status(int port, String path) {
        return get(port, path).statusCode();
    }

    public static int status(int port, String path, Map<String, String> headers) {
        return get(port, path, headers).statusCode();
    }

    /**
     * A port free at this instant; good enough to keep tests off the documented fixed ports.
     * Closing the socket before Tomcat binds is a known TOCTOU race, accepted in
     * docs/plans/TLY-004.md R14: asserting three specific distinct ports rules out binding to 0.
     */
    public static int freePort() {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        } catch (IOException e) {
            throw new IllegalStateException("no free port available", e);
        }
    }
}
