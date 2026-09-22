package com.tally.core.kernel.logging;

import org.jspecify.annotations.Nullable;

/**
 * Masks the two value shapes that must never reach a log, an event or a fixture in clear: email
 * addresses and secrets (API keys, webhook signing secrets, processor tokens). Root CLAUDE.md §6.6,
 * docs/10 §6, NFR-SEC-03, risk R-11.
 *
 * <p>Anything too short to mask safely collapses to {@code ***}: a three-character secret has no
 * prefix worth keeping and revealing it would defeat the point.
 */
public final class Mask {

    private static final String FULLY_MASKED = "***";
    private static final int SECRET_PREFIX = 3;
    private static final int SECRET_SUFFIX = 4;

    private Mask() {}

    /** {@code john.doe@example.com} becomes {@code j***@example.com}; the domain is not a secret. */
    public static String email(@Nullable String email) {
        if (email == null || email.isBlank()) {
            return FULLY_MASKED;
        }
        int at = email.indexOf('@');
        if (at < 1 || at == email.length() - 1) {
            return FULLY_MASKED;
        }
        return email.charAt(0) + FULLY_MASKED + email.substring(at);
    }

    /** {@code sk_live_0123456789abcdef} becomes {@code sk_***cdef}: enough to correlate, not to use. */
    public static String secret(@Nullable String secret) {
        if (secret == null || secret.isBlank() || secret.length() < SECRET_PREFIX + SECRET_SUFFIX + 1) {
            return FULLY_MASKED;
        }
        return secret.substring(0, SECRET_PREFIX) + FULLY_MASKED + secret.substring(secret.length() - SECRET_SUFFIX);
    }
}
