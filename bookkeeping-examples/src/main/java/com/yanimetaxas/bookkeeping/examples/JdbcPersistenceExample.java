package com.yanimetaxas.bookkeeping.examples;

import com.yanimetaxas.bookkeeping.ChartOfAccounts;
import com.yanimetaxas.bookkeeping.ChartOfAccounts.ChartOfAccountsBuilder;
import com.yanimetaxas.bookkeeping.Ledger;
import com.yanimetaxas.bookkeeping.Ledger.LedgerBuilder;
import com.yanimetaxas.bookkeeping.jdbc.ConnectionOptions;
import com.yanimetaxas.bookkeeping.model.AccountSide;
import lombok.extern.slf4j.Slf4j;

/**
 * Demonstrates JDBC persistence with an embedded H2 database, including ledger creation,
 * transactions, reset via {@code dropAll()}, and re-initialization.
 *
 * <p>Run: {@code mvn -pl bookkeeping-examples -am compile -Prun-example
 * -Dexec.mainClass=com.yanimetaxas.bookkeeping.examples.JdbcPersistenceExample}
 */
@Slf4j
public class JdbcPersistenceExample {

  public static void main(String... args) {
    ChartOfAccounts chart =
        new ChartOfAccountsBuilder()
            .create("Cash", "10000.00", "EUR", AccountSide.DEBIT)
            .create("Revenue", "0.00", "EUR", AccountSide.CREDIT)
            .create("Expenses", "0.00", "EUR", AccountSide.DEBIT)
            .create("Equity", "10000.00", "EUR", AccountSide.CREDIT)
            .build();

    Ledger ledger =
        new LedgerBuilder(chart)
            .name("JDBC H2 Ledger")
            .options(ConnectionOptions.EMBEDDED_H2_CONNECTION)
            .build()
            .init();

    ledger.commit(
        ledger
            .createTransferRequest()
            .reference("T1")
            .type("sale")
            .account("Cash")
            .debit("250.00", "EUR")
            .account("Revenue")
            .credit("250.00", "EUR")
            .build());

    ledger.commit(
        ledger
            .createTransferRequest()
            .reference("T2")
            .type("expense")
            .account("Cash")
            .debit("75.00", "EUR")
            .account("Expenses")
            .credit("75.00", "EUR")
            .build());

    log.info("=== JDBC Persistence Example ===");
    log.info("After 2 transactions:");
    ledger.printHistoryLog();
    ledger.printTrialBalance();

    log.info("--- After dropAll().init() ---");
    ledger.dropAll().init();

    log.info("Accounts after reset:");
    ledger.listAccounts().forEach(a -> log.info("  {}: {}", a.getAccountRef(), a.getBalance()));
    log.info("Transactions: {} (should be 0)", ledger.findTransactions("Cash").size());
  }
}
