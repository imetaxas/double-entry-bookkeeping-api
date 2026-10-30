package com.yanimetaxas.bookkeeping;

import com.yanimetaxas.bookkeeping.Ledger.LedgerBuilder;

class InMemoryLedgerContractTest extends LedgerContractTest {

  @Override
  protected Ledger ledger(ChartOfAccounts chart, String name) {
    return new LedgerBuilder(chart).name(name).build().init();
  }
}
