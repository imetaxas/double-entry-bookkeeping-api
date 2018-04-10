package com.yanimetaxas.doubleentry.model;

import static com.yanimetaxas.doubleentry.DataSourceDriver.JDBC_H2;
import static com.yanimetaxas.realitycheck.Reality.checkThat;

import com.yanimetaxas.doubleentry.InfrastructureException;
import com.yanimetaxas.doubleentry.Money;
import com.yanimetaxas.doubleentry.Transaction;
import com.yanimetaxas.doubleentry.TransferRequest;
import java.util.List;
import org.junit.Test;

/**
 * @author yanimetaxas
 * @since 03-Feb-18
 */
public class LedgerTest {

  private static final String CASH_ACCOUNT_1 = "cash_1_EUR";
  private static final String CASH_ACCOUNT_2 = "cash_2_SEK";
  private static final String REVENUE_ACCOUNT_1 = "revenue_1_EUR";
  private static final String REVENUE_ACCOUNT_2 = "revenue_2_SEK";

  private static final String URL = "jdbc:h2:~/test";
  private static final String USERNAME = "";
  private static final String PASSWORD = "";

  @Test
  public void accountBalancesUpdatedAfterTransfer_UsingH2() {
    ConnectionOptions options = new ConnectionOptions(
        JDBC_H2.getDriverClassName(),
        URL,
        USERNAME,
        PASSWORD);

    ChartOfAccounts chartOfAccounts = ChartOfAccountsBuilder.create()
        .account(CASH_ACCOUNT_1, "1000.00", "EUR")
        .account(REVENUE_ACCOUNT_1, "0.00", "EUR")
        .build();

    Ledger ledger = new Ledger(chartOfAccounts, options);

    checkThat(ledger.getAccountBalance(CASH_ACCOUNT_1)).isEqualTo(Money.toMoney("1000.00", "EUR"));
    checkThat(ledger.getAccountBalance(REVENUE_ACCOUNT_1)).isEqualTo(Money.toMoney("0.00", "EUR"));

    TransferRequest transferRequest1 = ledger.createTransferRequest()
        .reference("T1")
        .type("testing1")
        .account(CASH_ACCOUNT_1).debit("5.00", "EUR")
        .account(REVENUE_ACCOUNT_1).credit("5.00", "EUR")
        .build();

    ledger.commit(transferRequest1);

    TransferRequest transferRequest2 = ledger.createTransferRequest()
        .reference("T2")
        .type("testing2")
        .account(CASH_ACCOUNT_1).debit("10.50", "EUR")
        .account(REVENUE_ACCOUNT_1).credit("10.50", "EUR")
        .build();

    ledger.commit(transferRequest2);

    checkThat(Money.toMoney("984.50", "EUR")).isEqualTo(ledger.getAccountBalance(CASH_ACCOUNT_1));
    checkThat(Money.toMoney("15.50", "EUR")).isEqualTo(ledger.getAccountBalance(REVENUE_ACCOUNT_1));

    List<Transaction> cashAccountTransactionList = ledger.findTransactions(CASH_ACCOUNT_1);
    List<Transaction> revenueAccountTransactionList = ledger.findTransactions(REVENUE_ACCOUNT_1);
    Transaction transaction1 = ledger.getTransactionByRef("T1");
    Transaction transaction2 = ledger.getTransactionByRef("T2");

    checkThat(cashAccountTransactionList.size()).isEqualTo(2);
    checkThat(revenueAccountTransactionList.size()).isEqualTo(2);
    checkThat(transaction1).isNotNull();
    checkThat(transaction2).isNotNull();

    ledger.printHistoryLog();
  }

  @Test
  public void accountBalancesUpdatedAfterMultiLeggedTransfer_InMemory() {
    ChartOfAccounts chartOfAccounts = ChartOfAccountsBuilder.create()
        .account(CASH_ACCOUNT_1, "1000.00", "EUR")
        .account(REVENUE_ACCOUNT_1, "0.00", "EUR")
        .build();

    Ledger ledger = new Ledger(chartOfAccounts);

    checkThat(ledger.getAccountBalance(CASH_ACCOUNT_1)).isEqualTo(Money.toMoney("1000.00", "EUR"));
    checkThat(ledger.getAccountBalance(REVENUE_ACCOUNT_1)).isEqualTo(Money.toMoney("0.00", "EUR"));

    TransferRequest transferRequest = ledger.createTransferRequest()
        .reference("T1")
        .type("testing")
        .account(CASH_ACCOUNT_1).amount(Money.toMoney("-5.00", "EUR"))
        .account(REVENUE_ACCOUNT_1).amount(Money.toMoney("5.00", "EUR"))
        .account(CASH_ACCOUNT_1).amount(Money.toMoney("-10.50", "EUR"))
        .account(REVENUE_ACCOUNT_1).amount(Money.toMoney("10.50", "EUR"))
        .account(CASH_ACCOUNT_1).amount(Money.toMoney("-2.00", "EUR"))
        .account(REVENUE_ACCOUNT_1).amount(Money.toMoney("1.00", "EUR"))
        .account(REVENUE_ACCOUNT_1).amount(Money.toMoney("1.00", "EUR"))
        .build();

    ledger.commit(transferRequest);

    checkThat(Money.toMoney("982.50", "EUR")).isEqualTo(ledger.getAccountBalance(CASH_ACCOUNT_1));
    checkThat(Money.toMoney("17.50", "EUR")).isEqualTo(ledger.getAccountBalance(REVENUE_ACCOUNT_1));
  }

  @Test
  public void accountBalancesUpdatedAfterMultiLeggedMultiCurrencyTransfer_InMemory() {
    ChartOfAccounts chartOfAccounts = ChartOfAccountsBuilder.create()
        .account(CASH_ACCOUNT_1, "1000.00", "EUR")
        .account(REVENUE_ACCOUNT_1, "0.00", "EUR")
        .account(CASH_ACCOUNT_2, "1000.00", "SEK")
        .account(REVENUE_ACCOUNT_2, "0.00", "SEK")
        .build();

    Ledger ledger = new Ledger(chartOfAccounts);

    checkThat(ledger.getAccountBalance(CASH_ACCOUNT_1)).isEqualTo(Money.toMoney("1000.00", "EUR"));
    checkThat(ledger.getAccountBalance(REVENUE_ACCOUNT_1)).isEqualTo(Money.toMoney("0.00", "EUR"));
    checkThat(ledger.getAccountBalance(CASH_ACCOUNT_2)).isEqualTo(Money.toMoney("1000.00", "SEK"));
    checkThat(ledger.getAccountBalance(REVENUE_ACCOUNT_2)).isEqualTo(Money.toMoney("0.00", "SEK"));

    TransferRequest transferRequest = ledger.createTransferRequest()
        .reference("T1")
        .type("testing")
        .account(CASH_ACCOUNT_1).amount(Money.toMoney("-5.00", "EUR"))
        .account(REVENUE_ACCOUNT_1).amount(Money.toMoney("5.00", "EUR"))
        .account(CASH_ACCOUNT_2).amount(Money.toMoney("-10.50", "SEK"))
        .account(REVENUE_ACCOUNT_2).amount(Money.toMoney("10.50", "SEK"))
        .build();

    ledger.commit(transferRequest);

    checkThat(Money.toMoney("995.00", "EUR")).isEqualTo(ledger.getAccountBalance(CASH_ACCOUNT_1));
    checkThat(Money.toMoney("5.00", "EUR")).isEqualTo(ledger.getAccountBalance(REVENUE_ACCOUNT_1));
    checkThat(Money.toMoney("989.50", "SEK")).isEqualTo(ledger.getAccountBalance(CASH_ACCOUNT_2));
    checkThat(Money.toMoney("10.50", "SEK")).isEqualTo(ledger.getAccountBalance(REVENUE_ACCOUNT_2));
  }

  @Test(expected = IllegalArgumentException.class)
  public void buildChartOfAccountsWhenHavingNoAccounts() {
    ChartOfAccountsBuilder.create().build();
  }

  @Test(expected = InfrastructureException.class)
  public void createLedgerWhenIsInvalidAccountingImplementation() {
    new LedgerMock();
  }

}