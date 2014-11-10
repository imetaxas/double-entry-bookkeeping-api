package com.project.springjta.doubleentry.dao;

import com.project.springjta.doubleentry.Money;
import com.project.springjta.doubleentry.TransactionLeg;
import com.project.springjta.doubleentry.model.Account;

public interface AccountDao {

	boolean accountExists(String accountRef);

	void createAccount(String clientRef, String accountRef, Money initialAmount);

	Account getAccount(String accountRef);

	void truncateTables();

	void updateBalance(TransactionLeg leg);
	
	void setClientRef(String clientRef);
	
	String getClientRef();
}
