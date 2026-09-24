package com.tally.core.kernel.security;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Maps {@link ForbiddenScopeException} to {@code application/problem+json} with {@code code:
 * FORBIDDEN_SCOPE} (docs/04-api-contract.md §3, TLY-103 AC4). Scoped to this one exception, not the
 * full error mapper docs/02 §7.5 describes — see docs/plans/TLY-103.md.
 */
@RestControllerAdvice
public final class GlobalSecurityExceptionHandler {

    @ExceptionHandler(ForbiddenScopeException.class)
    ProblemDetail handleForbiddenScope(ForbiddenScopeException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, exception.getMessage());
        problem.setTitle("Role lacks permission");
        problem.setProperty("code", "FORBIDDEN_SCOPE");
        return problem;
    }
}
