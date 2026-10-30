package com.yanimetaxas.bookkeeping.model;

import java.io.Serializable;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Immutable value object representing a monetary transaction between two or more accounts. Each
 * account entry is represented by a {@link TransactionLeg}.
 *
 * <p>This class is thread-safe: all fields are final, the legs list is defensively copied and
 * wrapped in an unmodifiable view. The posting time is an {@link Instant}.
 *
 * @author yanimetaxas
 * @see TransactionLeg
 * @since 14-Nov-14
 */
public final class Transaction implements Serializable {

  private static final long serialVersionUID = 1L;

  private final String transactionRef;
  private final String transactionType;
  private final Instant transactionDate;
  private final List<TransactionLeg> legs;

  public Transaction(
      String transactionRef,
      String transactionType,
      Instant transactionDate,
      List<TransactionLeg> legs) {
    this.transactionRef = transactionRef;
    this.transactionType = transactionType;
    this.transactionDate = Objects.requireNonNull(transactionDate, "transactionDate");
    this.legs = Collections.unmodifiableList(new ArrayList<>(legs));
  }

  public String getTransactionRef() {
    return transactionRef;
  }

  public String getTransactionType() {
    return transactionType;
  }

  public List<TransactionLeg> getLegs() {
    return legs;
  }

  public Instant getTransactionDate() {
    return transactionDate;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Transaction that = (Transaction) o;
    return Objects.equals(transactionRef, that.transactionRef);
  }

  @Override
  public int hashCode() {
    return Objects.hash(transactionRef);
  }

  @Override
  public String toString() {
    return String.format(
        "Transaction{ref='%s', type='%s', legs=%d}", transactionRef, transactionType, legs.size());
  }
}
