package com.tally.core.kernel.tenant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

class TenantAwareDataSourceTest {

    private final DataSource delegate = mock(DataSource.class);
    private final Connection connection = mock(Connection.class);
    private final Statement statement = mock(Statement.class);
    private final TenantAwareDataSource dataSource = new TenantAwareDataSource(delegate);
    private final ListAppender<ILoggingEvent> logs = new ListAppender<>();

    @BeforeEach
    void wireMocksAndLogCapture() throws SQLException {
        when(delegate.getConnection()).thenReturn(connection);
        when(connection.createStatement()).thenReturn(statement);
        Logger logger = (Logger) LoggerFactory.getLogger(TenantAwareDataSource.class);
        logs.start();
        logger.addAppender(logs);
    }

    @AfterEach
    void detachLogCapture() {
        ((Logger) LoggerFactory.getLogger(TenantAwareDataSource.class)).detachAppender(logs);
    }

    @Test
    void should_set_local_app_tenant_id_when_a_tenant_is_bound__TLY_102_AC2() throws Exception {
        UUID tenantId = UUID.randomUUID();

        ScopedValue.where(TenantContextHolder.TENANT, new TenantContext(tenantId))
                .call(() -> {
                    dataSource.getConnection();
                    return null;
                });

        verify(statement).execute(contains("SET LOCAL app.tenant_id = '" + tenantId + "'"));
    }

    @Test
    void should_return_zero_rows_path_and_log_warn_when_context_is_unset__TLY_102_AC3() throws Exception {
        dataSource.getConnection();

        verify(statement, never()).execute(anyString());
        assertThat(logs.list).anySatisfy(event -> {
            assertThat(event.getLevel()).isEqualTo(Level.WARN);
            assertThat(event.getFormattedMessage()).isEqualTo("tenant_context_missing");
        });
    }
}
