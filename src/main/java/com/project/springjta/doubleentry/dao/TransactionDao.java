package com.project.springjta.doubleentry.dao;

import java.util.List;
import java.util.Set;

import com.project.springjta.doubleentry.Transaction;

public interface TransactionDao {

	void storeTransaction(Transaction transaction);

	Set<String> getTransactionRefsForAccount(String accountRef);

	List<Transaction> getTransactions(List<String> transactionRefs);
	
	Transaction getTransactionByRef(String transactionRef);
	
	void truncateTables();
	
	void setClientRef(String clientRef);
	
	String getClientRef();
}
