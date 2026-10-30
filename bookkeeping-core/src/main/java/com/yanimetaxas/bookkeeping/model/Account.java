package com.yanimetaxas.bookkeeping.model;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.Objects;

/**
 * Immutable value object representing an account with an optional {@link AccountSide normal
 * balance} for proper T-account semantics.
 *
 * <p>This class is thread-safe: all fields are final and the class is effectively immutable.
 *
 * @author yanimetaxas
 * @since 14-Nov-14
 */
public final class Account {

  private final String accountRef;
  private final Money balance;
  private final Money opening;
  private final AccountSide normalBalance;

  private static final Account NULL_ACCOUNT =
      new Account("", new Money(new BigDecimal("0.00"), Currency.getInstance("XXX")));

  public Account(String accountRef, Money balance) {
    this(accountRef, balance, balance, null);
  }

  public Account(String accountRef, Money balance, AccountSide normalBalance) {
    this(accountRef, balance, balance, normalBalance);
  }

  /**
   * @param opening amount classified by the trial balance; later postings change {@code balance}
   *     and leave this value in place
   */
  public Account(String accountRef, Money balance, Money opening, AccountSide normalBalance) {
    this.accountRef = Objects.requireNonNull(accountRef, "accountRef must not be null");
    this.balance = Objects.requireNonNull(balance, "balance must not be null");
    this.opening = Objects.requireNonNull(opening, "opening must not be null");
    this.normalBalance = normalBalance;
  }

  public static Account nullAccount() {
    return NULL_ACCOUNT;
  }

  public String getAccountRef() {
    return accountRef;
  }

  public Money getBalance() {
    return balance;
  }

  /** Amount recorded when the account was opened. Postings do not change it. */
  public Money getOpening() {
    return opening;
  }

  public Currency getCurrency() {
    return balance.getCurrency();
  }

  /**
   * Returns the normal balance side of this account, or {@code null} if not specified.
   */
  public AccountSide getNormalBalance() {
    return normalBalance;
  }

  public boolean isOverdrawn() {
    return balance.getAmount().compareTo(BigDecimal.ZERO) < 0;
  }

  public boolean isNullAccount() {
    return this.equals(NULL_ACCOUNT);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Account account = (Account) o;
    return accountRef.equals(account.accountRef)
        && balance.equals(account.balance)
        && opening.equals(account.opening)
        && normalBalance == account.normalBalance;
  }

  @Override
  public int hashCode() {
    return Objects.hash(accountRef, balance, opening, normalBalance);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder("Account{");
    sb.append(accountRef).append(", ").append(balance);
    if (normalBalance != null) {
      sb.append(", ").append(normalBalance);
    }
    sb.append('}');
    return sb.toString();
  }
}
