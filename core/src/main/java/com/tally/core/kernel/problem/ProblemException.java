package com.tally.core.kernel.problem;

import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpStatus;

/**
 * A business outcome carried as a value up to the web layer, mapped to the contracts' {@code
 * Problem} schema by {@link GlobalProblemAdvice} (docs/10 §4.2 item 10: business outcomes are
 * values, never bare exceptions with leaked internals).
 */
public class ProblemException extends RuntimeException {

    private final HttpStatus status;
    private final String code;
    private final @Nullable String param;

    public ProblemException(HttpStatus status, String code, String detail, @Nullable String param) {
        super(detail);
        this.status = status;
        this.code = code;
        this.param = param;
    }

    public static ProblemException validationFailed(String param, String detail) {
        return new ProblemException(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", detail, param);
    }

    public HttpStatus status() {
        return status;
    }

    public String code() {
        return code;
    }

    public @Nullable String param() {
        return param;
    }
}
