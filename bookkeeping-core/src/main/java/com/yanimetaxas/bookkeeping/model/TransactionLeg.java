package com.yanimetaxas.bookkeeping.model;

import com.yanimetaxas.bookkeeping.validation.TransferValidationException;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Objects;

/**
 * Immutable value object representing a single debit or credit entry within a {@link Transaction}.
 *
 * <p>The {@link #getSide() side} is derived from the sign of the amount when not explicitly set:
 * negative amounts are debits, positive amounts are credits.
 *
 * <p>This class is thread-safe: all fields are final and the class is effectively immutable.
 *
 * @author yanimetaxas
 * @see Transaction
 * @since 14-Nov-14
 */
public final class TransactionLeg implements Serializable {

  private static final long serialVersionUID = 1L;

  private final String accountRef;
  private final Money amount;
  private final AccountSide side;

  public TransactionLeg(String accountRef, Money amount) {
    this(accountRef, amount, null);
  }

  public TransactionLeg(String accountRef, Money amount, AccountSide side) {
    if (accountRef == null) {
      throw new TransferValidationException("accountRef is null");
    }
    if (amount == null) {
      throw new TransferValidationException("amount is null");
    }
    this.accountRef = accountRef;
    this.amount = amount;
    this.side = side;
  }

  public Money getAmount() {
    return amount;
  }

  /**
   * Returns the absolute (unsigned) amount of this leg.
   */
  public Money getAbsoluteAmount() {
    return new Money(amount.getAmount().abs(), amount.getCurrency());
  }

  public String getAccountRef() {
    return accountRef;
  }

  /**
   * Returns the accounting side of this leg. If not explicitly set, inferred from the amount sign:
   * negative = DEBIT, positive/zero = CREDIT.
   */
  public AccountSide getSide() {
    if (side != null) {
      return side;
    }
    return amount.getAmount().compareTo(BigDecimal.ZERO) < 0
        ? AccountSide.DEBIT
        : AccountSide.CREDIT;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    TransactionLeg that = (TransactionLeg) o;
    return accountRef.equals(that.accountRef)
        && amount.equals(that.amount)
        && side == that.side;
  }

  @Override
  public int hashCode() {
    return Objects.hash(accountRef, amount, side);
  }

  @Override
  public String toString() {
    return String.format("TransactionLeg{%s, %s, %s}", accountRef, amount, getSide());
  }
}
