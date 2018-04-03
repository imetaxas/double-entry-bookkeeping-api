package com.yanimetaxas.doubleentry.service;

import com.google.common.collect.Lists;
import com.yanimetaxas.doubleentry.AccountNotFoundException;
import com.yanimetaxas.doubleentry.InsufficientFundsException;
import com.yanimetaxas.doubleentry.Transaction;
import com.yanimetaxas.doubleentry.TransactionLeg;
import com.yanimetaxas.doubleentry.TransferRequest;
import com.yanimetaxas.doubleentry.TransferService;
import com.yanimetaxas.doubleentry.dao.AccountDao;
import com.yanimetaxas.doubleentry.dao.TransactionDao;
import com.yanimetaxas.doubleentry.validation.TransferValidator;
import java.util.Date;
import java.util.List;
import java.util.Set;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implements the methods of the transfer service.
 *
 * @author yanimetaxas
 * @since 14-Nov-14
 */
public class TransferServiceImpl implements TransferService {

  private TransferValidator validator;
  private AccountDao accountDao;
  private TransactionDao transactionDao;

  @Override
  @Transactional(rollbackFor = Exception.class, isolation = Isolation.SERIALIZABLE)
  public void transferFunds(TransferRequest transferRequest)
      throws InsufficientFundsException, AccountNotFoundException {
    validateRequest(transferRequest);
    for (TransactionLeg leg : transferRequest.getLegs()) {
      accountDao.updateBalance(leg);
    }
    validator.validBalance(transferRequest.getLegs());
    storeTransaction(transferRequest);
  }

  private void validateRequest(TransferRequest request) {
    validator.validateTransferRequest(request);
    validator.isTransactionBalanced(request.getLegs());
    validator.currenciesMatch(request.getLegs());
  }

  private void storeTransaction(TransferRequest request) {
    Transaction transaction = new Transaction(
        request.getTransactionRef(),
        request.getTransactionType(),
        new Date(),
        request.getLegs()
    );
    transactionDao.storeTransaction(transaction);
  }

  @Override
  public List<Transaction> findTransactionsByAccountRef(String accountRef)
      throws AccountNotFoundException {
    if (!accountDao.accountExists(accountRef)) {
      throw new AccountNotFoundException(accountRef);
    }
    Set<String> transactionRefs = transactionDao.getTransactionRefsForAccount(accountRef);
    if (transactionRefs.isEmpty()) {
      return Lists.newArrayList();
    }
    return transactionDao.getTransactions(Lists.newArrayList(transactionRefs));
  }

  @Override
  public Transaction getTransactionByRef(String transactionRef) {
    return transactionDao.getTransactionByRef(transactionRef);
  }

  public void setAccountDao(AccountDao accountDao) {
    this.accountDao = accountDao;
  }

  public void setTransactionDao(TransactionDao transactionDao) {
    this.transactionDao = transactionDao;
  }

  public void setValidator(TransferValidator validator) {
    this.validator = validator;
  }

}


