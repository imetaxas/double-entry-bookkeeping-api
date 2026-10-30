package com.yanimetaxas.bookkeeping;

import static io.github.imetaxas.realitycheck.RealityAssertions.assertThat;
import static io.github.imetaxas.realitycheck.RealityAssertions.assertThatThrownBy;

import com.yanimetaxas.bookkeeping.ChartOfAccounts.ChartOfAccountsBuilder;
import com.yanimetaxas.bookkeeping.exception.InsufficientFundsException;
import com.yanimetaxas.bookkeeping.exception.TransferRequestExistsException;
import com.yanimetaxas.bookkeeping.exception.UnbalancedLegsException;
import com.yanimetaxas.bookkeeping.model.AccountSide;
import com.yanimetaxas.bookkeeping.model.Money;
import com.yanimetaxas.bookkeeping.model.TransferRequest;
import com.yanimetaxas.bookkeeping.model.TrialBalance;
import com.yanimetaxas.bookkeeping.model.TrialBalanceEntry;
import com.yanimetaxas.bookkeeping.validation.TransferValidationException;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.Test;

/**
 * Behavior every ledger storage must provide. A subclass opens one ledger for the chart and name
 * supplied by each test.
 */
public abstract class LedgerContractTest {

  protected abstract Ledger ledger(ChartOfAccounts chart, String name);

  @Test
  void transferMovesTheAmountBetweenAccounts() {
    Ledger ledger = ledger(cashAndRevenue("1000.00"), "transfer");

    ledger.commit(payment(ledger, "T1", "50.00"));

    assertThat(ledger.getAccountBalance("CASH")).isEqualTo(Money.toMoney("950.00", "EUR"));
    assertThat(ledger.getAccountBalance("REVENUE")).isEqualTo(Money.toMoney("50.00", "EUR"));
    assertThat(ledger.findTransactions("CASH")).hasSize(1);
    assertThat(ledger.getTransactionByRef("T1").getTransactionDate())
        .isBetween(Instant.now().minusSeconds(60), Instant.now().plusSeconds(5));
  }

  @Test
  void unbalancedTransferLeavesBalancesUntouched() {
    Ledger ledger = ledger(cashAndRevenue("1000.00"), "unbalanced");
    TransferRequest request =
        ledger
            .createTransferRequest()
            .reference("T1")
            .type("sale")
            .account("CASH")
            .amount(Money.toMoney("-50.00", "EUR"))
            .account("REVENUE")
            .amount(Money.toMoney("25.00", "EUR"))
            .build();

    assertThatThrownBy(() -> ledger.commit(request)).isInstanceOf(UnbalancedLegsException.class);
    assertThat(ledger.getAccountBalance("CASH")).isEqualTo(Money.toMoney("1000.00", "EUR"));
    assertThat(ledger.getTransactionByRef("T1")).isNull();
  }

  @Test
  void overdraftLeavesBalancesAndStoresNoTransaction() {
    Ledger ledger = ledger(cashAndRevenue("10.00"), "overdraft");

    assertThatThrownBy(() -> ledger.commit(payment(ledger, "T1", "50.00")))
        .isInstanceOf(InsufficientFundsException.class);
    assertThat(ledger.getAccountBalance("CASH")).isEqualTo(Money.toMoney("10.00", "EUR"));
    assertThat(ledger.getAccountBalance("REVENUE")).isEqualTo(Money.toMoney("0.00", "EUR"));
    assertThat(ledger.findTransactions("CASH")).isEmpty();
    assertThat(ledger.getTransactionByRef("T1")).isNull();
  }

  @Test
  void duplicateReferenceIsRejectedAndTheFirstPostingRemains() {
    Ledger ledger = ledger(cashAndRevenue("1000.00"), "duplicate");
    ledger.commit(payment(ledger, "DUP", "10.00"));

    assertThatThrownBy(() -> ledger.commit(payment(ledger, "DUP", "5.00")))
        .isInstanceOf(TransferRequestExistsException.class);
    assertThat(ledger.getAccountBalance("CASH")).isEqualTo(Money.toMoney("990.00", "EUR"));
  }

  @Test
  void currencyMismatchLeavesBalancesUnchanged() {
    Ledger ledger = ledger(cashAndRevenue("1000.00"), "currency");
    TransferRequest request =
        ledger
            .createTransferRequest()
            .reference("FX")
            .type("mismatch")
            .account("CASH")
            .debit("10.00", "USD")
            .account("REVENUE")
            .credit("10.00", "USD")
            .build();

    assertThatThrownBy(() -> ledger.commit(request)).isInstanceOf(TransferValidationException.class);
    assertThat(ledger.getAccountBalance("CASH")).isEqualTo(Money.toMoney("1000.00", "EUR"));
    assertThat(ledger.getTransactionByRef("FX")).isNull();
  }

  @Test
  void fractionalAmountsKeepTheirScale() {
    Ledger ledger = ledger(cashAndRevenue("1000.000"), "scale");
    ledger.commit(
        ledger
            .createTransferRequest()
            .reference("T-SCALE")
            .type("testing")
            .account("CASH")
            .amount(Money.toMoney("-5.123", "EUR"))
            .account("REVENUE")
            .amount(Money.toMoney("5.123", "EUR"))
            .build());

    assertThat(ledger.getAccountBalance("CASH")).isEqualTo(Money.toMoney("994.877", "EUR"));
    assertThat(ledger.getAccountBalance("REVENUE")).isEqualTo(Money.toMoney("5.123", "EUR"));
    assertThat(
            ledger.getTransactionByRef("T-SCALE").getLegs().stream()
                .map(leg -> leg.getAmount().getAmount().toPlainString())
                .toList())
        .containsExactlyInAnyOrder("-5.123", "5.123");
    assertThat(ledger.toString()).contains("-5.123").contains("5.123");
  }

  @Test
  void trialBalanceCountsEachTransferOnce() {
    ChartOfAccounts chart =
        new ChartOfAccountsBuilder()
            .create("CASH", "1000.00", "EUR", AccountSide.DEBIT)
            .create("REVENUE", "0.00", "EUR", AccountSide.CREDIT)
            .build();
    Ledger ledger = ledger(chart, "trial");
    ledger.commit(payment(ledger, "T1", "50.00"));

    TrialBalance trialBalance = ledger.computeTrialBalance();

    assertThat(
            trialBalance.getEntries().stream()
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

  private static ChartOfAccounts cashAndRevenue(String cashAmount) {
    return new ChartOfAccountsBuilder()
        .create("CASH", cashAmount, "EUR")
        .create("REVENUE", "0.00", "EUR")
        .build();
  }

  private static TransferRequest payment(Ledger ledger, String reference, String amount) {
    return ledger
        .createTransferRequest()
        .reference(reference)
        .type("sale")
        .account("CASH")
        .debit(amount, "EUR")
        .account("REVENUE")
        .credit(amount, "EUR")
        .build();
  }
}
