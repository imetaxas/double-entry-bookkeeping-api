package com.yanimetaxas.bookkeeping;

/**
 * Business exception thrown if an account participating in a transaction
 * has insufficient funds.
 */
public class InsufficientFundsException extends BusinessException {

  private static final long serialVersionUID = 1L;

  public InsufficientFundsException(String accountRef) {
    super("Insufficient funds for '" + accountRef + "'");
  }
}
