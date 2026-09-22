package com.tally.core.support;

import com.atlassian.oai.validator.springmvc.OpenApiValidationFilter;
import com.atlassian.oai.validator.springmvc.OpenApiValidationInterceptor;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Validates requests and responses against {@code contracts/openapi.yaml} (TLY-004 AC5). Test scope
 * only: validating every production response against the spec at runtime would cost latency and
 * availability for a guarantee CI already gives.
 */
@TestConfiguration(proxyBeanMethods = false)
public class OpenApiValidationConfig implements WebMvcConfigurer {

    /** Set by the Gradle test tasks; contracts/ sits outside the Gradle project directory. */
    private static final String SPEC_PROPERTY = "tally.contracts.openapi";

    private final OpenApiValidationInterceptor interceptor;

    public OpenApiValidationConfig() throws IOException {
        this.interceptor = new OpenApiValidationInterceptor(
                new EncodedResource(new FileSystemResource(spec()), StandardCharsets.UTF_8));
    }

    @Bean
    OpenApiValidationFilter openApiValidationFilter() {
        return new OpenApiValidationFilter(true, true);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(interceptor);
    }

    /** Fails loudly: a validator that quietly found no spec would make AC5 a lie. */
    private static Path spec() {
        String configured = System.getProperty(SPEC_PROPERTY);
        Path path = configured != null
                ? Path.of(configured)
                : Path.of(System.getProperty("user.dir")).resolve("../contracts/openapi.yaml");
        if (!Files.isRegularFile(path)) {
            throw new IllegalStateException(
                    "OpenAPI spec not found at " + path.toAbsolutePath() + "; set -D" + SPEC_PROPERTY);
        }
        return path.toAbsolutePath().normalize();
    }
}
