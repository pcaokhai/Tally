package com.tally.core.tenancy.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.tally.core.support.PostgresKafkaIT;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;

/**
 * Proves the fix for the race an independent review caught in TLY-101's first version: two
 * concurrent {@code POST /ops/v1/tenants} calls with the same {@code Idempotency-Key} must
 * provision exactly one tenant, not two (docs/06 TLY-101 AC1; docs/plans/TLY-101.md Ruling 1).
 */
class ConcurrentIdempotencyIT extends PostgresKafkaIT {

    @Autowired
    private CreateTenantUseCase useCase;

    @Autowired
    private JdbcClient jdbc;

    @Test
    void should_provision_exactly_one_tenant_when_two_requests_race_the_same_idempotency_key__TLY_101_AC1()
            throws Exception {
        String key = "race-key-" + java.util.UUID.randomUUID();
        Callable<CreateTenantResult> call =
                () -> useCase.create(new CreateTenantCommand("RaceCo", "starter", "owner@race.test", "operator", key));

        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Future<CreateTenantResult> f1 = pool.submit(call);
            Future<CreateTenantResult> f2 = pool.submit(call);
            CreateTenantResult r1 = f1.get();
            CreateTenantResult r2 = f2.get();

            assertThat(r2).isEqualTo(r1);

            Long tenantCount = jdbc.sql("SELECT count(*) FROM tenancy.tenants WHERE name = 'RaceCo'")
                    .query(Long.class)
                    .single();
            assertThat(tenantCount)
                    .as("exactly one tenant provisioned despite the race")
                    .isEqualTo(1L);
        } finally {
            pool.shutdownNow();
        }
    }
}
