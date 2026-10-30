package com.yanimetaxas.bookkeeping.model;

/**
 * Reporting label for an account's normal balance. It does not change posting arithmetic:
 * {@code debit()} decreases the stored signed balance and {@code credit()} increases it.
 *
 * <ul>
 *   <li>{@link #DEBIT} — Assets and Expenses increase on the debit side</li>
 *   <li>{@link #CREDIT} — Liabilities, Equity, and Revenue increase on the credit side</li>
 * </ul>
 *
 * @author yanimetaxas
 * @since 1.0.0
 */
public enum AccountSide {
  DEBIT,
  CREDIT
}
