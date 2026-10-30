package com.yanimetaxas.bookkeeping.model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Currency;
import java.util.List;

/**
 * Immutable request describing a balanced, multi-legged monetary transaction between two or more
 * accounts. A request must have at least two legs. Different currencies are allowed when each
 * currency sums to zero.
 *
 * <p>{@code build()} returns a request that does not change if the builder is used again.
 *
 * @author yanimetaxas
 * @since 14-Nov-14
 */
public final class TransferRequest implements Serializable {

  private static final long serialVersionUID = 1L;

  private final String transactionRef;
  private final String transactionType;
  private final List<TransactionLeg> legs;

  private TransferRequest(String transactionRef, String transactionType, List<TransactionLeg> legs) {
    this.transactionRef = transactionRef;
    this.transactionType = transactionType;
    this.legs = List.copyOf(legs);
  }

  public static ReferenceStep builder() {
    return new Builder();
  }

  @FunctionalInterface
  public interface ReferenceStep {
    TypeStep reference(String transactionRef);
  }

  @FunctionalInterface
  public interface TypeStep {
    AccountStep type(String transactionType);
  }

  @FunctionalInterface
  public interface AccountStep {
    AmountStep account(String accountRef);
  }

  public interface AmountStep {
    BuildStep amount(Money money);

    /**
     * Posts the absolute amount as a negative value, decreasing the stored account balance.
     * {@link AccountSide} does not reverse this sign. A leading minus is accepted and ignored.
     */
    BuildStep debit(String amount, String currency);

    /**
     * Posts {@code amount} as a positive value, increasing the stored account balance.
     * {@link AccountSide} does not reverse this sign.
     */
    BuildStep credit(String amount, String currency);
  }

  public interface BuildStep {
    AmountStep account(String accountRef);

    TransferRequest build();
  }

  private static final class Builder
      implements ReferenceStep, TypeStep, AccountStep, AmountStep, BuildStep {

    private String transactionRef;
    private String transactionType;
    private final List<TransactionLeg> legs = new ArrayList<>();
    private String accountRef;

    @Override
    public TypeStep reference(String transactionRef) {
      this.transactionRef = transactionRef;
      return this;
    }

    @Override
    public AccountStep type(String transactionType) {
      this.transactionType = transactionType;
      return this;
    }

    @Override
    public AmountStep account(String accountRef) {
      this.accountRef = accountRef;
      return this;
    }

    @Override
    public BuildStep amount(Money money) {
      legs.add(new TransactionLeg(accountRef, money));
      accountRef = null;
      return this;
    }

    @Override
    public BuildStep debit(String amount, String currency) {
      BigDecimal negative = new BigDecimal(amount).abs().negate();
      legs.add(
          new TransactionLeg(
              accountRef, new Money(negative, Currency.getInstance(currency)), AccountSide.DEBIT));
      accountRef = null;
      return this;
    }

    @Override
    public BuildStep credit(String amount, String currency) {
      legs.add(
          new TransactionLeg(accountRef, Money.toMoney(amount, currency), AccountSide.CREDIT));
      accountRef = null;
      return this;
    }

    @Override
    public TransferRequest build() {
      return new TransferRequest(transactionRef, transactionType, legs);
    }
  }

  public List<TransactionLeg> getLegs() {
    return legs;
  }

  public String getTransactionRef() {
    return transactionRef;
  }

  public String getTransactionType() {
    return transactionType;
  }
}
