package com.yanimetaxas.bookkeeping.validation;

import com.yanimetaxas.bookkeeping.AccountNotFoundException;
import com.yanimetaxas.bookkeeping.InsufficientFundsException;
import com.yanimetaxas.bookkeeping.TransactionLeg;
import com.yanimetaxas.bookkeeping.TransferRequest;
import com.yanimetaxas.bookkeeping.UnbalancedLegsException;

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
