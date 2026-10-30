package com.yanimetaxas.bookkeeping.jdbc;

import com.yanimetaxas.bookkeeping.exception.AccountNotFoundException;
import com.yanimetaxas.bookkeeping.exception.InfrastructureException;
import com.yanimetaxas.bookkeeping.model.Account;
import com.yanimetaxas.bookkeeping.model.AccountSide;
import com.yanimetaxas.bookkeeping.model.Money;
import com.yanimetaxas.bookkeeping.service.AccountService;
import java.util.List;

final class JdbcAccountService implements AccountService {

  private final JdbcAccountRepository accounts;

  JdbcAccountService(JdbcAccountRepository accounts) {
    this.accounts = accounts;
  }

  @Override
  public void createAccount(String accountRef, Money amount) {
    createAccount(accountRef, amount, null);
  }

  @Override
  public void createAccount(String accountRef, Money amount, AccountSide normalBalance) {
    if (accounts.accountExists(accountRef)) {
      throw new InfrastructureException("Account already exists: " + accountRef);
    }
    accounts.createAccount(accounts.getClientRef(), accountRef, amount, normalBalance);
  }

  @Override
  public boolean accountExists(String accountRef) {
    return accounts.accountExists(accountRef);
  }

  @Override
  public Money getAccountBalance(String accountRef) throws AccountNotFoundException {
    Account account = accounts.getAccount(accountRef);
    if (account.isNullAccount()) {
      throw new AccountNotFoundException(accountRef);
    }
    return account.getBalance();
  }

  @Override
  public List<Account> listAccounts() {
    return accounts.getAccounts();
  }
}
