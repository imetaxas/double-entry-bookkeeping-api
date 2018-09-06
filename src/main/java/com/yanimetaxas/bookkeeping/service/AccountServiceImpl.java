package com.yanimetaxas.bookkeeping.service;

import com.yanimetaxas.bookkeeping.dao.AccountDao;
import com.yanimetaxas.bookkeeping.exception.AccountNotFoundException;
import com.yanimetaxas.bookkeeping.exception.InfrastructureException;
import com.yanimetaxas.bookkeeping.model.Account;
import com.yanimetaxas.bookkeeping.model.Money;

/**
 * Implements the methods of the account service.
 *
 * @author yanimetaxas
 * @since 14-Nov-14
 */
public class AccountServiceImpl implements AccountService {

  private AccountDao accountDao;

  @Override
  public void createAccount(String accountRef, Money amount) throws InfrastructureException {
    if (accountDao.accountExists(accountRef)) {
      throw new InfrastructureException("Account already exists: " + accountRef);
    }
    accountDao.createAccount(accountDao.getClientRef(), accountRef, amount);
  }

  @Override
  public Money getAccountBalance(String accountRef) throws AccountNotFoundException {
    Account account = accountDao.getAccount(accountRef);
    if (account.isNullAccount()) {
      throw new AccountNotFoundException(accountRef);
    }
    return account.getBalance();
  }

  public void setAccountDao(AccountDao accountDao) {
    this.accountDao = accountDao;
  }

}
