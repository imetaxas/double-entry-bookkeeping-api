package com.yanimetaxas.doubleentry.service;

import com.yanimetaxas.doubleentry.AccountNotFoundException;
import com.yanimetaxas.doubleentry.AccountService;
import com.yanimetaxas.doubleentry.InfrastructureException;
import com.yanimetaxas.doubleentry.Money;
import com.yanimetaxas.doubleentry.dao.AccountDao;
import com.yanimetaxas.doubleentry.model.Account;

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
