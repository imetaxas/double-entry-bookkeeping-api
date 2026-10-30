package com.yanimetaxas.bookkeeping.jdbc;

import com.yanimetaxas.bookkeeping.ChartOfAccounts;
import com.yanimetaxas.bookkeeping.Ledger;
import com.yanimetaxas.bookkeeping.Ledger.LedgerBuilder;
import com.yanimetaxas.bookkeeping.LedgerContractTest;

class DerbyLedgerContractTest extends LedgerContractTest {

  @Override
  protected Ledger ledger(ChartOfAccounts chart, String name) {
    ConnectionOptions options =
        new ConnectionOptions("org.apache.derby.jdbc.EmbeddedDriver")
            .url("jdbc:derby:memory:contract-" + name + ";create=true")
            .username("sa")
            .password("")
            .schema("derby-schema.sql");
    return new LedgerBuilder(chart).name(name).options(options).build().init();
  }
}
