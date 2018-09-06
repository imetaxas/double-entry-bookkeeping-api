package com.yanimetaxas.bookkeeping.exception;

/**
 * @author yanimetaxas
 * @since 28-Aug-18
 */
public class LedgerAccountException extends BusinessException {

  private static final long serialVersionUID = -6113857738235916873L;

  public LedgerAccountException(String accountRef) {
    super("Ledger does not contain account with reference '" + accountRef + "'");
  }
}
