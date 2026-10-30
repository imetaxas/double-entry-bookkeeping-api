package com.yanimetaxas.bookkeeping;

import static io.github.imetaxas.realitycheck.RealityAssertions.assertThat;

import com.yanimetaxas.bookkeeping.ChartOfAccounts.ChartOfAccountsBuilder;
import com.yanimetaxas.bookkeeping.Ledger.LedgerBuilder;
import com.yanimetaxas.bookkeeping.model.Account;
import com.yanimetaxas.bookkeeping.model.AccountSide;
import com.yanimetaxas.bookkeeping.model.Money;
import com.yanimetaxas.bookkeeping.model.Transaction;
import com.yanimetaxas.bookkeeping.model.TransactionLeg;
import com.yanimetaxas.bookkeeping.model.TrialBalance;
import com.yanimetaxas.bookkeeping.model.TrialBalanceEntry;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Currency;
import java.util.List;
import org.junit.jupiter.api.Test;

class TrialBalanceTest {

  private static final Currency EUR = Currency.getInstance("EUR");

  @Test
  void saleIncreasesCashDebitAndRevenueCredit() {
    ChartOfAccounts chart =
        new ChartOfAccountsBuilder()
            .create("CASH", "1000.00", "EUR", AccountSide.DEBIT)
            .create("EQUITY", "1000.00", "EUR", AccountSide.CREDIT)
            .create("REVENUE", "0.00", "EUR", AccountSide.CREDIT)
            .build();
    Ledger ledger = new LedgerBuilder(chart).build().init();
    ledger.commit(
        ledger
            .createTransferRequest()
            .reference("T1")
            .type("sale")
            .account("CASH")
            .debit("200.00", "EUR")
            .account("REVENUE")
            .credit("200.00", "EUR")
            .build());

    TrialBalance trialBalance = ledger.computeTrialBalance();

    assertThat(trialBalance.isBalanced()).isTrue();
    assertThat(trialBalance.toString()).contains("Status: BALANCED");
    assertThat(amounts(trialBalance))
        .containsExactlyInAnyOrder("CASH 1200.00 0", "EQUITY 0 1000.00", "REVENUE 0 200.00");
  }

  @Test
  void negativeOpeningWithoutNormalSideIsADebit() {
    TrialBalance trialBalance =
        TrialBalance.compute(
            List.of(
                new Account("CASH", Money.toMoney("-40.00", "EUR")),
                new Account("REVENUE", Money.toMoney("40.00", "EUR"))),
            List.of());

    assertThat(trialBalance.isBalanced()).isTrue();
    assertThat(amounts(trialBalance))
        .containsExactlyInAnyOrder("CASH 40.00 0", "REVENUE 0 40.00");
  }

  @Test
  void cashAndRevenueOpeningIsUnbalanced() {
    ChartOfAccounts chart =
        new ChartOfAccountsBuilder()
            .create("CASH", "1000.00", "EUR", AccountSide.DEBIT)
            .create("REVENUE", "0.00", "EUR", AccountSide.CREDIT)
            .build();

    TrialBalance trialBalance = new LedgerBuilder(chart).build().init().computeTrialBalance();

    assertThat(trialBalance.isBalanced()).isFalse();
    assertThat(trialBalance.toString()).contains("UNBALANCED");
  }

  @Test
  void matchingOpeningDebitAndCreditAreBalanced() {
    ChartOfAccounts chart =
        new ChartOfAccountsBuilder()
            .create("CASH", "1000.00", "EUR", AccountSide.DEBIT)
            .create("EQUITY", "1000.00", "EUR", AccountSide.CREDIT)
            .build();

    TrialBalance trialBalance = new LedgerBuilder(chart).build().init().computeTrialBalance();

    assertThat(trialBalance.isBalanced()).isTrue();
  }

  @Test
  void aLegForAnUnknownAccountIsIgnored() {
    TrialBalance trialBalance =
        TrialBalance.compute(
            List.of(new Account("CASH", Money.toMoney("0.00", "EUR"), AccountSide.DEBIT)),
            List.of(
                new Transaction(
                    "T1",
                    "sale",
                    Instant.EPOCH,
                    List.of(
                        new TransactionLeg("OTHER", Money.toMoney("-5.00", "EUR")),
                        new TransactionLeg("CASH", Money.toMoney("5.00", "EUR"))))));

    assertThat(
            trialBalance.getEntries().stream()
                .map(entry -> entry.getAccountRef() + " " + entry.getCreditTotal())
                .toList())
        .containsExactly("CASH 5.00");
  }

  @Test
  void negativeDebitNormalOpeningIsReportedAsACredit() {
    TrialBalance trialBalance =
        TrialBalance.compute(
            List.of(
                new Account("CASH", Money.toMoney("-40.00", "EUR"), AccountSide.DEBIT),
                new Account("REVENUE", Money.toMoney("-40.00", "EUR"), AccountSide.CREDIT)),
            List.of());

    assertThat(trialBalance.isBalanced()).isTrue();
    assertThat(trialBalance.getTimestamp()).isNotNull();
    assertThat(trialBalance.toString()).contains("CASH").contains("TOTAL");
    assertThat(
            trialBalance.getEntries().stream()
                .map(
                    entry ->
                        entry.getAccountRef()
                            + " "
                            + entry.getDebitTotal()
                            + " "
                            + entry.getCreditTotal()
                            + " "
                            + entry.getNormalBalance()
                            + " "
                            + entry.getCurrency())
                .toList())
        .containsExactlyInAnyOrder("CASH 0 40.00 DEBIT EUR", "REVENUE 40.00 0 CREDIT EUR");
  }

  @Test
  void reportPrintsZeroAsPlainText() {
    TrialBalance trialBalance =
        TrialBalance.compute(
            List.of(new Account("CASH", new Money(BigDecimal.ZERO, EUR), AccountSide.DEBIT)),
            List.of(),
            Clock.fixed(Instant.EPOCH, ZoneOffset.UTC));

    assertThat(trialBalance.toString())
        .matches("(?s).*\\| CASH\\s+\\|\\s+0 \\|\\s+0 \\|.*")
        .doesNotContain("0.00");
  }

  @Test
  void reportPrintsANegativeScaleAmountAsPlainText() {
    TrialBalance trialBalance =
        TrialBalance.compute(
            List.of(
                new Account(
                    "WIDE", new Money(new BigDecimal("1E+2"), EUR), AccountSide.CREDIT)),
            List.of(),
            Clock.fixed(Instant.EPOCH, ZoneOffset.UTC));

    assertThat(trialBalance.toString())
        .matches("(?s).*\\| WIDE\\s+\\|\\s+0 \\|\\s+100 \\|.*")
        .doesNotContain("0.00")
        .doesNotContain("100.00");
  }

  private static List<String> amounts(TrialBalance trialBalance) {
    return trialBalance.getEntries().stream()
        .map(
            entry ->
                entry.getAccountRef()
                    + " "
                    + entry.getDebitTotal()
                    + " "
                    + entry.getCreditTotal())
        .toList();
  }
}
