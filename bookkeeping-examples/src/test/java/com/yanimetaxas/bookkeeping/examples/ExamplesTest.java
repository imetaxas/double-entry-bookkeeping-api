package com.yanimetaxas.bookkeeping.examples;

import static io.github.imetaxas.realitycheck.RealityAssertions.assertThat;
import static io.github.imetaxas.realitycheck.RealityAssertions.assertThatCode;

import com.yanimetaxas.bookkeeping.ChartOfAccounts;
import com.yanimetaxas.bookkeeping.ChartOfAccounts.ChartOfAccountsBuilder;
import com.yanimetaxas.bookkeeping.Ledger;
import com.yanimetaxas.bookkeeping.Ledger.LedgerBuilder;
import com.yanimetaxas.bookkeeping.model.AccountSide;
import com.yanimetaxas.bookkeeping.model.Money;
import com.yanimetaxas.bookkeeping.model.TrialBalance;
import org.junit.jupiter.api.Test;

class ExamplesTest {

  @Test
  void ecommerceScenarioProducesBalancedTrialBalance() {
    ChartOfAccounts chart =
        new ChartOfAccountsBuilder()
            .create("Cash", "10000.00", "EUR", AccountSide.DEBIT)
            .create("Inventory", "5000.00", "EUR", AccountSide.DEBIT)
            .create("Revenue", "0.00", "EUR", AccountSide.CREDIT)
            .create("CostOfGoodsSold", "0.00", "EUR", AccountSide.DEBIT)
            .create("OwnersEquity", "15000.00", "EUR", AccountSide.CREDIT)
            .build();

    Ledger ledger = new LedgerBuilder(chart).name("E-Commerce Test").build().init();

    ledger.commit(
        ledger
            .createTransferRequest()
            .reference("SALE-1")
            .type("sale")
            .account("Cash")
            .debit("200.00", "EUR")
            .account("Revenue")
            .credit("200.00", "EUR")
            .build());

    ledger.commit(
        ledger
            .createTransferRequest()
            .reference("COGS-1")
            .type("cost-of-goods")
            .account("Inventory")
            .debit("120.00", "EUR")
            .account("CostOfGoodsSold")
            .credit("120.00", "EUR")
            .build());

    TrialBalance tb = ledger.computeTrialBalance();
    assertThat(tb.isBalanced()).isTrue();
    assertThat(ledger.getAccountBalance("Cash")).isEqualTo(Money.toMoney("9800.00", "EUR"));
    assertThat(ledger.getAccountBalance("Revenue")).isEqualTo(Money.toMoney("200.00", "EUR"));
  }

  @Test
  void expenseTrackerFlowCompletes() {
    ChartOfAccounts chart =
        new ChartOfAccountsBuilder()
            .create("BankAccount", "8000.00", "USD", AccountSide.DEBIT)
            .create("Groceries", "0.00", "USD", AccountSide.DEBIT)
            .create("Rent", "0.00", "USD", AccountSide.DEBIT)
            .create("OpeningBalance", "8000.00", "USD", AccountSide.CREDIT)
            .build();

    Ledger ledger = new LedgerBuilder(chart).name("Expense Test").build().init();

    ledger.commit(
        ledger
            .createTransferRequest()
            .reference("RENT-1")
            .type("expense")
            .account("BankAccount")
            .debit("1200.00", "USD")
            .account("Rent")
            .credit("1200.00", "USD")
            .build());

    ledger.commit(
        ledger
            .createTransferRequest()
            .reference("GRO-1")
            .type("expense")
            .account("BankAccount")
            .debit("85.50", "USD")
            .account("Groceries")
            .credit("85.50", "USD")
            .build());

    assertThat(ledger.getAccountBalance("BankAccount")).isEqualTo(Money.toMoney("6714.50", "USD"));
    assertThat(ledger.computeTrialBalance().isBalanced()).isTrue();
  }

  @Test
  void multiCurrencyLedgersAreIndependent() {
    Ledger eurLedger =
        new LedgerBuilder(
                new ChartOfAccountsBuilder()
                    .create("EUR:Cash", "10000.00", "EUR", AccountSide.DEBIT)
                    .create("EUR:FXOutflow", "0.00", "EUR", AccountSide.CREDIT)
                    .create("EUR:Equity", "10000.00", "EUR", AccountSide.CREDIT)
                    .build())
            .name("EUR")
            .build()
            .init();

    Ledger usdLedger =
        new LedgerBuilder(
                new ChartOfAccountsBuilder()
                    .create("USD:Cash", "5000.00", "USD", AccountSide.DEBIT)
                    .create("USD:FXOutflow", "0.00", "USD", AccountSide.CREDIT)
                    .create("USD:Equity", "5000.00", "USD", AccountSide.CREDIT)
                    .build())
            .name("USD")
            .build()
            .init();

    eurLedger.commit(
        eurLedger
            .createTransferRequest()
            .reference("FX-1-EUR")
            .type("fx-sell")
            .account("EUR:Cash")
            .debit("1850.00", "EUR")
            .account("EUR:FXOutflow")
            .credit("1850.00", "EUR")
            .build());

    usdLedger.commit(
        usdLedger
            .createTransferRequest()
            .reference("FX-1-USD")
            .type("fx-buy")
            .account("USD:Cash")
            .debit("2000.00", "USD")
            .account("USD:FXOutflow")
            .credit("2000.00", "USD")
            .build());

    assertThat(eurLedger.computeTrialBalance().isBalanced()).isTrue();
    assertThat(usdLedger.computeTrialBalance().isBalanced()).isTrue();
    assertThat(eurLedger.getAccountBalance("EUR:Cash")).isEqualTo(Money.toMoney("8150.00", "EUR"));
    assertThat(usdLedger.getAccountBalance("USD:Cash")).isEqualTo(Money.toMoney("3000.00", "USD"));
  }

  @Test
  void allExampleMainMethodsRunWithoutException() {
    assertThatCode(() -> ECommerceLedgerExample.main()).doesNotThrow();
    assertThatCode(() -> ExpenseTrackerExample.main()).doesNotThrow();
    assertThatCode(() -> MultiCurrencyFXExample.main()).doesNotThrow();
    assertThatCode(() -> JdbcPersistenceExample.main()).doesNotThrow();
    assertThatCode(() -> SpringBootLedgerExample.main()).doesNotThrow();
    assertThatCode(() -> SqliteFileLedgerExample.main()).doesNotThrow();
    assertThatCode(() -> RejectedTransferExample.main()).doesNotThrow();
    assertThatCode(() -> ReopenLedgerExample.main()).doesNotThrow();
    assertThatCode(() -> FixedClockExample.main()).doesNotThrow();
    assertThatCode(() -> MultiCurrencyTransferExample.main()).doesNotThrow();
    assertThatCode(() -> SupportedDatabasesExample.main()).doesNotThrow();
  }
}
