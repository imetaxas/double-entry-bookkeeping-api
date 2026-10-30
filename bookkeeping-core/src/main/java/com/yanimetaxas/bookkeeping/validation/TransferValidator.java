package com.yanimetaxas.bookkeeping.validation;

import com.yanimetaxas.bookkeeping.exception.InsufficientFundsException;
import com.yanimetaxas.bookkeeping.exception.UnbalancedLegsException;
import com.yanimetaxas.bookkeeping.model.Account;
import com.yanimetaxas.bookkeeping.model.TransactionLeg;
import com.yanimetaxas.bookkeeping.model.TransferRequest;
import java.util.Map;

/**
 * @author yanimetaxas
 * @since 14-Nov-14
 */
public interface TransferValidator {

  void validateTransferRequest(TransferRequest transferRequest);

  void transferRequestExists(String transactionRef);

  void isTransactionBalanced(Iterable<TransactionLeg> legs) throws UnbalancedLegsException;

  void currenciesMatch(Iterable<TransactionLeg> legs)
      throws TransferValidationException;

  /**
   * Checks {@code legs} against accounts already loaded for the current transaction.
   */
  void currenciesMatch(Iterable<TransactionLeg> legs, Map<String, Account> accounts)
      throws TransferValidationException;

  /**
   * Rejects the transfer when applying its legs would make a stored balance negative. Does not
   * change any balance.
   */
  void validBalance(Iterable<TransactionLeg> legs) throws InsufficientFundsException;

  /**
   * Same rule as {@link #validBalance(Iterable)}, using accounts loaded for the current
   * transaction.
   */
  void validBalance(Iterable<TransactionLeg> legs, Map<String, Account> accounts)
      throws InsufficientFundsException;
}
