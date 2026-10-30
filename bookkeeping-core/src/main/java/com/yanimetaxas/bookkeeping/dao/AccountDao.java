package com.yanimetaxas.bookkeeping.dao;

import com.yanimetaxas.bookkeeping.model.Account;
import com.yanimetaxas.bookkeeping.model.AccountSide;
import com.yanimetaxas.bookkeeping.model.Money;
import java.util.List;

/**
 * @author yanimetaxas
 * @since 14-Nov-14
 */
public interface AccountDao {

  boolean accountExists(String accountRef);

  void createAccount(
      String clientRef, String accountRef, Money initialAmount, AccountSide normalBalance);

  Account getAccount(String accountRef);

  List<Account> getAccounts();

  String getClientRef();
}
