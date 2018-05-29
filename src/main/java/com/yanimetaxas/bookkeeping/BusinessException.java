package com.yanimetaxas.bookkeeping;

/**
 * Base type for recoverable business exceptions.
 *
 * @author yanimetaxas
 */
public abstract class BusinessException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  protected BusinessException(String message) {
    super(message);
  }
}
