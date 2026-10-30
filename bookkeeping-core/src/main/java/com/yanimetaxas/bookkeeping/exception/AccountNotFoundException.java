package com.yanimetaxas.bookkeeping.exception;

public class AccountNotFoundException extends BusinessException {

  private static final long serialVersionUID = 1L;

  public AccountNotFoundException(String accountRef) {
    super("Account '" + accountRef + "' not found in storage."
        + " Was init() called? If using JDBC, verify the database connection.");
  }
}
