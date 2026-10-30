package com.yanimetaxas.bookkeeping.examples;

import com.yanimetaxas.bookkeeping.ChartOfAccounts;
import com.yanimetaxas.bookkeeping.ChartOfAccounts.ChartOfAccountsBuilder;
import com.yanimetaxas.bookkeeping.Ledger;
import com.yanimetaxas.bookkeeping.Ledger.LedgerBuilder;
import com.yanimetaxas.bookkeeping.exception.InsufficientFundsException;
import com.yanimetaxas.bookkeeping.exception.UnbalancedLegsException;
import com.yanimetaxas.bookkeeping.model.AccountSide;
import com.yanimetaxas.bookkeeping.model.Money;
import com.yanimetaxas.bookkeeping.model.TransferRequest;
import com.yanimetaxas.bookkeeping.validation.TransferValidationException;
import lombok.extern.slf4j.Slf4j;

/**
 * Shows three rejected postings. Each one leaves the stored balance and the transaction history
 * unchanged, and a later valid posting still succeeds.
 *
 * <p>Run: {@code mvn -pl bookkeeping-examples -am compile -Prun-example
 * -Dexec.mainClass=com.yanimetaxas.bookkeeping.examples.RejectedTransferExample}
 */
@Slf4j
public class RejectedTransferExample {

  public static void main(String... args) {
    ChartOfAccounts chart =
        new ChartOfAccountsBuilder()
            .create("Cash", "100.00", "EUR", AccountSide.DEBIT)
            .create("Revenue", "0.00", "EUR", AccountSide.CREDIT)
            .create("Equity", "100.00", "EUR", AccountSide.CREDIT)
            .build();
    Ledger ledger = new LedgerBuilder(chart).name("Rejections").build().init();

    reject(
        ledger,
        InsufficientFundsException.class,
        ledger
            .createTransferRequest()
            .reference("OVERDRAFT")
            .type("sale")
            .account("Cash")
            .debit("150.00", "EUR")
            .account("Revenue")
            .credit("150.00", "EUR")
            .build());
    reject(
        ledger,
        UnbalancedLegsException.class,
        ledger
            .createTransferRequest()
            .reference("UNBALANCED")
            .type("sale")
            .account("Cash")
            .debit("10.00", "EUR")
            .account("Revenue")
            .credit("20.00", "EUR")
            .build());
    reject(
        ledger,
        TransferValidationException.class,
        ledger
            .createTransferRequest()
            .reference("CURRENCY")
            .type("sale")
            .account("Cash")
            .debit("10.00", "USD")
            .account("Revenue")
            .credit("10.00", "USD")
            .build());

    expect(ledger.getAccountBalance("Cash"), "100.00", "EUR");
    if (!ledger.findTransactions("Cash").isEmpty()) {
      throw new IllegalStateException("Rejected transfers were stored");
    }

    ledger.commit(
        ledger
            .createTransferRequest()
            .reference("OK")
            .type("sale")
            .account("Cash")
            .debit("10.00", "EUR")
            .account("Revenue")
            .credit("10.00", "EUR")
            .build());
    expect(ledger.getAccountBalance("Cash"), "90.00", "EUR");
    log.info("=== Rejected transfers left Cash at {} ===", ledger.getAccountBalance("Cash"));
    ledger.printHistoryLog();
  }

  private static void reject(Ledger ledger, Class<? extends RuntimeException> type, TransferRequest request) {
    try {
      ledger.commit(request);
      throw new IllegalStateException(request.getTransactionRef() + " was accepted");
    } catch (RuntimeException exception) {
      if (!type.isInstance(exception)) {
        throw exception;
      }
      log.info("Rejected {}: {}", request.getTransactionRef(), exception.getMessage());
    }
  }

  private static void expect(Money actual, String amount, String currency) {
    Money expected = Money.toMoney(amount, currency);
    if (!actual.equals(expected)) {
      throw new IllegalStateException("Expected " + expected + " but was " + actual);
    }
  }
}
