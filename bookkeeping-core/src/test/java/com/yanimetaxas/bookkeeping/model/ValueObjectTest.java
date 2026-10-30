package com.yanimetaxas.bookkeeping.model;

import static io.github.imetaxas.realitycheck.RealityAssertions.assertThat;
import static io.github.imetaxas.realitycheck.RealityAssertions.assertThatThrownBy;

import com.yanimetaxas.bookkeeping.exception.InfrastructureException;
import com.yanimetaxas.bookkeeping.exception.LedgerAccountException;
import com.yanimetaxas.bookkeeping.validation.TransferValidationException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Currency;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ValueObjectTest {

  private static final Currency EUR = Currency.getInstance("EUR");

  @Test
  void moneyRejectsADifferentValueAndDescribesItself() {
    Money money = Money.toMoney("1000.00", "SEK");

    assertThat(money).isNotNull();
    assertThat((Object) money).isNotEqualTo("1000.00 SEK");
    assertThat(money.toString()).isEqualTo("1000.00 SEK");
  }

  @Test
  void accountEqualityFollowsRefBalanceAndNormalSide() {
    Account debit = new Account("CASH", Money.toMoney("10.00", "EUR"), AccountSide.DEBIT);
    Account same = new Account("CASH", Money.toMoney("10.00", "EUR"), AccountSide.DEBIT);
    Account credit = new Account("CASH", Money.toMoney("10.00", "EUR"), AccountSide.CREDIT);
    Account otherAmount = new Account("CASH", Money.toMoney("11.00", "EUR"), AccountSide.DEBIT);
    Account otherRef = new Account("BANK", Money.toMoney("10.00", "EUR"), AccountSide.DEBIT);
    Account noSide = new Account("CASH", Money.toMoney("10.00", "EUR"));

    assertThat(debit).isEqualTo(debit).isEqualTo(same).hasSameHashCodeAs(same);
    TrialBalanceEntry coarse =
        new TrialBalanceEntry("CASH", AccountSide.DEBIT, new BigDecimal("100.00"), BigDecimal.ZERO, EUR);
    TrialBalanceEntry scientific =
        new TrialBalanceEntry("CASH", AccountSide.DEBIT, new BigDecimal("1E+2"), BigDecimal.ZERO, EUR);
    assertThat(coarse).isEqualTo(scientific).hasSameHashCodeAs(scientific);
    assertThat(debit).isNotNull();
    assertThat((Object) debit).isNotEqualTo("CASH");
    assertThat(debit)
        .isNotEqualTo(credit)
        .isNotEqualTo(otherAmount)
        .isNotEqualTo(otherRef)
        .isNotEqualTo(noSide);
    Account moved =
        new Account(
            "CASH",
            Money.toMoney("7.00", "EUR"),
            Money.toMoney("10.00", "EUR"),
            AccountSide.DEBIT);
    assertThat(moved.getOpening()).isEqualTo(Money.toMoney("10.00", "EUR"));
    assertThat(moved.getBalance()).isEqualTo(Money.toMoney("7.00", "EUR"));
    assertThat(moved)
        .isNotEqualTo(new Account("CASH", Money.toMoney("7.00", "EUR"), AccountSide.DEBIT));
    assertThat(debit.toString()).contains("CASH").contains("DEBIT");
    assertThat(noSide.toString()).doesNotContain("DEBIT");
    assertThat(new Account("CASH", Money.toMoney("-1.00", "EUR")).isOverdrawn()).isTrue();
    assertThat(debit.isOverdrawn()).isFalse();
    assertThat(Account.nullAccount().isNullAccount()).isTrue();
  }

  @Test
  void transactionEqualityUsesOnlyTheReference() {
    TransactionLeg leg = new TransactionLeg("CASH", Money.toMoney("-1.00", "EUR"));
    Instant posted = Instant.parse("2024-05-06T07:08:09Z");
    Transaction first = new Transaction("T1", "sale", posted, List.of(leg));
    Transaction sameRef = new Transaction("T1", "refund", Instant.EPOCH, List.of());
    Transaction otherRef = new Transaction("T2", "sale", posted, List.of(leg));
    Transaction unnamed = new Transaction(null, "sale", posted, List.of(leg));

    assertThat(first).isEqualTo(sameRef).hasSameHashCodeAs(sameRef);
    assertThat(first).isNotEqualTo(otherRef).isNotNull();
    assertThat((Object) first).isNotEqualTo(leg);
    assertThat(unnamed).isEqualTo(new Transaction(null, "other", posted, List.of()));
    assertThat(first.toString()).contains("T1").contains("sale");
    assertThat(first.equals(first)).isTrue();
    assertThat(leg.equals(leg)).isTrue();
    assertThat(first.getTransactionDate()).isEqualTo(posted);
    assertThatThrownBy(() -> new Transaction("T1", "sale", null, List.of()))
        .isInstanceOf(NullPointerException.class);
  }

  @Test
  void transactionLegDerivesItsSideFromTheAmount() {
    TransactionLeg debit = new TransactionLeg("CASH", Money.toMoney("-5.00", "EUR"));
    TransactionLeg explicit =
        new TransactionLeg("CASH", Money.toMoney("5.00", "EUR"), AccountSide.DEBIT);
    TransactionLeg zero = new TransactionLeg("CASH", Money.toMoney("0.00", "EUR"));

    assertThat(debit.getSide()).isEqualTo(AccountSide.DEBIT);
    assertThat(explicit.getSide()).isEqualTo(AccountSide.DEBIT);
    assertThat(zero.getSide()).isEqualTo(AccountSide.CREDIT);
    assertThat(debit.getAbsoluteAmount()).isEqualTo(Money.toMoney("5.00", "EUR"));
  }

  @Test
  void transactionLegEqualityUsesTheAccountAndAmount() {
    TransactionLeg debit = new TransactionLeg("CASH", Money.toMoney("-5.00", "EUR"));
    TransactionLeg explicit =
        new TransactionLeg("CASH", Money.toMoney("5.00", "EUR"), AccountSide.DEBIT);

    assertThat(debit).isEqualTo(new TransactionLeg("CASH", Money.toMoney("-5.00", "EUR")));
    assertThat(debit.hashCode())
        .isEqualTo(new TransactionLeg("CASH", Money.toMoney("-5.00", "EUR")).hashCode());
    assertThat(debit).isNotEqualTo(explicit).isNotNull();
    assertThat((Object) debit).isNotEqualTo("CASH");
    assertThat(debit)
        .isNotEqualTo(new TransactionLeg("BANK", Money.toMoney("-5.00", "EUR")))
        .isNotEqualTo(new TransactionLeg("CASH", Money.toMoney("-6.00", "EUR")));
    assertThat(debit.toString()).contains("CASH").contains("DEBIT");
  }

  @Test
  void transactionLegRejectsANullPart() {
    assertThatThrownBy(() -> new TransactionLeg(null, Money.toMoney("1.00", "EUR")))
        .isInstanceOf(TransferValidationException.class);
    assertThatThrownBy(() -> new TransactionLeg("CASH", null))
        .isInstanceOf(TransferValidationException.class);
  }

  @Test
  void trialBalanceEntryEqualityComparesEveryColumn() {
    TrialBalanceEntry entry =
        new TrialBalanceEntry(
            "CASH", AccountSide.DEBIT, new BigDecimal("10.00"), BigDecimal.ZERO, EUR);
    TrialBalanceEntry same =
        new TrialBalanceEntry(
            "CASH", AccountSide.DEBIT, new BigDecimal("10.00"), BigDecimal.ZERO, EUR);

    assertThat(entry).isEqualTo(entry).isEqualTo(same).hasSameHashCodeAs(same);
    assertThat(entry).isNotNull();
    assertThat((Object) entry).isNotEqualTo("CASH");
    assertThat(entry)
        .isNotEqualTo(
            new TrialBalanceEntry(
                "BANK", AccountSide.DEBIT, new BigDecimal("10.00"), BigDecimal.ZERO, EUR))
        .isNotEqualTo(
            new TrialBalanceEntry(
                "CASH", AccountSide.CREDIT, new BigDecimal("10.00"), BigDecimal.ZERO, EUR))
        .isNotEqualTo(
            new TrialBalanceEntry(
                "CASH", AccountSide.DEBIT, new BigDecimal("11.00"), BigDecimal.ZERO, EUR))
        .isNotEqualTo(
            new TrialBalanceEntry(
                "CASH", AccountSide.DEBIT, new BigDecimal("10.00"), new BigDecimal("1.00"), EUR))
        .isNotEqualTo(
            new TrialBalanceEntry(
                "CASH",
                AccountSide.DEBIT,
                new BigDecimal("10.00"),
                BigDecimal.ZERO,
                Currency.getInstance("USD")));
    assertThat(entry.toString()).contains("CASH").contains("EUR");
  }

  @Test
  void ledgerAccountExceptionNamesTheMissingAccount() {
    assertThat(new LedgerAccountException("CASH").getMessage()).contains("CASH");
    assertThat(new LedgerAccountException("CASH", Set.of("BANK")).getMessage()).contains("BANK");
  }

  @Test
  void infrastructureExceptionKeepsItsMessageAndCause() {
    assertThat(new InfrastructureException("disk full").getMessage()).isEqualTo("disk full");
    InfrastructureException caused =
        new InfrastructureException("disk full", new IllegalStateException("io"));
    assertThat(caused.getMessage()).isEqualTo("disk full");
    assertThat(caused.getCause()).isInstanceOf(IllegalStateException.class);
    assertThat(new InfrastructureException(new IllegalStateException("io")).getCause().getMessage())
        .isEqualTo("io");
  }

  @Test
  void trialBalanceAmountsWithDifferentScalesAreEqual() {
    TrialBalanceEntry cents =
        new TrialBalanceEntry(
            "CASH", AccountSide.DEBIT, new BigDecimal("10.00"), BigDecimal.ZERO, EUR);
    TrialBalanceEntry units =
        new TrialBalanceEntry(
            "CASH", AccountSide.DEBIT, new BigDecimal("10.0"), BigDecimal.ZERO, EUR);

    assertThat(cents).isEqualTo(units).hasSameHashCodeAs(units);
  }

  @Test
  void debitOfANegativeStringPostsASingleNegativeAmount() {
    TransferRequest request =
        TransferRequest.builder()
            .reference("T1")
            .type("sale")
            .account("CASH")
            .debit("-5.00", "EUR")
            .account("REVENUE")
            .credit("5.00", "EUR")
            .build();

    assertThat(request.getLegs().get(0).getAmount()).isEqualTo(Money.toMoney("-5.00", "EUR"));
  }

  @Test
  void builtRequestDoesNotChangeWhenTheBuilderContinues() {
    TransferRequest.BuildStep builder =
        TransferRequest.builder().reference("T1").type("sale").account("CASH").debit("1.00", "EUR");
    TransferRequest built = builder.build();

    builder.account("REVENUE").credit("1.00", "EUR");

    assertThat(built.getLegs()).hasSize(1);
  }
}
