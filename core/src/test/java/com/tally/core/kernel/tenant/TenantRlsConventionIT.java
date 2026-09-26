package com.tally.core.kernel.tenant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tally.core.support.PostgresKafkaIT;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.DataSourceUtils;

/**
 * Proves the RLS + composite-FK convention every tenant table must follow (TLY-102 AC2/AC3/AC4),
 * using two throwaway test-only tables that mirror the convention documented in
 * docs/plans/TLY-102.md. TLY-101's real tenant tables follow the same shape.
 *
 * <p>{@code kernel.rls_probe_parent}/{@code _child} are dropped and recreated in each test so this
 * class never depends on execution order or on TLY-101 having merged real tables yet.
 */
class TenantRlsConventionIT extends PostgresKafkaIT {

    private final JdbcClient jdbc;
    private final DataSource dataSource;

    @Autowired
    TenantRlsConventionIT(JdbcClient jdbc, DataSource dataSource) {
        this.jdbc = jdbc;
        this.dataSource = dataSource;
    }

    @BeforeEach
    void recreateProbeSchema() {
        jdbc.sql("DROP TABLE IF EXISTS kernel.rls_probe_child").update();
        jdbc.sql("DROP TABLE IF EXISTS kernel.rls_probe_parent").update();
        jdbc.sql("""
                CREATE TABLE kernel.rls_probe_parent (
                  tenant_id uuid NOT NULL,
                  id uuid NOT NULL,
                  PRIMARY KEY (tenant_id, id))
                """).update();
        jdbc.sql("""
                CREATE TABLE kernel.rls_probe_child (
                  tenant_id uuid NOT NULL,
                  id uuid NOT NULL,
                  parent_id uuid NOT NULL,
                  PRIMARY KEY (tenant_id, id),
                  FOREIGN KEY (tenant_id, parent_id) REFERENCES kernel.rls_probe_parent (tenant_id, id))
                """).update();
        for (String table : List.of("kernel.rls_probe_parent", "kernel.rls_probe_child")) {
            jdbc.sql("ALTER TABLE " + table + " ENABLE ROW LEVEL SECURITY").update();
            jdbc.sql("ALTER TABLE " + table + " FORCE ROW LEVEL SECURITY").update();
            jdbc.sql("CREATE POLICY tenant_isolation ON " + table
                            + " USING (tenant_id = kernel.current_tenant())"
                            + " WITH CHECK (tenant_id = kernel.current_tenant())")
                    .update();
            jdbc.sql("GRANT SELECT, INSERT, UPDATE, DELETE ON " + table + " TO tally_app")
                    .update();
        }
        jdbc.sql("GRANT USAGE ON SCHEMA kernel TO tally_app").update();
    }

    @Test
    void should_have_rls_enabled_and_forced_on_every_table_with_a_tenant_id_column__TLY_102_AC2() {
        var tablesMissingRls = jdbc.sql("""
                SELECT c.relnamespace::regnamespace || '.' || c.relname
                FROM pg_class c
                JOIN information_schema.columns col
                  ON col.table_schema = c.relnamespace::regnamespace::text
                 AND col.table_name = c.relname
                 AND col.column_name = 'tenant_id'
                WHERE c.relkind = 'r'
                  AND (NOT c.relrowsecurity OR NOT c.relforcerowsecurity)
                """).query(String.class).list();

        assertThat(tablesMissingRls).isEmpty();
    }

    @Test
    void should_return_zero_rows_when_tenant_context_is_unset__TLY_102_AC3() throws SQLException {
        UUID otherTenant = UUID.randomUUID();
        jdbc.sql("INSERT INTO kernel.rls_probe_parent (tenant_id, id) VALUES (?, ?)")
                .params(otherTenant, UUID.randomUUID())
                .update();

        Connection connection = DataSourceUtils.getConnection(dataSource);
        try (var statement = connection.createStatement()) {
            statement.execute("SET ROLE tally_app");
            var rows = statement.executeQuery("SELECT * FROM kernel.rls_probe_parent");
            assertThat(rows.next())
                    .as("no row visible without app.tenant_id set")
                    .isFalse();
            statement.execute("RESET ROLE");
        } finally {
            DataSourceUtils.releaseConnection(connection, dataSource);
        }
    }

    @Test
    void should_reject_a_composite_fk_across_tenants_even_when_rls_would_not_block_it__TLY_102_AC4() {
        UUID tenantA = UUID.randomUUID();
        UUID tenantB = UUID.randomUUID();
        UUID parentBId = UUID.randomUUID();
        jdbc.sql("INSERT INTO kernel.rls_probe_parent (tenant_id, id) VALUES (?, ?)")
                .params(tenantB, parentBId)
                .update();

        // This connection is the default superuser test role, which always bypasses RLS: the
        // composite FK is the only thing that can still reject this insert.
        assertThatThrownBy(
                        () -> jdbc.sql("INSERT INTO kernel.rls_probe_child (tenant_id, id, parent_id) VALUES (?, ?, ?)")
                                .params(tenantA, UUID.randomUUID(), parentBId)
                                .update())
                .hasMessageContaining("foreign key");
    }
}
