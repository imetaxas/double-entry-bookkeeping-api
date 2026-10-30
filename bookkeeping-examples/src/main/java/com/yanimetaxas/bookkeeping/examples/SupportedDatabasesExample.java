package com.yanimetaxas.bookkeeping.examples;

import com.yanimetaxas.bookkeeping.ChartOfAccounts;
import com.yanimetaxas.bookkeeping.ChartOfAccounts.ChartOfAccountsBuilder;
import com.yanimetaxas.bookkeeping.Ledger;
import com.yanimetaxas.bookkeeping.Ledger.LedgerBuilder;
import com.yanimetaxas.bookkeeping.jdbc.ConnectionOptions;
import com.yanimetaxas.bookkeeping.jdbc.DataSourceDriver;
import com.yanimetaxas.bookkeeping.model.AccountSide;
import com.yanimetaxas.bookkeeping.model.Money;
import java.nio.file.Files;
import lombok.extern.slf4j.Slf4j;

/**
 * Posts the same sale to embedded H2, HSQLDB, and Derby. MySQL and PostgreSQL use the same
 * connection setup and post only when a JDBC URL is supplied:
 *
 * <pre>{@code
 * mvn -pl bookkeeping-examples -am compile -Prun-example \
 *   -Dexec.mainClass=com.yanimetaxas.bookkeeping.examples.SupportedDatabasesExample \
 *   -Dbookkeeping.example.mysql.url=jdbc:mysql://localhost:3306/bookkeeping \
 *   -Dbookkeeping.example.mysql.username=bookkeeping \
 *   -Dbookkeeping.example.mysql.password=bookkeeping \
 *   -Dbookkeeping.example.postgres.url=jdbc:postgresql://localhost:5432/bookkeeping \
 *   -Dbookkeeping.example.postgres.username=bookkeeping \
 *   -Dbookkeeping.example.postgres.password=bookkeeping
 * }</pre>
 *
 * SQLite is demonstrated by {@link SqliteFileLedgerExample}.
 */
@Slf4j
public class SupportedDatabasesExample {

  public static void main(String... args) throws Exception {
    System.setProperty(
        "derby.system.home", Files.createTempDirectory("bookkeeping-derby").toString());
    long id = System.nanoTime();
    post(
        "H2",
        ConnectionOptions.EMBEDDED_H2_CONNECTION.url(
            "jdbc:h2:mem:example-h2-" + id + ";DB_CLOSE_DELAY=-1"));
    post(
        "HSQLDB",
        ConnectionOptions.EMBEDDED_HSQL_CONNECTION.url("jdbc:hsqldb:mem:example-hsql-" + id));
    post(
        "Derby",
        ConnectionOptions.EMBEDDED_DERBY_CONNECTION.url(
            "jdbc:derby:memory:example-derby-" + id + ";create=true"));
    connectIfConfigured("MySQL", DataSourceDriver.JDBC_MYSQL, "mysql");
    connectIfConfigured("PostgreSQL", DataSourceDriver.JDBC_POSTGRES, "postgres");
  }

  private static void connectIfConfigured(String label, DataSourceDriver driver, String key) {
    String url = System.getProperty("bookkeeping.example." + key + ".url");
    ConnectionOptions options =
        new ConnectionOptions(driver)
            .url(url == null || url.isBlank() ? defaultUrl(key) : url)
            .username(System.getProperty("bookkeeping.example." + key + ".username", "bookkeeping"))
            .password(System.getProperty("bookkeeping.example." + key + ".password", "bookkeeping"));
    log.info(
        "{} setup: driver={}, schema={}, url={}",
        label,
        options.driverClassName(),
        options.schema(),
        options.url());
    if (url == null || url.isBlank()) {
      log.info(
          "Skipped live {} posting. Set -Dbookkeeping.example.{}.url to connect.", label, key);
      return;
    }
    post(label, options);
  }

  private static String defaultUrl(String key) {
    if ("mysql".equals(key)) {
      return "jdbc:mysql://localhost:3306/bookkeeping";
    }
    return "jdbc:postgresql://localhost:5432/bookkeeping";
  }

  private static void post(String label, ConnectionOptions options) {
    ChartOfAccounts chart =
        new ChartOfAccountsBuilder()
            .create("Cash", "1000.00", "EUR", AccountSide.DEBIT)
            .create("Revenue", "0.00", "EUR", AccountSide.CREDIT)
            .create("Equity", "1000.00", "EUR", AccountSide.CREDIT)
            .build();
    Ledger ledger =
        new LedgerBuilder(chart).name(label + " shop").options(options).build().dropAll().init();
    ledger.commit(
        ledger
            .createTransferRequest()
            .reference("T1")
            .type("sale")
            .account("Cash")
            .debit("50.00", "EUR")
            .account("Revenue")
            .credit("50.00", "EUR")
            .build());
    expect(ledger.getAccountBalance("Cash"), "950.00", "EUR");
    if (!ledger.computeTrialBalance().isBalanced()) {
      throw new IllegalStateException(label + " trial balance did not balance");
    }
    log.info("=== {} posted Cash {} ===", label, ledger.getAccountBalance("Cash"));
  }

  private static void expect(Money actual, String amount, String currency) {
    Money expected = Money.toMoney(amount, currency);
    if (!actual.equals(expected)) {
      throw new IllegalStateException("Expected " + expected + " but was " + actual);
    }
  }
}
