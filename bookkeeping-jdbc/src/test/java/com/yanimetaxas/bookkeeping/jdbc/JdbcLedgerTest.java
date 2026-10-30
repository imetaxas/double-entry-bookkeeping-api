package com.yanimetaxas.bookkeeping.jdbc;

import static io.github.imetaxas.realitycheck.RealityAssertions.assertThat;

import com.yanimetaxas.bookkeeping.ChartOfAccounts;
import com.yanimetaxas.bookkeeping.ChartOfAccounts.ChartOfAccountsBuilder;
import com.yanimetaxas.bookkeeping.Ledger;
import com.yanimetaxas.bookkeeping.Ledger.LedgerBuilder;
import com.yanimetaxas.bookkeeping.model.Account;
import com.yanimetaxas.bookkeeping.model.AccountSide;
import com.yanimetaxas.bookkeeping.model.Money;
import com.yanimetaxas.bookkeeping.model.TransferRequest;
import com.yanimetaxas.bookkeeping.model.TrialBalanceEntry;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class JdbcLedgerTest {

  @BeforeEach
  void cleanDatabase() {
    new JdbcStorageProvider(ConnectionOptions.EMBEDDED_H2_CONNECTION).dropAll();
  }

  @Test
  void listAccountsReturnsAllJdbcAccounts() {
    ChartOfAccounts chart =
        new ChartOfAccountsBuilder()
            .create("CASH", "1000.00", "EUR", AccountSide.DEBIT)
            .create("REVENUE", "0.00", "EUR", AccountSide.CREDIT)
            .build();

    Ledger ledger =
        new LedgerBuilder(chart)
            .name("JDBC H2")
            .options(ConnectionOptions.EMBEDDED_H2_CONNECTION)
            .build()
            .init();

    assertThat(
            ledger.listAccounts().stream()
                .map(account -> account.getAccountRef() + " " + account.getNormalBalance())
                .toList())
        .containsExactly("CASH DEBIT", "REVENUE CREDIT");
  }

  @Test
  void dropAllResetsJdbcDatabase() {
    ChartOfAccounts chart =
        new ChartOfAccountsBuilder()
            .create("cash_EUR", "500.00", "EUR")
            .create("revenue_EUR", "0.00", "EUR")
            .build();

    JdbcStorageProvider storage = new JdbcStorageProvider(ConnectionOptions.EMBEDDED_H2_CONNECTION);
    Ledger ledger =
        new LedgerBuilder(chart).name("JDBC H2").storage(storage).build().init();

    ledger.commit(
        ledger
            .createTransferRequest()
            .reference("T1")
            .type("sale")
            .account("cash_EUR")
            .debit("100.00", "EUR")
            .account("revenue_EUR")
            .credit("100.00", "EUR")
            .build());

    assertThat(ledger.getAccountBalance("cash_EUR")).isEqualTo(Money.toMoney("400.00", "EUR"));

    ledger.dropAll().init();

    assertThat(ledger.getAccountBalance("cash_EUR")).isEqualTo(Money.toMoney("500.00", "EUR"));
    assertThat(ledger.findTransactions("cash_EUR")).isEmpty();
  }

  @Test
  void initPreservesCommittedBalance() {
    ConnectionOptions options = memoryDatabase("reopen");
    new JdbcStorageProvider(options).dropAll();
    ChartOfAccounts chart =
        new ChartOfAccountsBuilder()
            .create("CASH", "800.00", "EUR")
            .create("REVENUE", "0.00", "EUR")
            .build();

    Ledger first = new LedgerBuilder(chart).name("Books").options(options).build().init();
    first.commit(payment(first, "T1", "50.00"));

    Ledger reopened = new LedgerBuilder(chart).name("Books").options(options).build().init();

    assertThat(reopened.getAccountBalance("CASH")).isEqualTo(Money.toMoney("750.00", "EUR"));
    assertThat(reopened.getTransactionByRef("T1")).isNotNull();
  }

  @Test
  void ledgersWithDifferentNamesDoNotShareBalances() {
    ConnectionOptions options = memoryDatabase("scoped");
    new JdbcStorageProvider(options).dropAll();
    ChartOfAccounts chart =
        new ChartOfAccountsBuilder()
            .create("CASH", "1000.00", "EUR")
            .create("REVENUE", "0.00", "EUR")
            .build();

    Ledger primary = new LedgerBuilder(chart).name("Primary").options(options).build().init();
    Ledger secondary = new LedgerBuilder(chart).name("Secondary").options(options).build().init();
    primary.commit(payment(primary, "T1", "100.00"));

    assertThat(primary.getAccountBalance("CASH")).isEqualTo(Money.toMoney("900.00", "EUR"));
    assertThat(secondary.getAccountBalance("CASH")).isEqualTo(Money.toMoney("1000.00", "EUR"));
    assertThat(secondary.findTransactions("CASH")).isEmpty();
    assertThat(secondary.getTransactionByRef("T1")).isNull();
  }

  @Test
  void sqlitePreservesFractionalAmountsAcrossReopen(@TempDir Path tempDir) {
    ConnectionOptions options =
        new ConnectionOptions(DataSourceDriver.JDBC_SQLITE)
            .url("jdbc:sqlite:" + tempDir.resolve("ledger.db").toAbsolutePath());
    ChartOfAccounts chart =
        new ChartOfAccountsBuilder()
            .create("CASH", "1000.000", "EUR")
            .create("REVENUE", "0.000", "EUR")
            .build();

    Ledger ledger = new LedgerBuilder(chart).name("SQLite Books").options(options).build().init();
    ledger.commit(
        TransferRequest.builder()
            .reference("T-SCALE")
            .type("testing")
            .account("CASH")
            .amount(Money.toMoney("-5.123", "EUR"))
            .account("REVENUE")
            .amount(Money.toMoney("5.123", "EUR"))
            .build());

    Ledger reopened =
        new LedgerBuilder(chart).name("SQLite Books").options(options).build().init();

    assertThat(reopened.getAccountBalance("CASH")).isEqualTo(Money.toMoney("994.877", "EUR"));
    assertThat(reopened.getAccountBalance("REVENUE")).isEqualTo(Money.toMoney("5.123", "EUR"));
  }

  @Test
  void jdbcStoresTheClockInstant() {
    Instant posted = Instant.parse("2024-05-06T07:08:09Z");
    ConnectionOptions options = memoryDatabase("clock");
    new JdbcStorageProvider(options).dropAll();
    ChartOfAccounts chart =
        new ChartOfAccountsBuilder()
            .create("CASH", "10.00", "EUR")
            .create("REVENUE", "0.00", "EUR")
            .build();

    Ledger ledger =
        new LedgerBuilder(chart)
            .name("clock")
            .options(options)
            .clock(Clock.fixed(posted, ZoneOffset.UTC))
            .build()
            .init();
    ledger.commit(payment(ledger, "T1", "1.00"));

    assertThat(ledger.getTransactionByRef("T1").getTransactionDate()).isEqualTo(posted);
  }

  @Test
  void reopenedLedgerUsesTheStoredOpening() {
    ConnectionOptions options = memoryDatabase("reopen-opening");
    new JdbcStorageProvider(options).dropAll();
    ChartOfAccounts original =
        new ChartOfAccountsBuilder()
            .create("CASH", "1000.00", "EUR", AccountSide.DEBIT)
            .create("REVENUE", "0.00", "EUR", AccountSide.CREDIT)
            .build();
    Ledger first = new LedgerBuilder(original).name("books").options(options).build().init();
    first.commit(payment(first, "T1", "50.00"));

    ChartOfAccounts replacement =
        new ChartOfAccountsBuilder()
            .create("CASH", "1.00", "EUR", AccountSide.DEBIT)
            .create("REVENUE", "1.00", "EUR", AccountSide.CREDIT)
            .build();
    Ledger again = new LedgerBuilder(replacement).name("books").options(options).build().init();

    assertThat(
            again.computeTrialBalance().getEntries().stream()
                .map(
                    entry ->
                        entry.getAccountRef()
                            + " "
                            + entry.getDebitTotal()
                            + " "
                            + entry.getCreditTotal())
                .toList())
        .containsExactly("CASH 1050.00 0", "REVENUE 0 50.00");
  }

  private static ConnectionOptions memoryDatabase(String name) {
    return new ConnectionOptions(DataSourceDriver.JDBC_H2)
        .url("jdbc:h2:mem:" + name + ";DB_CLOSE_DELAY=-1");
  }

  private static TransferRequest payment(Ledger ledger, String reference, String amount) {
    return ledger
        .createTransferRequest()
        .reference(reference)
        .type("payment")
        .account("CASH")
        .debit(amount, "EUR")
        .account("REVENUE")
        .credit(amount, "EUR")
        .build();
  }
}
