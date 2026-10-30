package com.yanimetaxas.bookkeeping.service;

import com.yanimetaxas.bookkeeping.model.Transaction;
import com.yanimetaxas.bookkeeping.model.TransferRequest;
import java.util.List;

/**
 * Defines the business logic for performing monetary transactions between accounts.
 *
 * @author yanimetaxas
 * @since 14-Nov-14
 */
public interface TransferService {

  void transferFunds(TransferRequest transferRequest);

  List<Transaction> findTransactionsByAccountRef(String accountRef);

  /** Returns each committed transaction once. */
  List<Transaction> findAllTransactions();

  Transaction getTransactionByRef(String transactionRef);
}
