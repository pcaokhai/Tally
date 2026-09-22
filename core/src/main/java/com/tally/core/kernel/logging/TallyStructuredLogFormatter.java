package com.tally.core.kernel.logging;

import ch.qos.logback.classic.pattern.ThrowableProxyConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.springframework.boot.json.JsonWriter;
import org.springframework.boot.logging.structured.StructuredLogFormatter;
import org.springframework.core.env.Environment;

/**
 * One JSON object per line, with the fields docs/02 §7.6 prescribes: {@code ts, level, msg, service,
 * tenant.id, request_id, trace_id, span_id, actor.type, actor.id, event.id}. Everything after
 * {@code service} comes from the MDC and is omitted when the caller has no value for it — a null
 * field would only cost bytes in the log pipeline.
 *
 * <p>A short allow-list of extra fields follows, which is how per-call detail (HTTP method, status,
 * …) reaches the log while messages stay constant (docs/10 §6). Anything else in the MDC is dropped:
 * the allow-list is the gate that stops a secret, a token or an unmasked email reaching the log
 * because some future module put it in the MDC — see {@link Mask}.
 *
 * <p>Wired through {@code logging.structured.format.console}; Boot instantiates it with the
 * parameters its own formatters take (see {@code StructuredLogEncoder}).
 */
public final class TallyStructuredLogFormatter implements StructuredLogFormatter<ILoggingEvent> {

    /** The docs/02 §7.6 fields carried in the MDC, in the order the document lists them. */
    private static final List<String> MDC_FIELDS =
            List.of("tenant.id", "request_id", "trace_id", "span_id", "actor.type", "actor.id", "event.id");

    /**
     * The only MDC keys emitted beyond the docs/02 §7.6 set. docs/02 §7.6 is the authority on what a
     * Tally log line carries: a new field is added here deliberately, with a reviewer, and never by
     * dropping a key into the MDC and hoping the formatter passes it through.
     */
    private static final List<String> ADDITIONAL_ALLOWED_FIELDS = List.of("http.method", "http.path", "http.status");

    private static final JsonWriter<Map<String, Object>> JSON =
            JsonWriter.<Map<String, Object>>standard().withNewLineAtEnd();

    private final String service;
    private final ThrowableProxyConverter throwableProxyConverter;

    public TallyStructuredLogFormatter(Environment environment, ThrowableProxyConverter throwableProxyConverter) {
        this.service = environment.getProperty("spring.application.name", "tally-core");
        this.throwableProxyConverter = throwableProxyConverter;
    }

    @Override
    public String format(ILoggingEvent event) {
        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("ts", Instant.ofEpochMilli(event.getTimeStamp()).toString());
        fields.put("level", event.getLevel().toString());
        fields.put("msg", event.getFormattedMessage());
        fields.put("service", service);
        addContext(fields, event.getMDCPropertyMap());
        if (event.getThrowableProxy() != null) {
            fields.put("error.type", event.getThrowableProxy().getClassName());
            fields.put("error.stack_trace", throwableProxyConverter.convert(event));
        }
        return JSON.writeToString(fields);
    }

    private static void addContext(Map<String, Object> fields, Map<String, String> mdc) {
        MDC_FIELDS.forEach(field -> putIfPresent(fields, field, mdc.get(field)));
        ADDITIONAL_ALLOWED_FIELDS.forEach(field -> putIfPresent(fields, field, mdc.get(field)));
    }

    private static void putIfPresent(Map<String, Object> fields, String field, @Nullable String value) {
        if (value != null && !value.isBlank()) {
            fields.put(field, value);
        }
    }
}
