package com.yanimetaxas.bookkeeping.model;

import static com.yanimetaxas.bookkeeping.ConnectionOptions.EMBEDDED_DERBY_CONNECTION;
import static com.yanimetaxas.bookkeeping.ConnectionOptions.EMBEDDED_HSQL_CONNECTION;
import static com.yanimetaxas.bookkeeping.DataSourceDriver.JDBC_H2;
import static com.yanimetaxas.realitycheck.Reality.checkThat;

import com.yanimetaxas.bookkeeping.ChartOfAccounts;
import com.yanimetaxas.bookkeeping.ChartOfAccounts.ChartOfAccountsBuilder;
import com.yanimetaxas.bookkeeping.ConnectionOptions;
import com.yanimetaxas.bookkeeping.Ledger;
import com.yanimetaxas.bookkeeping.Ledger.LedgerBuilder;
import com.yanimetaxas.bookkeeping.exception.InfrastructureException;
import com.yanimetaxas.bookkeeping.exception.LedgerAccountException;
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

  @Test
  public void accountBalancesUpdatedAfterTransferUsingEmbeddedHSQL() {
    ChartOfAccounts chartOfAccounts = new ChartOfAccountsBuilder()
        .create(CASH_ACCOUNT_1, "1000.00", "EUR")
        .create(REVENUE_ACCOUNT_1, "0.00", "EUR")
        .build();

    Ledger ledger = new LedgerBuilder(chartOfAccounts)
        .name("Embedded HSQL")
        .options(EMBEDDED_HSQL_CONNECTION)
        .build()
        .init();

    checkThat(ledger.getAccountBalance(CASH_ACCOUNT_1)).isEqualTo(Money.toMoney("1000.00", "EUR"));
    checkThat(ledger.getAccountBalance(REVENUE_ACCOUNT_1)).isEqualTo(Money.toMoney("0.00", "EUR"));

    ledger.printHistoryLog();
  }

  @Test
  public void accountBalancesUpdatedAfterTransferUsingEmbeddedDerby() {
    ChartOfAccounts chartOfAccounts = new ChartOfAccountsBuilder()
        .create(CASH_ACCOUNT_1, "1000.00", "EUR")
        .create(REVENUE_ACCOUNT_1, "0.00", "EUR")
        .build();

    Ledger ledger = new LedgerBuilder(chartOfAccounts)
        .name("Embedded Derby")
        .options(EMBEDDED_DERBY_CONNECTION)
        .build()
        .init();

    checkThat(ledger.getAccountBalance(CASH_ACCOUNT_1)).isEqualTo(Money.toMoney("1000.00", "EUR"));
    checkThat(ledger.getAccountBalance(REVENUE_ACCOUNT_1)).isEqualTo(Money.toMoney("0.00", "EUR"));

    ledger.printHistoryLog();
  }

  @Test
  public void accountBalancesUpdatedAfterTransferUsingH2() {
    ConnectionOptions options = new ConnectionOptions(JDBC_H2)
        .url("jdbc:h2:~/test")
        .username("")
        .password("");

    ChartOfAccounts chartOfAccounts = new ChartOfAccountsBuilder()
        .create(CASH_ACCOUNT_1, "1000.00", "EUR")
        .create(REVENUE_ACCOUNT_1, "0.00", "EUR")
        .build();

    Ledger ledger = new LedgerBuilder(chartOfAccounts)
        .name("JDBC H2")
        .options(options)
        .build()
        .init();

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
  public void accountBalancesUpdatedAfterMultiLeggedTransferUsingEmbeddedH2() {
    ChartOfAccounts chartOfAccounts = new ChartOfAccountsBuilder()
        .create(CASH_ACCOUNT_1, "1000.00", "EUR")
        .create(REVENUE_ACCOUNT_1, "0.00", "EUR")
        .build();

    Ledger ledger = new LedgerBuilder(chartOfAccounts)
        .name("Embedded H2")
        .build()
        .init();

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

    ledger.printHistoryLog();
  }

  @Test
  public void accountBalancesUpdatedAfterMultiLeggedMultiCurrencyTransferUsingEmbeddedH2() {
    ChartOfAccounts chartOfAccounts = new ChartOfAccountsBuilder()
        .create(CASH_ACCOUNT_1, "1000.00", "EUR")
        .create(REVENUE_ACCOUNT_1, "0.00", "EUR")
        .create(CASH_ACCOUNT_2, "1000.00", "SEK")
        .create(REVENUE_ACCOUNT_2, "0.00", "SEK")
        .build();

    Ledger ledger = new LedgerBuilder(chartOfAccounts)
        .name("Embedded H2")
        .build()
        .init();

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

    ledger.printHistoryLog();
  }

  @Test
  public void includeAlreadyExistedAccountBalanceUsingEmbeddedH2() {
    ChartOfAccounts chartOfAccounts = new ChartOfAccountsBuilder()
        .create(CASH_ACCOUNT_1, "1000.00", "EUR")
        .create(REVENUE_ACCOUNT_1, "0.00", "EUR")
        .includeExisted(CASH_ACCOUNT_1)
        .build();

    Ledger ledger = new LedgerBuilder(chartOfAccounts)
        .name("Embedded H2")
        .build()
        .init();

    TransferRequest transferRequest1 = ledger.createTransferRequest()
        .reference("TR")
        .type("testing1")
        .account(CASH_ACCOUNT_1).debit("5.00", "EUR")
        .account(REVENUE_ACCOUNT_1).credit("5.00", "EUR")
        .build();

    ledger.commit(transferRequest1);

    ledger.findTransactions(CASH_ACCOUNT_1);
    ledger.findTransactions(REVENUE_ACCOUNT_1);

    ledger.getTransactionByRef("TR");

    ledger.printHistoryLog();

  }

  @Test(expected = LedgerAccountException.class)
  public void commitWhenAccountNotExistsInLedgerUsingEmbeddedH2() {
    ChartOfAccounts chartOfAccounts = new ChartOfAccountsBuilder()
        .create(CASH_ACCOUNT_1, "1000.00", "EUR")
        .create(REVENUE_ACCOUNT_1, "0.00", "EUR")
        .build();

    Ledger ledger = new LedgerBuilder(chartOfAccounts)
        .name("Embedded H2")
        .build()
        .init();

    TransferRequest transferRequest1 = ledger.createTransferRequest()
        .reference("TR")
        .type("testing1")
        .account("account11").debit("5.00", "EUR")
        .account(REVENUE_ACCOUNT_1).credit("5.00", "EUR")
        .build();

    ledger.commit(transferRequest1);
  }

  /*@Test
  public void accountBalancesUpdatedAfterTransferUsingJdbcMysql() {
    try {
      ConnectionOptions options = new ConnectionOptions(JDBC_MYSQL)
          .url("jdbc:mysql://localhost:3306/test?createDatabaseIfNotExist=true")
          .username("root")
          .password("");

      ChartOfAccounts chartOfAccounts = new ChartOfAccountsBuilder()
          .create("account1", "1000.00", "EUR")
          .create("account2", "0.00", "EUR")
          .create("account3", "0.00", "EUR")
          .build();

      Ledger ledger = new LedgerBuilder(chartOfAccounts)
          .name("JDBC MYSQL")
          .options(options)
          .build()
          .init();

      TransferRequest transferRequest1 = ledger.createTransferRequest()
          .reference("TR")
          .type("testing1")
          .account("account1").debit("5.00", "EUR")
          .account("account2").credit("5.00", "EUR")
          .build();

      ledger.commit(transferRequest1);

      ledger.findTransactions("account1");
      ledger.findTransactions("account2");

      ledger.getTransactionByRef("TR");

      ledger.printHistoryLog();

      TransferRequest transferRequest2 = ledger.createTransferRequest()
          .reference("TR2")
          .type("testing1")
          .account("account1").debit("5.00", "EUR")
          .account("account3").credit("5.00", "EUR")
          .build();

      ledger.commit(transferRequest2);

      ledger.findTransactions("account1");
      ledger.findTransactions("account3");

      ledger.getTransactionByRef("TR2");

      ledger.printHistoryLog();


      ChartOfAccounts chartOfAccounts2 = new ChartOfAccountsBuilder()
          .includeExisted("account1")
          .includeExisted("account2")
          .includeExisted("account3")
          .build();

      ledger = new LedgerBuilder(chartOfAccounts2)
          .name("JDBC MYSQL")
          .options(options)
          .build()
          .init();

      TransferRequest transferRequest3 = ledger.createTransferRequest()
          .reference("TR3")
          .type("testing1")
          .account("account2").debit("5.00", "EUR")
          .account("account3").credit("5.00", "EUR")
          .build();
      ledger.commit(transferRequest3);

      ledger.printHistoryLog();

    } catch (Exception e) {
      System.out.println("\nCaught: " + e.getMessage());
    }
  }*/

  /*@Test
  public void accountBalancesUpdatedAfterTransferUsingJdbcPostgres() {
    ConnectionOptions options = new ConnectionOptions(JDBC_POSTGRES)
        .url("jdbc:postgresql://localhost:5432/test")
        .username("postgres")
        .password("pass");

    ChartOfAccounts chartOfAccounts = new ChartOfAccountsBuilder()
        .create(CASH_ACCOUNT_1, "1000.00", "EUR")
        .create(REVENUE_ACCOUNT_1, "0.00", "EUR")
        .build();

    Ledger ledger = new LedgerBuilder(chartOfAccounts)
        .name("JDBC POSTGRES")
        .options(options)
        .build()
        .init();

    ledger.printHistoryLog();
  }*/

  @Test(expected = IllegalArgumentException.class)
  public void buildChartOfAccountsWhenHavingNoAccounts() {
    new ChartOfAccountsBuilder().build();
  }

  @Test(expected = InfrastructureException.class)
  public void createLedgerWhenIsInvalidAccountingImplementation() {
    new LedgerMock();
  }

}