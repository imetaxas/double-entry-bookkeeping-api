package com.yanimetaxas.bookkeeping.memory;

import com.yanimetaxas.bookkeeping.dao.AccountDao;
import com.yanimetaxas.bookkeeping.model.Account;
import com.yanimetaxas.bookkeeping.model.AccountSide;
import com.yanimetaxas.bookkeeping.model.Money;
import com.yanimetaxas.bookkeeping.model.TransactionLeg;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Account storage for one in-memory ledger. The caller holds the storage lock.
 *
 * @author yanimetaxas
 * @since 1.0.0
 */
public class InMemoryAccountDao implements AccountDao {

  private final InMemoryLedgerData data;

  InMemoryAccountDao(InMemoryLedgerData data) {
    this.data = data;
  }

  @Override
  public boolean accountExists(String accountRef) {
    return data.accounts.containsKey(accountRef);
  }

  @Override
  public void createAccount(
      String clientRef, String accountRef, Money initialAmount, AccountSide normalBalance) {
    data.accounts.put(accountRef, new Account(accountRef, initialAmount, normalBalance));
  }

  @Override
  public Account getAccount(String accountRef) {
    Account account = data.accounts.get(accountRef);
    return account != null ? account : Account.nullAccount();
  }

  @Override
  public List<Account> getAccounts() {
    return data.accounts.values().stream()
        .sorted(Comparator.comparing(Account::getAccountRef))
        .toList();
  }

  void updateBalance(TransactionLeg leg) {
    Account existing = data.accounts.get(leg.getAccountRef());
    if (existing == null) {
      return;
    }
    BigDecimal newAmount = existing.getBalance().getAmount().add(leg.getAmount().getAmount());
    data.accounts.put(
        leg.getAccountRef(),
        new Account(
            leg.getAccountRef(),
            new Money(newAmount, existing.getCurrency()),
            existing.getOpening(),
            existing.getNormalBalance()));
  }

  @Override
  public String getClientRef() {
    return data.clientRef;
  }

  Map<String, Account> copyAccounts() {
    return new LinkedHashMap<>(data.accounts);
  }

  void replaceAccounts(Map<String, Account> snapshot) {
    data.accounts.clear();
    data.accounts.putAll(snapshot);
  }
}
