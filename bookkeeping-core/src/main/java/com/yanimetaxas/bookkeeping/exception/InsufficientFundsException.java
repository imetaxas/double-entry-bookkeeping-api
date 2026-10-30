package com.yanimetaxas.bookkeeping.exception;

public class InsufficientFundsException extends BusinessException {

  private static final long serialVersionUID = 1L;

  public InsufficientFundsException(String accountRef) {
    super("Insufficient funds in account '" + accountRef + "'."
        + " The transfer would result in a negative balance."
        + " Ensure the source account has enough funds before committing.");
  }
}
