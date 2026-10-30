package com.yanimetaxas.bookkeeping.jdbc.integration;

import com.yanimetaxas.bookkeeping.ChartOfAccounts;
import com.yanimetaxas.bookkeeping.Ledger;
import com.yanimetaxas.bookkeeping.Ledger.LedgerBuilder;
import com.yanimetaxas.bookkeeping.LedgerContractTest;
import com.yanimetaxas.bookkeeping.jdbc.ConnectionOptions;
import com.yanimetaxas.bookkeeping.jdbc.DataSourceDriver;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@Testcontainers
class PostgresIntegrationIT extends LedgerContractTest {

  @Container
  static PostgreSQLContainer postgres =
      new PostgreSQLContainer("postgres:16-alpine")
          .withDatabaseName("bookkeeping_test")
          .withUsername("test")
          .withPassword("test");

  @Override
  protected Ledger ledger(ChartOfAccounts chart, String name) {
    ConnectionOptions options =
        new ConnectionOptions(DataSourceDriver.JDBC_POSTGRES)
            .url(postgres.getJdbcUrl())
            .username(postgres.getUsername())
            .password(postgres.getPassword());
    return new LedgerBuilder(chart).name(name).options(options).build().init();
  }
}
