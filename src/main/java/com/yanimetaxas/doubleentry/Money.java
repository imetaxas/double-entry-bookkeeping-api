package com.yanimetaxas.doubleentry;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Currency;
import javax.annotation.Nonnull;

/**
 * Immutable monetary money class that couples an amount with a currency.
 * The amount value is represented by {@code java.math.BigDecimal} and the currency
 * by {@code java.util.Currency}.
 */
public final class Money implements Serializable, Comparable<Money> {

  private static final long serialVersionUID = 1L;

  /**
   * The monetary amount.
   */
  private BigDecimal amount;

  /**
   * ISO-4701 currency code.
   */
  private Currency currency;

  /**
   * Creates a new Money object but not intended to be used directly.
   *
   * @param amount the decimal amount
   * @param currency the currency
   * @throws IllegalArgumentException if value or currency are null
   */
  public Money(BigDecimal amount, Currency currency) {
    if (amount == null) {
      throw new IllegalArgumentException("Amount is null");
    }
    if (currency == null) {
      throw new IllegalArgumentException("Currency is null");
    }
    this.amount = amount;
    this.currency = currency;
  }

  public Currency getCurrency() {
    return currency;
  }

  public BigDecimal getAmount() {
    return amount;
  }

  public static Money toMoney(String amount, String currency) {
    return new Money(new BigDecimal(amount), Currency.getInstance(currency));
  }

  /**
   * Compares this money object with another instance. The money objects are
   * compared by their underlying long value.
   * <p/>
   * {@inheritDoc}
   */
  @Override
  public int compareTo(@Nonnull Money obj) {
    return getAmount().compareTo(obj.getAmount());
  }

  /**
   * Compares two money objects for equality. The money objects are
   * compared by their underlying bigDecimal value and currency ISO code.
   * <p/>
   * {@inheritDoc}
   */
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
    int result = amount.hashCode();
    result = 31 * result + currency.hashCode();
    return result;
  }
}
