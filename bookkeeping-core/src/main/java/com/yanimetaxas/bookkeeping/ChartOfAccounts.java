package com.yanimetaxas.bookkeeping;

import static com.yanimetaxas.bookkeeping.model.Money.NULL_MONEY;
import static com.yanimetaxas.bookkeeping.model.Money.toMoney;

import com.yanimetaxas.bookkeeping.model.Account;
import com.yanimetaxas.bookkeeping.model.AccountSide;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Accounts used to classify money received and spent. Each account reference appears once, in the
 * order it was added.
 *
 * @author yanimetaxas
 * @since 02-Feb-18
 */
public class ChartOfAccounts {

  private final Map<String, Account> accounts;

  private ChartOfAccounts(Map<String, Account> accounts) {
    if (accounts.isEmpty()) {
      throw new IllegalArgumentException(
          "Chart of accounts must contain at least one account."
              + " Call .create(\"AccountName\", \"0.00\", \"USD\") on the builder before .build().");
    }
    this.accounts = Collections.unmodifiableMap(new LinkedHashMap<>(accounts));
  }

  /**
   * Returns the accounts in insertion order.
   */
  public List<Account> get() {
    return List.copyOf(accounts.values());
  }

  public static class ChartOfAccountsBuilder {

    private final Map<String, Account> accounts = new LinkedHashMap<>();

    public ChartOfAccountsBuilder create(String accountRef, String amount, String currency) {
      return add(new Account(accountRef, toMoney(amount, currency)));
    }

    /**
     * Creates an account with an explicit normal balance side for trial-balance reporting.
     *
     * @param normalBalance reporting label; it does not change posting arithmetic
     */
    public ChartOfAccountsBuilder create(
        String accountRef, String amount, String currency, AccountSide normalBalance) {
      return add(new Account(accountRef, toMoney(amount, currency), normalBalance));
    }

    /**
     * Names an account that already exists in storage. {@code init()} does not create or reset it.
     */
    public ChartOfAccountsBuilder includeExisted(String accountRef) {
      return add(new Account(accountRef, NULL_MONEY));
    }

    public ChartOfAccounts build() {
      return new ChartOfAccounts(accounts);
    }

    private ChartOfAccountsBuilder add(Account account) {
      Account previous = accounts.putIfAbsent(account.getAccountRef(), account);
      if (previous != null) {
        throw new IllegalArgumentException(
            "Account '" + account.getAccountRef() + "' is already in the chart");
      }
      return this;
    }
  }
}
