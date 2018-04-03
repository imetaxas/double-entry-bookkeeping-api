package com.yanimetaxas.doubleentry.model;

import com.yanimetaxas.doubleentry.Money;
import java.math.BigDecimal;
import java.util.Currency;
import java.util.HashSet;
import java.util.Set;

/**
 * @author yanimetaxas
 * @since 02-Feb-18
 */
public class ChartOfAccountsBuilder {

  private Set<Account> accountList = new HashSet<>();

  private ChartOfAccountsBuilder() {
  }

  public static ChartOfAccountsBuilder create() {
    return new ChartOfAccountsBuilder();
  }

  public ChartOfAccountsBuilder account(String accountRef, String amount, String currency) {
    this.accountList.add(new Account(accountRef, toMoney(amount, currency)));
    return this;
  }

  public ChartOfAccounts build() {
    return new ChartOfAccounts(accountList);
  }

  private static Money toMoney(String amount, String currency) {
    return new Money(new BigDecimal(amount), Currency.getInstance(currency));
  }
}
