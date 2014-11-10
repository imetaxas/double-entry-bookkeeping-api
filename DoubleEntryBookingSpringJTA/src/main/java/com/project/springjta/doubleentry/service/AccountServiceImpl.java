package com.project.springjta.doubleentry.service;

import com.project.springjta.doubleentry.AccountNotFoundException;
import com.project.springjta.doubleentry.AccountService;
import com.project.springjta.doubleentry.InfrastructureException;
import com.project.springjta.doubleentry.Money;
import com.project.springjta.doubleentry.dao.AccountDao;
import com.project.springjta.doubleentry.model.Account;

/**
* Implements the methods of the account service.
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
