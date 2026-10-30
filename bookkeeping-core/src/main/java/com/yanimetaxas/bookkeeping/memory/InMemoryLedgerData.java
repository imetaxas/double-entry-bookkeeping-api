package com.yanimetaxas.bookkeeping.memory;

import com.yanimetaxas.bookkeeping.model.Account;
import com.yanimetaxas.bookkeeping.model.Transaction;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Accounts and transactions for one ledger name. Callers must hold the storage lock.
 */
final class InMemoryLedgerData {

  final String clientRef;
  final Map<String, Account> accounts = new LinkedHashMap<>();
  final Map<String, Transaction> transactions = new LinkedHashMap<>();
  final Map<String, Set<String>> transactionRefsByAccount = new LinkedHashMap<>();

  InMemoryLedgerData(String clientRef) {
    this.clientRef = clientRef;
  }

  void clear() {
    accounts.clear();
    transactions.clear();
    transactionRefsByAccount.clear();
  }

  Set<String> refsFor(String accountRef) {
    return transactionRefsByAccount.computeIfAbsent(accountRef, ref -> new LinkedHashSet<>());
  }
}
