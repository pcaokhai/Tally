package com.tally.core.kernel.tenant;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.datasource.DelegatingDataSource;

/**
 * Issues {@code SET LOCAL app.tenant_id} on every connection handed out (ADR-003, TLY-102 AC2).
 * Spring's transaction manager obtains exactly one connection per transaction (via {@code
 * DataSourceUtils}) and holds it for the transaction's lifetime, so setting it here, once, at
 * {@link #getConnection()} covers every statement the transaction runs. {@code SET LOCAL} reverts
 * automatically at commit/rollback, so nothing leaks onto the next borrower of a pooled connection.
 *
 * <p>When no tenant is bound, nothing is set: {@code kernel.current_tenant()} then returns NULL and
 * every RLS policy comparing {@code tenant_id = kernel.current_tenant()} fails closed (AC3). A WARN
 * is logged so a missing context is visible in ops, not just silently empty result sets.
 */
public final class TenantAwareDataSource extends DelegatingDataSource {

    private static final Logger log = LoggerFactory.getLogger(TenantAwareDataSource.class);

    public TenantAwareDataSource(DataSource delegate) {
        super(delegate);
    }

    @Override
    public Connection getConnection() throws SQLException {
        return apply(super.getConnection());
    }

    @Override
    public Connection getConnection(String username, String password) throws SQLException {
        return apply(super.getConnection(username, password));
    }

    private Connection apply(Connection connection) throws SQLException {
        var tenant = TenantContextHolder.current();
        if (tenant.isEmpty()) {
            log.warn("tenant_context_missing");
            return connection;
        }
        try (Statement statement = connection.createStatement()) {
            // UUID#toString never contains SQL metacharacters, so this is not injectable.
            statement.execute("SET LOCAL app.tenant_id = '" + tenant.get().tenantId() + "'");
        }
        return connection;
    }
}
