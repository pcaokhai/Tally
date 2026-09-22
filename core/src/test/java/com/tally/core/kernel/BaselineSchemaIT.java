package com.tally.core.kernel;

import static org.assertj.core.api.Assertions.assertThat;

import com.tally.core.support.PostgresKafkaIT;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;

class BaselineSchemaIT extends PostgresKafkaIT {

    private static final List<String> BASELINE_SCHEMAS = List.of(
            "tenancy",
            "catalog",
            "customers",
            "billing",
            "metering",
            "payments",
            "ledger",
            "kernel",
            "reporting",
            "ops",
            "events");

    private final JdbcClient jdbc;

    @Autowired
    BaselineSchemaIT(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Test
    void should_record_one_successful_baseline_row_when_flyway_has_migrated__TLY_004_AC3() {
        var applied = jdbc.sql("SELECT version || ' ' || description FROM flyway_schema_history WHERE success")
                .query(String.class)
                .list();

        // Flyway keeps the version exactly as the filename spells it, so V001 is stored as "001".
        assertThat(applied).containsExactly("001 baseline roles schemas");
    }

    @Test
    void should_create_all_eleven_schemas_when_flyway_has_migrated__TLY_004_AC3() {
        var schemas = jdbc.sql("SELECT schema_name FROM information_schema.schemata")
                .query(String.class)
                .list();

        assertThat(schemas).containsAll(BASELINE_SCHEMAS);
    }

    @Test
    void should_return_null_tenant_when_app_tenant_id_is_unset__TLY_004_AC3() {
        var tenantId =
                jdbc.sql("SELECT kernel.current_tenant()").query(String.class).optional();

        assertThat(tenantId).isEmpty();
    }
}
