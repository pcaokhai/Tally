package com.tally.core.support;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

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
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .timeout(TIMEOUT)
                .GET()
                .build();
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

    /** A port free at this instant; good enough to keep tests off the documented fixed ports. */
    public static int freePort() {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        } catch (IOException e) {
            throw new IllegalStateException("no free port available", e);
        }
    }
}
