package com.yanimetaxas.bookkeeping.model;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.Objects;

/**
 * Immutable value object representing a single row in a {@link TrialBalance} — one account's
 * debit and credit totals.
 *
 * @author yanimetaxas
 * @since 1.0.0
 */
public final class TrialBalanceEntry {

  private final String accountRef;
  private final AccountSide normalBalance;
  private final BigDecimal debitTotal;
  private final BigDecimal creditTotal;
  private final Currency currency;

  public TrialBalanceEntry(
      String accountRef,
      AccountSide normalBalance,
      BigDecimal debitTotal,
      BigDecimal creditTotal,
      Currency currency) {
    this.accountRef = accountRef;
    this.normalBalance = normalBalance;
    this.debitTotal = debitTotal;
    this.creditTotal = creditTotal;
    this.currency = currency;
  }

  public String getAccountRef() {
    return accountRef;
  }

  public AccountSide getNormalBalance() {
    return normalBalance;
  }

  public BigDecimal getDebitTotal() {
    return debitTotal;
  }

  public BigDecimal getCreditTotal() {
    return creditTotal;
  }

  public Currency getCurrency() {
    return currency;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    TrialBalanceEntry that = (TrialBalanceEntry) o;
    return accountRef.equals(that.accountRef)
        && normalBalance == that.normalBalance
        && debitTotal.compareTo(that.debitTotal) == 0
        && creditTotal.compareTo(that.creditTotal) == 0
        && currency.equals(that.currency);
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        accountRef, normalBalance, canonical(debitTotal), canonical(creditTotal), currency);
  }

  /**
   * Scale-insensitive form so {@code hashCode} agrees with {@link BigDecimal#compareTo}.
   */
  private static BigDecimal canonical(BigDecimal value) {
    if (value.signum() == 0) {
      return BigDecimal.ZERO;
    }
    BigDecimal stripped = value.stripTrailingZeros();
    return stripped.scale() < 0 ? stripped.setScale(0) : stripped;
  }

  @Override
  public String toString() {
    return String.format(
        "TrialBalanceEntry{%s, debit=%.2f, credit=%.2f, %s}",
        accountRef, debitTotal, creditTotal, currency.getCurrencyCode());
  }
}
