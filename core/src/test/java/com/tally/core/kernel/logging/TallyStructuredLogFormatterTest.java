package com.tally.core.kernel.logging;

import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.pattern.ThrowableProxyConverter;
import ch.qos.logback.classic.spi.LoggingEvent;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.json.JsonParserFactory;
import org.springframework.mock.env.MockEnvironment;

class TallyStructuredLogFormatterTest {

    private static final Map<String, String> FULL_MDC = Map.of(
            "tenant.id", "tnt_123",
            "request_id", "req_456",
            "trace_id", "4bf92f3577b34da6a3ce929d0e0e4736",
            "span_id", "00f067aa0ba902b7",
            "actor.type", "user",
            "actor.id", "usr_789",
            "event.id", "evt_012");

    @Test
    void should_emit_service_and_trace_id_when_formatting_an_event__TLY_004_AC4() {
        Map<String, Object> json = format(event(FULL_MDC));

        assertThat(json).containsEntry("service", "tally-core");
        assertThat(json).containsEntry("trace_id", "4bf92f3577b34da6a3ce929d0e0e4736");
        assertThat(json).containsEntry("level", "INFO");
        assertThat(json).containsEntry("msg", "invoice finalized");
        assertThat(json).containsKey("ts");
    }

    @Test
    void should_emit_every_documented_field_when_the_mdc_carries_them__TLY_004_AC4() {
        Map<String, Object> json = format(event(FULL_MDC));

        assertThat(json).containsAllEntriesOf(FULL_MDC);
    }

    @Test
    void should_omit_absent_fields_when_the_mdc_is_empty__TLY_004_AC4() {
        Map<String, Object> json = format(event(Map.of()));

        assertThat(json).containsOnlyKeys("ts", "level", "msg", "service");
    }

    @Test
    void should_drop_mdc_keys_outside_the_allow_list_when_formatting_an_event__TLY_004_AC4() {
        // A field nobody allow-listed, holding something that must never reach a log. The fake value
        // proves the gate is the key, not the content: the formatter never inspects the value.
        Map<String, Object> json =
                format(event(Map.of("customer.email", "not-a-real-person@example.invalid", "http.path", "/v1/me")));

        assertThat(json).doesNotContainKey("customer.email");
        assertThat(json.toString()).doesNotContain("not-a-real-person");
        assertThat(json).containsEntry("http.path", "/v1/me");
    }

    @Test
    void should_produce_one_line_of_valid_json_when_the_message_contains_quotes__TLY_004_AC4() {
        LoggingEvent event = event(Map.of(), "he said \"no\"\nand left");

        String line = formatter().format(event);

        assertThat(line).endsWith("\n");
        assertThat(line.strip()).doesNotContain("\n");
        assertThat(JsonParserFactory.getJsonParser().parseMap(line)).containsEntry("msg", "he said \"no\"\nand left");
    }

    private static Map<String, Object> format(LoggingEvent event) {
        return JsonParserFactory.getJsonParser().parseMap(formatter().format(event));
    }

    private static TallyStructuredLogFormatter formatter() {
        ThrowableProxyConverter converter = new ThrowableProxyConverter();
        converter.start();
        return new TallyStructuredLogFormatter(
                new MockEnvironment().withProperty("spring.application.name", "tally-core"), converter);
    }

    private static LoggingEvent event(Map<String, String> mdc) {
        return event(mdc, "invoice finalized");
    }

    private static LoggingEvent event(Map<String, String> mdc, String message) {
        LoggingEvent event = new LoggingEvent();
        event.setTimeStamp(1_764_000_000_000L);
        event.setLevel(Level.INFO);
        event.setMessage(message);
        event.setMDCPropertyMap(mdc);
        return event;
    }
}
