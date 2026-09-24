package com.tally.core.archfixtures.tenantsql.adapter.out;

import org.springframework.jdbc.core.simple.JdbcClient;

/** Deliberate violator of {@code sqlInAdapterOutRunsInATransaction}. Never used in production code. */
public class NonTransactionalSqlFixture {

    private final JdbcClient jdbc;

    public NonTransactionalSqlFixture(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public void readWithoutTransaction() {
        jdbc.sql("SELECT 1").query(Integer.class).single();
    }
}
