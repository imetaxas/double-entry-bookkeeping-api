package com.yanimetaxas.bookkeeping.exception;

import java.util.Set;

public class LedgerAccountException extends BusinessException {

  private static final long serialVersionUID = -6113857738235916873L;

  public LedgerAccountException(String accountRef) {
    super("Ledger does not contain account '" + accountRef + "'."
        + " Ensure the account is listed in your ChartOfAccounts.");
  }

  public LedgerAccountException(String accountRef, Set<String> validRefs) {
    super("Ledger does not contain account '" + accountRef + "'."
        + " Valid accounts: " + validRefs + "."
        + " Add this account to your ChartOfAccounts or check for typos.");
  }
}
