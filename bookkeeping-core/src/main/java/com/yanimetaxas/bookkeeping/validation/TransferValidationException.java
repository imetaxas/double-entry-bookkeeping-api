package com.yanimetaxas.bookkeeping.validation;

/**
 * @author yanimetaxas
 * @since 14-Nov-14
 */
public class TransferValidationException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  public TransferValidationException(String message) {
    super(message);
  }
}
