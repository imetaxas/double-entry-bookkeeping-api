package com.yanimetaxas.doubleentry.dao;

import com.yanimetaxas.doubleentry.Money;
import com.yanimetaxas.doubleentry.TransactionLeg;
import com.yanimetaxas.doubleentry.model.Account;

/**
 * @author yanimetaxas
 * @since 14-Nov-14
 */
public interface AccountDao {

  boolean accountExists(String accountRef);

  void createAccount(String clientRef, String accountRef, Money initialAmount);

  Account getAccount(String accountRef);

  void truncateTables();

  void updateBalance(TransactionLeg leg);

  void setClientRef(String clientRef);

  String getClientRef();
}
