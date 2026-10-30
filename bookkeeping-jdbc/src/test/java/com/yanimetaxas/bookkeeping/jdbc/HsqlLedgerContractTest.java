package com.yanimetaxas.bookkeeping.jdbc;

import com.yanimetaxas.bookkeeping.ChartOfAccounts;
import com.yanimetaxas.bookkeeping.Ledger;
import com.yanimetaxas.bookkeeping.Ledger.LedgerBuilder;
import com.yanimetaxas.bookkeeping.LedgerContractTest;

class HsqlLedgerContractTest extends LedgerContractTest {

  @Override
  protected Ledger ledger(ChartOfAccounts chart, String name) {
    ConnectionOptions options =
        new ConnectionOptions("org.hsqldb.jdbcDriver")
            .url("jdbc:hsqldb:mem:contract-" + name)
            .username("sa")
            .password("")
            .schema("h2-schema.sql");
    return new LedgerBuilder(chart).name(name).options(options).build().init();
  }
}
