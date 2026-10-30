package com.yanimetaxas.bookkeeping.model;

import com.yanimetaxas.bookkeeping.report.TrialBalanceReport;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Currency;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A trial balance lists every account with its total debits and total credits. If double-entry
 * bookkeeping has been performed correctly, total debits equal total credits.
 *
 * <p>Use {@link #compute(List, List)} to create a trial balance from accounts and their
 * transactions.
 *
 * @author yanimetaxas
 * @since 1.0.0
 */
public final class TrialBalance {

  private final List<TrialBalanceEntry> entries;
  private final boolean balanced;
  private final Instant timestamp;

  private TrialBalance(List<TrialBalanceEntry> entries, boolean balanced, Instant timestamp) {
    this.entries = Collections.unmodifiableList(entries);
    this.balanced = balanced;
    this.timestamp = timestamp;
  }

  /**
   * Computes a trial balance from a list of accounts and all transactions in the ledger.
   *
   * <p>A positive opening is a debit for a debit-normal account and a credit otherwise. A negative
   * opening is placed on the opposite side. Each posting is classified by its own side.
   *
   * @param accounts all accounts in the ledger
   * @param allTransactions all committed transactions
   * @return a new trial balance
   */
  public static TrialBalance compute(List<Account> accounts, List<Transaction> allTransactions) {
    return compute(accounts, allTransactions, Clock.systemUTC());
  }

  /**
   * Computes a trial balance and stamps it with {@code clock}.
   *
   * @param accounts all accounts in the ledger
   * @param allTransactions each committed transaction once
   * @param clock clock used for {@link #getTimestamp()}
   * @return a new trial balance
   */
  public static TrialBalance compute(
      List<Account> accounts, List<Transaction> allTransactions, Clock clock) {
    Map<String, DebitCredit> debitCredit = new LinkedHashMap<>();
    Map<String, AccountSide> normalBalances = new LinkedHashMap<>();
    Map<String, Currency> currencies = new LinkedHashMap<>();

    for (Account account : accounts) {
      String ref = account.getAccountRef();
      debitCredit.put(ref, new DebitCredit());
      normalBalances.put(ref, account.getNormalBalance());
      currencies.put(ref, account.getCurrency());
    }

    for (Account account : accounts) {
      debitCredit
          .get(account.getAccountRef())
          .addOpening(account.getOpening().getAmount(), account.getNormalBalance());
    }

    for (Transaction tx : allTransactions) {
      for (TransactionLeg leg : tx.getLegs()) {
        DebitCredit totals = debitCredit.get(leg.getAccountRef());
        if (totals == null) {
          continue;
        }
        totals.add(leg.getSide(), leg.getAmount().getAmount().abs());
      }
    }

    List<TrialBalanceEntry> entries = new ArrayList<>();
    BigDecimal totalDebit = BigDecimal.ZERO;
    BigDecimal totalCredit = BigDecimal.ZERO;

    for (Map.Entry<String, DebitCredit> e : debitCredit.entrySet()) {
      String ref = e.getKey();
      DebitCredit totals = e.getValue();
      entries.add(
          new TrialBalanceEntry(
              ref, normalBalances.get(ref), totals.debit(), totals.credit(), currencies.get(ref)));
      totalDebit = totalDebit.add(totals.debit());
      totalCredit = totalCredit.add(totals.credit());
    }

    boolean balanced = totalDebit.compareTo(totalCredit) == 0;
    return new TrialBalance(entries, balanced, clock.instant());
  }

  /** Debit and credit totals for one account. */
  private static final class DebitCredit {

    private BigDecimal debit = BigDecimal.ZERO;
    private BigDecimal credit = BigDecimal.ZERO;

    void addOpening(BigDecimal amount, AccountSide side) {
      if (amount.compareTo(BigDecimal.ZERO) == 0) {
        return;
      }
      boolean creditSide = side == AccountSide.DEBIT ? amount.signum() < 0 : amount.signum() > 0;
      add(creditSide ? AccountSide.CREDIT : AccountSide.DEBIT, amount.abs());
    }

    void add(AccountSide side, BigDecimal magnitude) {
      if (side == AccountSide.DEBIT) {
        debit = debit.add(magnitude);
      } else {
        credit = credit.add(magnitude);
      }
    }

    BigDecimal debit() {
      return debit;
    }

    BigDecimal credit() {
      return credit;
    }
  }

  public List<TrialBalanceEntry> getEntries() {
    return entries;
  }

  public boolean isBalanced() {
    return balanced;
  }

  public Instant getTimestamp() {
    return timestamp;
  }

  public BigDecimal getTotalDebits() {
    return entries.stream()
        .map(TrialBalanceEntry::getDebitTotal)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
  }

  public BigDecimal getTotalCredits() {
    return entries.stream()
        .map(TrialBalanceEntry::getCreditTotal)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
  }

  @Override
  public String toString() {
    return TrialBalanceReport.format(this);
  }
}
