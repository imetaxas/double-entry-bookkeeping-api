package com.yanimetaxas.bookkeeping.model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Currency;
import java.util.Objects;

/**
 * Immutable monetary value that couples an amount with a currency. The amount is represented by
 * {@link BigDecimal} and the currency by {@link Currency}.
 *
 * @author yanimetaxas
 */
public final class Money implements Serializable, Comparable<Money> {

  private static final long serialVersionUID = 1L;

  public static final Money NULL_MONEY =
      new Money(new BigDecimal("0.00"), Currency.getInstance("XXX"));

  private final BigDecimal amount;
  private final Currency currency;

  public Money(BigDecimal amount, Currency currency) {
    this.amount = Objects.requireNonNull(amount, "Amount must not be null");
    this.currency = Objects.requireNonNull(currency, "Currency must not be null");
  }

  public static Money toMoney(String amount, String currency) {
    return new Money(new BigDecimal(amount), Currency.getInstance(currency));
  }

  public Currency getCurrency() {
    return currency;
  }

  public BigDecimal getAmount() {
    return amount;
  }

  public boolean isNullMoney() {
    return this.equals(NULL_MONEY);
  }

  @Override
  public int compareTo(Money other) {
    return getAmount().compareTo(other.getAmount());
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Money money = (Money) o;
    return amount.equals(money.amount) && currency.equals(money.currency);
  }

  @Override
  public int hashCode() {
    return Objects.hash(amount, currency);
  }

  @Override
  public String toString() {
    return String.format("%s %s", amount.toPlainString(), currency.getCurrencyCode());
  }
}
