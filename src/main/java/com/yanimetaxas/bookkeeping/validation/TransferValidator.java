package com.yanimetaxas.bookkeeping.validation;

import com.yanimetaxas.bookkeeping.exception.AccountNotFoundException;
import com.yanimetaxas.bookkeeping.exception.InsufficientFundsException;
import com.yanimetaxas.bookkeeping.exception.UnbalancedLegsException;
import com.yanimetaxas.bookkeeping.model.TransactionLeg;
import com.yanimetaxas.bookkeeping.model.TransferRequest;

/**
 * @author yanimetaxas
 * @since 14-Nov-14
 */
public interface TransferValidator {

  void validateTransferRequest(TransferRequest transferRequest);

  void transferRequestExists(String transactionRef);

  void isTransactionBalanced(Iterable<TransactionLeg> legs) throws UnbalancedLegsException;

  void currenciesMatch(Iterable<TransactionLeg> legs)
      throws TransferValidationException, AccountNotFoundException;

  void validBalance(Iterable<TransactionLeg> legs) throws InsufficientFundsException;

}
