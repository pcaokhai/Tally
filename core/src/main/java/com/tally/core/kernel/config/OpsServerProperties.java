package com.tally.core.kernel.config;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * The operator API port (docs/02 §8, ADR-022). Typed and validated so a bad value fails startup
 * rather than silently binding somewhere unexpected (docs/10 §12).
 *
 * @param port the TCP port the {@code /ops/v1} connector listens on
 */
@Validated
@ConfigurationProperties("tally.ops")
public record OpsServerProperties(@Min(1) @Max(65535) int port) {}
