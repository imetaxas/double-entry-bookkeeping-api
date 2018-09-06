package com.yanimetaxas.bookkeeping.dao;

import com.yanimetaxas.bookkeeping.model.Account;
import com.yanimetaxas.bookkeeping.model.Money;
import com.yanimetaxas.bookkeeping.model.TransactionLeg;

/**
 * @author yanimetaxas
 * @since 14-Nov-14
 */
public interface AccountDao {

  boolean accountExists(String accountRef);

  void createAccount(String clientRef, String accountRef, Money initialAmount);

  Account getAccount(String accountRef);

  void updateBalance(TransactionLeg leg);

  void setClientRef(String clientRef);

  String getClientRef();
}
