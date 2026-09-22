package com.tally.core.support;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Tag;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Base class for integration tests. Starts one Postgres and one Kafka container per JVM and lets
 * Flyway migrate the database. The empty {@code spring.autoconfigure.exclude} undoes the exclusions
 * that keep the unit {@code test} task runnable without Docker.
 */
@Tag("integration")
@SpringBootTest(properties = "spring.autoconfigure.exclude=")
public abstract class PostgresKafkaIT {

    protected static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-09-22T07:00:00Z"), ZoneOffset.UTC);

    /** Container images, kept together so the tech lead can retune them in one place. */
    private static final String POSTGRES_IMAGE = "postgres:17-alpine";

    /** Matches the broker pinned in {@code deploy/compose.yaml} (TLY-002). */
    private static final String KAFKA_IMAGE = "apache/kafka:4.1.0";

    @ServiceConnection
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer(POSTGRES_IMAGE).withReuse(true);

    static final KafkaContainer KAFKA = new KafkaContainer(KAFKA_IMAGE).withReuse(true);

    static {
        POSTGRES.start();
        KAFKA.start();
    }

    @DynamicPropertySource
    static void kafkaProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", KAFKA::getBootstrapServers);
    }
}
