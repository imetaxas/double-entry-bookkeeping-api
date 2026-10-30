package com.yanimetaxas.bookkeeping.jdbc;

import com.yanimetaxas.bookkeeping.ChartOfAccounts;
import com.yanimetaxas.bookkeeping.Ledger;
import com.yanimetaxas.bookkeeping.Ledger.LedgerBuilder;
import com.yanimetaxas.bookkeeping.LedgerContractTest;
import java.nio.file.Path;
import org.junit.jupiter.api.io.TempDir;

class SqliteLedgerContractTest extends LedgerContractTest {

  @TempDir Path tempDir;

  @Override
  protected Ledger ledger(ChartOfAccounts chart, String name) {
    ConnectionOptions options =
        new ConnectionOptions(DataSourceDriver.JDBC_SQLITE)
            .url("jdbc:sqlite:" + tempDir.resolve(name + ".db").toAbsolutePath());
    return new LedgerBuilder(chart).name(name).options(options).build().init();
  }
}
