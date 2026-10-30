package com.yanimetaxas.bookkeeping.exception;

public class UnbalancedLegsException extends BusinessException {

  private static final long serialVersionUID = 1L;

  public UnbalancedLegsException(String message) {
    super(message
        + " Total debits must equal total credits within each currency."
        + " Check the amounts in your TransferRequest legs.");
  }
}
