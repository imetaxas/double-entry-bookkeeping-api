package com.yanimetaxas.bookkeeping.examples;

import com.yanimetaxas.bookkeeping.ChartOfAccounts;
import com.yanimetaxas.bookkeeping.ChartOfAccounts.ChartOfAccountsBuilder;
import com.yanimetaxas.bookkeeping.Ledger;
import com.yanimetaxas.bookkeeping.Ledger.LedgerBuilder;
import com.yanimetaxas.bookkeeping.jdbc.ConnectionOptions;
import com.yanimetaxas.bookkeeping.jdbc.DataSourceDriver;
import com.yanimetaxas.bookkeeping.jdbc.JdbcStorageProvider;
import com.yanimetaxas.bookkeeping.model.AccountSide;
import com.yanimetaxas.bookkeeping.model.Money;
import com.yanimetaxas.bookkeeping.model.TransferRequest;
import java.nio.file.Files;
import java.nio.file.Path;
import lombok.extern.slf4j.Slf4j;
import org.sqlite.SQLiteDataSource;

/**
 * Writes a ledger to a SQLite file, then reads that file through an application-owned {@code
 * DataSource}. {@link JdbcStorageProvider} enables SQLite foreign keys for that external source.
 *
 * <p>Run: {@code mvn -pl bookkeeping-examples -am compile -Prun-example
 * -Dexec.mainClass=com.yanimetaxas.bookkeeping.examples.SqliteFileLedgerExample}
 */
@Slf4j
public class SqliteFileLedgerExample {

  public static void main(String... args) throws Exception {
    Path file = Files.createTempFile("bookkeeping-", ".db");
    try {
      String url = "jdbc:sqlite:" + file.toAbsolutePath();
      Ledger created =
          new LedgerBuilder(chart())
              .name("sqlite-file")
              .options(new ConnectionOptions(DataSourceDriver.JDBC_SQLITE).url(url))
              .build()
              .init();
      created.commit(sale(created));
      expect(created.getAccountBalance("Cash"), "950.00", "EUR");

      SQLiteDataSource dataSource = new SQLiteDataSource();
      dataSource.setUrl(url);
      Ledger reloaded =
          new LedgerBuilder(existingChart())
              .name("sqlite-file")
              .storage(new JdbcStorageProvider(dataSource, "sqlite-schema.sql"))
              .build()
              .init();
      expect(reloaded.getAccountBalance("Cash"), "950.00", "EUR");
      log.info("=== SQLite file {} ===", file.toAbsolutePath());
      reloaded.printHistoryLog();
    } finally {
      Files.deleteIfExists(file);
    }
  }

  private static ChartOfAccounts chart() {
    return new ChartOfAccountsBuilder()
        .create("Cash", "1000.00", "EUR", AccountSide.DEBIT)
        .create("Revenue", "0.00", "EUR", AccountSide.CREDIT)
        .create("Equity", "1000.00", "EUR", AccountSide.CREDIT)
        .build();
  }

  private static ChartOfAccounts existingChart() {
    return new ChartOfAccountsBuilder()
        .includeExisted("Cash")
        .includeExisted("Revenue")
        .includeExisted("Equity")
        .build();
  }

  private static TransferRequest sale(Ledger ledger) {
    return ledger
        .createTransferRequest()
        .reference("T1")
        .type("sale")
        .account("Cash")
        .debit("50.00", "EUR")
        .account("Revenue")
        .credit("50.00", "EUR")
        .build();
  }

  private static void expect(Money actual, String amount, String currency) {
    Money expected = Money.toMoney(amount, currency);
    if (!actual.equals(expected)) {
      throw new IllegalStateException("Expected " + expected + " but was " + actual);
    }
  }
}
