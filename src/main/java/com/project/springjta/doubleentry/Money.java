package com.project.springjta.doubleentry;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Currency;

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
     * @throws NullPointerException if value or currency are null
     */
    public Money(BigDecimal amount, Currency currency) {
        if (amount == null) {
            throw new NullPointerException("value is null");
        }
        if (currency == null) {
            throw new NullPointerException("currency is null");
        }
        this.amount = amount;
        this.currency = currency;
    }

    protected Money() {
    }

    public Currency getCurrency() {
        return currency;
    }

    /**
     * Return the underlying monetary value as BigDecimal.
     *
     * @return the monetary value
     */
    public BigDecimal getAmount() {
        return amount;
    }

    /**
     * Compares this money object with another instance. The money objects are
     * compared by their underlying long value.
     * <p/>
     * {@inheritDoc}
     */
    public int compareTo(Money o) {
        return getAmount().compareTo(o.getAmount());
    }

    /**
     * Compares two money objects for equality. The money objects are
     * compared by their underlying bigdecimal value and currency ISO code.
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

        if (!amount.equals(money.amount)) {
            return false;
        }
        if (!currency.equals(money.currency)) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = amount.hashCode();
        result = 31 * result + currency.hashCode();
        return result;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("Money");
        sb.append("{amount=").append(amount);
        sb.append(", currency=").append(currency);
        sb.append('}');
        return sb.toString();
    }
}
