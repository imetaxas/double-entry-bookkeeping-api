package com.yanimetaxas.bookkeeping.examples;

import com.yanimetaxas.bookkeeping.ChartOfAccounts;
import com.yanimetaxas.bookkeeping.ChartOfAccounts.ChartOfAccountsBuilder;
import com.yanimetaxas.bookkeeping.Ledger;
import com.yanimetaxas.bookkeeping.Ledger.LedgerBuilder;
import com.yanimetaxas.bookkeeping.memory.InMemoryStorageProvider;
import com.yanimetaxas.bookkeeping.model.AccountSide;
import com.yanimetaxas.bookkeeping.model.Money;
import lombok.extern.slf4j.Slf4j;

/**
 * Reopens one ledger name without replacing stored balances, and keeps a second name on the same
 * provider isolated. {@code includeExisted} refers to accounts already stored and does not seed
 * them again.
 *
 * <p>Run: {@code mvn -pl bookkeeping-examples -am compile -Prun-example
 * -Dexec.mainClass=com.yanimetaxas.bookkeeping.examples.ReopenLedgerExample}
 */
@Slf4j
public class ReopenLedgerExample {

  public static void main(String... args) {
    InMemoryStorageProvider provider = new InMemoryStorageProvider();
    Ledger shop =
        new LedgerBuilder(openingChart()).name("shop").storage(provider).build().init();
    shop.commit(
        shop.createTransferRequest()
            .reference("SALE")
            .type("sale")
            .account("Cash")
            .debit("50.00", "EUR")
            .account("Revenue")
            .credit("50.00", "EUR")
            .build());

    Ledger reopened =
        new LedgerBuilder(reopenedChart()).name("shop").storage(provider).build().init();
    expect(reopened.getAccountBalance("Cash"), "950.00", "EUR");
    reopened.commit(
        reopened
            .createTransferRequest()
            .reference("RENT")
            .type("expense")
            .account("Cash")
            .debit("100.00", "EUR")
            .account("Rent")
            .credit("100.00", "EUR")
            .build());
    expect(reopened.getAccountBalance("Cash"), "850.00", "EUR");
    expect(reopened.getAccountBalance("Rent"), "100.00", "EUR");

    Ledger warehouse =
        new LedgerBuilder(openingChart()).name("warehouse").storage(provider).build().init();
    expect(warehouse.getAccountBalance("Cash"), "1000.00", "EUR");
    if (warehouse.getTransactionByRef("SALE") != null) {
      throw new IllegalStateException("warehouse saw the shop transaction");
    }

    log.info("=== Reopened shop and a separate warehouse ===");
    reopened.printHistoryLog();
    warehouse.printHistoryLog();
  }

  private static ChartOfAccounts openingChart() {
    return new ChartOfAccountsBuilder()
        .create("Cash", "1000.00", "EUR", AccountSide.DEBIT)
        .create("Revenue", "0.00", "EUR", AccountSide.CREDIT)
        .create("Equity", "1000.00", "EUR", AccountSide.CREDIT)
        .build();
  }

  private static ChartOfAccounts reopenedChart() {
    return new ChartOfAccountsBuilder()
        .includeExisted("Cash")
        .includeExisted("Revenue")
        .includeExisted("Equity")
        .create("Rent", "0.00", "EUR", AccountSide.DEBIT)
        .build();
  }

  private static void expect(Money actual, String amount, String currency) {
    Money expected = Money.toMoney(amount, currency);
    if (!actual.equals(expected)) {
      throw new IllegalStateException("Expected " + expected + " but was " + actual);
    }
  }
}
