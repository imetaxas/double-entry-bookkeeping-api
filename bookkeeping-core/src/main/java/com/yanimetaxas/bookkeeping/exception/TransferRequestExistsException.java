package com.yanimetaxas.bookkeeping.exception;

public class TransferRequestExistsException extends BusinessException {

  private static final long serialVersionUID = -6114857738235916873L;

  public TransferRequestExistsException(String transactionRef) {
    super("Transfer request with reference '" + transactionRef + "' already exists."
        + " Each transaction must have a unique reference.");
  }
}
