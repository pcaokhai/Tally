package com.tally.core.kernel.problem;

import org.slf4j.MDC;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * The service's problem+json mapper for known business outcomes (docs/10 §4.2 item 10); never
 * leaks a stack trace or SQL.
 *
 * <p>ponytail: no catch-all {@code Exception.class} handler here — a prior version added one and
 * it swallowed Spring's own 404 handling for unmapped routes (PortsIT caught this: the management
 * port's {@code /v1/me} started returning 500 instead of 404). A generic 5xx mapper is a separate,
 * later concern once there's an actual unhandled-exception story to shape it around.
 */
@RestControllerAdvice
public class GlobalProblemAdvice {

    @ExceptionHandler(ProblemException.class)
    public ProblemDetail handle(ProblemException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(exception.status(), exception.getMessage());
        problem.setTitle(exception.code());
        problem.setProperty("code", exception.code());
        problem.setProperty("param", exception.param());
        problem.setProperty("request_id", MDC.get("request_id"));
        return problem;
    }
}
