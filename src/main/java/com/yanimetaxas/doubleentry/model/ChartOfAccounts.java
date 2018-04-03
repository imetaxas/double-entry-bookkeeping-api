package com.yanimetaxas.doubleentry.model;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * @author yanimetaxas
 * @since 02-Feb-18
 */
public class ChartOfAccounts {

  private final Map<String, Account> accountRefToAccountsMap = new HashMap<>();

  public ChartOfAccounts(Set<Account> accounts) {
    checkNotNull(accounts);
    checkArgument(!accounts.isEmpty());
    accounts.forEach(account -> this.accountRefToAccountsMap.put(account.getAccountRef(), account));
  }

  public Map<String, Account> getAccountRefToAccountsMap() {
    return accountRefToAccountsMap;
  }
}
