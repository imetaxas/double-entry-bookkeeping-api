package com.yanimetaxas.bookkeeping.memory;

import com.yanimetaxas.bookkeeping.dao.TransactionDao;
import com.yanimetaxas.bookkeeping.model.Transaction;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Transaction index for one in-memory ledger. The caller holds the storage lock.
 *
 * @author yanimetaxas
 * @since 1.0.0
 */
public class InMemoryTransactionDao implements TransactionDao {

  private final InMemoryLedgerData data;

  InMemoryTransactionDao(InMemoryLedgerData data) {
    this.data = data;
  }

  @Override
  public void storeTransaction(Transaction transaction) {
    data.transactions.put(transaction.getTransactionRef(), transaction);
    transaction
        .getLegs()
        .forEach(leg -> data.refsFor(leg.getAccountRef()).add(transaction.getTransactionRef()));
  }

  @Override
  public Set<String> getTransactionRefsForAccount(String accountRef) {
    Set<String> refs = data.transactionRefsByAccount.get(accountRef);
    return refs != null ? new LinkedHashSet<>(refs) : new LinkedHashSet<>();
  }

  @Override
  public List<Transaction> getTransactions(List<String> transactionRefs) {
    List<Transaction> result = new ArrayList<>();
    for (String transactionRef : transactionRefs) {
      Transaction transaction = data.transactions.get(transactionRef);
      if (transaction != null) {
        result.add(transaction);
      }
    }
    return result;
  }

  @Override
  public List<Transaction> getAllTransactions() {
    return new ArrayList<>(data.transactions.values());
  }

  @Override
  public Transaction getTransactionByRef(String transactionRef) {
    return data.transactions.get(transactionRef);
  }

  @Override
  public String getClientRef() {
    return data.clientRef;
  }
}
