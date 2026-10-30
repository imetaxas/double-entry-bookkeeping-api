package com.yanimetaxas.bookkeeping.jdbc;

import com.yanimetaxas.bookkeeping.ChartOfAccounts;
import com.yanimetaxas.bookkeeping.Ledger;
import com.yanimetaxas.bookkeeping.Ledger.LedgerBuilder;
import com.yanimetaxas.bookkeeping.LedgerContractTest;

class H2LedgerContractTest extends LedgerContractTest {

  @Override
  protected Ledger ledger(ChartOfAccounts chart, String name) {
    ConnectionOptions options =
        new ConnectionOptions("org.h2.Driver")
            .url("jdbc:h2:mem:" + name + ";DB_CLOSE_DELAY=-1")
            .username("sa")
            .password("")
            .schema("h2-schema.sql");
    return new LedgerBuilder(chart).name(name).options(options).build().init();
  }
}
