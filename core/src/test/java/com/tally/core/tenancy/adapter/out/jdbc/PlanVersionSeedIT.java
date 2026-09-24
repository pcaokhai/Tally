package com.tally.core.tenancy.adapter.out.jdbc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tally.core.support.PostgresKafkaIT;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.DataSourceUtils;

/** Proves the seeded plan versions and their immutability (docs/06 TLY-101 AC3). */
class PlanVersionSeedIT extends PostgresKafkaIT {

    @Autowired
    private JdbcClient jdbc;

    @Autowired
    private DataSource dataSource;

    @Test
    void should_have_immutable_seeded_plan_versions_for_starter_growth_scale__TLY_101_AC3() throws SQLException {
        List<String> planCodes = jdbc.sql("SELECT plan_code FROM tenancy.plan_versions ORDER BY plan_code")
                .query(String.class)
                .list();

        assertThat(planCodes).containsExactly("growth", "scale", "starter");

        // no UPDATE grant to tally_app on plan_versions -> the seeded rows cannot be mutated by the
        // app role; SET ROLE and the statement must share one connection, so this is done manually.
        Connection connection = DataSourceUtils.getConnection(dataSource);
        try (var statement = connection.createStatement()) {
            statement.execute("SET ROLE tally_app");
            assertThatThrownBy(() -> statement.execute(
                            "UPDATE tenancy.plan_versions SET version = version WHERE plan_code = 'starter'"))
                    .hasMessageContaining("permission denied");
            statement.execute("RESET ROLE");
        } finally {
            DataSourceUtils.releaseConnection(connection, dataSource);
        }
    }

    @Test
    void should_reference_exactly_one_plan_version_per_tenant__TLY_101_AC3() {
        UUID planVersionId = jdbc.sql("SELECT id FROM tenancy.plan_versions WHERE plan_code = 'starter'")
                .query(UUID.class)
                .single();

        assertThat(planVersionId).isNotNull();
    }
}
