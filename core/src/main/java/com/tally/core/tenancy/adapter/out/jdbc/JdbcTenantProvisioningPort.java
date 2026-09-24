package com.tally.core.tenancy.adapter.out.jdbc;

import com.tally.core.tenancy.application.port.out.TenantProvisioningPort;
import com.tally.core.tenancy.domain.IsolationTier;
import com.tally.core.tenancy.domain.PlanVersion;
import com.tally.core.tenancy.domain.Tenant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * JdbcClient adapter for {@link TenantProvisioningPort}. The whole class is {@code @Transactional}
 * (TLY-102 AC5: every SQL call from {@code ..adapter.out..} must run inside a transaction so
 * {@code TenantAwareDataSource} has a transaction-scoped connection to set {@code app.tenant_id}
 * on).
 */
@Repository
@Transactional
public class JdbcTenantProvisioningPort implements TenantProvisioningPort {

    private final JdbcClient jdbcClient;

    public JdbcTenantProvisioningPort(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    public Optional<PlanVersion> findLatestPlanVersion(String planCode) {
        return jdbcClient
                .sql("""
                        SELECT id, plan_code, version, limits
                        FROM tenancy.plan_versions
                        WHERE plan_code = :planCode
                        ORDER BY version DESC
                        LIMIT 1
                        """)
                .param("planCode", planCode)
                .query((rs, rowNum) -> new PlanVersion(
                        UUID.fromString(rs.getString("id")),
                        rs.getString("plan_code"),
                        rs.getInt("version"),
                        rs.getString("limits").contains("\"isolation_tier\": \"SILO\"") ? IsolationTier.SILO : null))
                .optional();
    }

    @Override
    public void insertTenant(Tenant tenant) {
        jdbcClient
                .sql("""
                        INSERT INTO tenancy.tenants (id, name, plan_version_id, isolation_tier, status)
                        VALUES (:id, :name, :planVersionId, :isolationTier, :status)
                        """)
                .param("id", tenant.id())
                .param("name", tenant.name())
                .param("planVersionId", tenant.planVersionId())
                .param("isolationTier", tenant.isolationTier().name())
                .param("status", tenant.status())
                .update();
    }

    @Override
    public void insertOwnerInvitation(UUID tenantId, UUID invitationId, String ownerEmail) {
        jdbcClient
                .sql("""
                        INSERT INTO tenancy.invitations (tenant_id, id, email, role)
                        VALUES (:tenantId, :id, :email, 'OWNER')
                        """)
                .param("tenantId", tenantId)
                .param("id", invitationId)
                .param("email", ownerEmail)
                .update();
    }

    @Override
    public void insertInvoiceNumberSeq(UUID tenantId) {
        jdbcClient
                .sql("INSERT INTO billing.invoice_number_seqs (tenant_id) VALUES (:tenantId)")
                .param("tenantId", tenantId)
                .update();
    }

    @Override
    public void insertDefaultLedgerAccounts(UUID tenantId) {
        for (DefaultAccount account : DefaultAccount.values()) {
            jdbcClient
                    .sql("""
                            INSERT INTO ledger.accounts (tenant_id, id, code, name, type)
                            VALUES (:tenantId, :id, :code, :name, :type)
                            """)
                    .param("tenantId", tenantId)
                    .param("id", UUID.randomUUID())
                    .param("code", account.code)
                    .param("name", account.accountName)
                    .param("type", account.type)
                    .update();
        }
    }

    @Override
    public void writeAuditLog(UUID id, UUID tenantId, String actor, String action, String payloadJson) {
        jdbcClient
                .sql("""
                        INSERT INTO kernel.audit_log (id, tenant_id, actor, action, payload)
                        VALUES (:id, :tenantId, :actor, :action, :payload::jsonb)
                        """)
                .param("id", id)
                .param("tenantId", tenantId)
                .param("actor", actor)
                .param("action", action)
                .param("payload", payloadJson)
                .update();
    }

    @Override
    public Optional<String> findIdempotentResponse(String idempotencyKey) {
        return jdbcClient
                .sql("SELECT response::text AS response FROM kernel.idempotency_keys WHERE key = :key")
                .param("key", idempotencyKey)
                .query((rs, rowNum) -> rs.getString("response"))
                .optional();
    }

    @Override
    public void saveIdempotentResponse(String idempotencyKey, String responseJson) {
        jdbcClient
                .sql("""
                        INSERT INTO kernel.idempotency_keys (key, response) VALUES (:key, :response::jsonb)
                        ON CONFLICT (key) DO NOTHING
                        """)
                .param("key", idempotencyKey)
                .param("response", responseJson)
                .update();
    }

    /** Minimal chart of accounts every new tenant needs before invoicing can post anything (TLY-202
     * owns real postings; this story just needs the rows to exist). */
    private enum DefaultAccount {
        RECEIVABLE("1000", "Accounts Receivable", "ASSET"),
        REVENUE("4000", "Subscription Revenue", "REVENUE"),
        CASH("1010", "Cash", "ASSET");

        final String code;
        final String accountName;
        final String type;

        DefaultAccount(String code, String accountName, String type) {
            this.code = code;
            this.accountName = accountName;
            this.type = type;
        }
    }
}
