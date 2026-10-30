package com.yanimetaxas.bookkeeping.examples;

import com.yanimetaxas.bookkeeping.ChartOfAccounts;
import com.yanimetaxas.bookkeeping.ChartOfAccounts.ChartOfAccountsBuilder;
import com.yanimetaxas.bookkeeping.Ledger;
import com.yanimetaxas.bookkeeping.Ledger.LedgerBuilder;
import com.yanimetaxas.bookkeeping.model.AccountSide;
import com.yanimetaxas.bookkeeping.model.Money;
import lombok.extern.slf4j.Slf4j;

/**
 * Posts EUR and SEK in one transfer. Each currency balances on its own, so the signed amounts of
 * one currency do not have to offset the other.
 *
 * <p>Run: {@code mvn -pl bookkeeping-examples -am compile -Prun-example
 * -Dexec.mainClass=com.yanimetaxas.bookkeeping.examples.MultiCurrencyTransferExample}
 */
@Slf4j
public class MultiCurrencyTransferExample {

  public static void main(String... args) {
    ChartOfAccounts chart =
        new ChartOfAccountsBuilder()
            .create("CashEUR", "1000.00", "EUR", AccountSide.DEBIT)
            .create("RevenueEUR", "0.00", "EUR", AccountSide.CREDIT)
            .create("EquityEUR", "1000.00", "EUR", AccountSide.CREDIT)
            .create("CashSEK", "5000.00", "SEK", AccountSide.DEBIT)
            .create("RevenueSEK", "0.00", "SEK", AccountSide.CREDIT)
            .create("EquitySEK", "5000.00", "SEK", AccountSide.CREDIT)
            .build();
    Ledger ledger = new LedgerBuilder(chart).name("Mixed Currency").build().init();
    ledger.commit(
        ledger
            .createTransferRequest()
            .reference("MC1")
            .type("sale")
            .account("CashEUR")
            .debit("100.00", "EUR")
            .account("RevenueEUR")
            .credit("100.00", "EUR")
            .account("CashSEK")
            .debit("500.00", "SEK")
            .account("RevenueSEK")
            .credit("500.00", "SEK")
            .build());

    expect(ledger.getAccountBalance("CashEUR"), "900.00", "EUR");
    expect(ledger.getAccountBalance("CashSEK"), "4500.00", "SEK");
    if (!ledger.computeTrialBalance().isBalanced()) {
      throw new IllegalStateException("Trial balance did not balance");
    }
    log.info("=== One transfer, two currencies ===");
    ledger.printHistoryLog();
    ledger.printTrialBalance();
  }

  private static void expect(Money actual, String amount, String currency) {
    Money expected = Money.toMoney(amount, currency);
    if (!actual.equals(expected)) {
      throw new IllegalStateException("Expected " + expected + " but was " + actual);
    }
  }
}
