package com.yanimetaxas.bookkeeping.service;

import com.yanimetaxas.bookkeeping.exception.AccountNotFoundException;
import com.yanimetaxas.bookkeeping.model.Account;
import com.yanimetaxas.bookkeeping.model.AccountSide;
import com.yanimetaxas.bookkeeping.model.Money;
import java.util.List;

/**
 * Defines the business logic for managing monetary accounts.
 *
 * @author yanimetaxas
 * @since 14-Nov-14
 */
public interface AccountService {

  void createAccount(String accountRef, Money amount);

  void createAccount(String accountRef, Money amount, AccountSide normalBalance);

  boolean accountExists(String accountRef);

  Money getAccountBalance(String accountRef) throws AccountNotFoundException;

  List<Account> listAccounts();
}
