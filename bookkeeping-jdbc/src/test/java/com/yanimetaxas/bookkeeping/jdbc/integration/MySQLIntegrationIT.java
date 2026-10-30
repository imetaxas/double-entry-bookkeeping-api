package com.yanimetaxas.bookkeeping.jdbc.integration;

import com.yanimetaxas.bookkeeping.ChartOfAccounts;
import com.yanimetaxas.bookkeeping.Ledger;
import com.yanimetaxas.bookkeeping.Ledger.LedgerBuilder;
import com.yanimetaxas.bookkeeping.LedgerContractTest;
import com.yanimetaxas.bookkeeping.jdbc.ConnectionOptions;
import com.yanimetaxas.bookkeeping.jdbc.DataSourceDriver;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;

@Testcontainers
class MySQLIntegrationIT extends LedgerContractTest {

  @Container
  static MySQLContainer mysql =
      new MySQLContainer("mysql:8.0")
          .withDatabaseName("bookkeeping_test")
          .withUsername("test")
          .withPassword("test");

  @Override
  protected Ledger ledger(ChartOfAccounts chart, String name) {
    ConnectionOptions options =
        new ConnectionOptions(DataSourceDriver.JDBC_MYSQL)
            .url(mysql.getJdbcUrl())
            .username(mysql.getUsername())
            .password(mysql.getPassword());
    return new LedgerBuilder(chart).name(name).options(options).build().init();
  }
}