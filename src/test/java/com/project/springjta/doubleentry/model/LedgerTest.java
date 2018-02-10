package com.project.springjta.doubleentry.model;

import static org.junit.Assert.assertEquals;

import com.project.springjta.doubleentry.Money;
import com.project.springjta.doubleentry.Transaction;
import com.project.springjta.doubleentry.TransferRequest;
import java.math.BigDecimal;
import java.util.Currency;
import java.util.List;
import org.junit.Test;

/**
 * @author yanimetaxas
 * @since 03-Feb-18
 */
public class LedgerTest {

  private static final String CASH_ACCOUNT_1 = "cash_1_EUR   ";
  private static final String CASH_ACCOUNT_2 = "cash_2_SEK   ";
  private static final String REVENUE_ACCOUNT_1 = "revenue_1_EUR";
  private static final String REVENUE_ACCOUNT_2 = "revenue_2_SEK";

  @Test
  public void accountBalancesUpdatedAfterTransfer() {
    ChartOfAccounts chartOfAccounts = ChartOfAccountsBuilder.create()
        .account(CASH_ACCOUNT_1, "1000.00", "EUR")
        .account(REVENUE_ACCOUNT_1, "0.00", "EUR")
        .build();

    Ledger ledger = new Ledger(chartOfAccounts);

    TransferRequest transferRequest1 = ledger.createTransferRequest()
        .reference("T1")
        .type("testing1")
        .account(CASH_ACCOUNT_1).debit("5.00", "EUR")
        .account(REVENUE_ACCOUNT_1).credit("5.00", "EUR")
        .build();

    ledger.commitTransaction(transferRequest1);

    TransferRequest transferRequest2 = ledger.createTransferRequest()
        .reference("T2")
        .type("testing2")
        .account(CASH_ACCOUNT_1).debit("10.50", "EUR")
        .account(REVENUE_ACCOUNT_1).credit("10.50", "EUR")
        .build();

    ledger.commitTransaction(transferRequest2);

    assertEquals(toMoney("984.50", "EUR"), ledger.getAccountBalance(CASH_ACCOUNT_1));
    assertEquals(toMoney("15.50", "EUR"), ledger.getAccountBalance(REVENUE_ACCOUNT_1));

    List<Transaction> cashAccountTransactionList = ledger.findTransactionsByAccountRef(CASH_ACCOUNT_1);
    List<Transaction> revenueAccountTransactionList = ledger.findTransactionsByAccountRef(REVENUE_ACCOUNT_1);

    assertEquals(2, cashAccountTransactionList.size());
    assertEquals(2, revenueAccountTransactionList.size());

    System.out.println(ledger.printHistoryLog());
  }

  private static Money toMoney(String amount, String currency) {
    return new Money(new BigDecimal(amount), Currency.getInstance(currency));
  }
}