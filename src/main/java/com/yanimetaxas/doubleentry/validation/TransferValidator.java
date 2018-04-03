package com.yanimetaxas.doubleentry.validation;

import com.yanimetaxas.doubleentry.AccountNotFoundException;
import com.yanimetaxas.doubleentry.InsufficientFundsException;
import com.yanimetaxas.doubleentry.TransactionLeg;
import com.yanimetaxas.doubleentry.TransferRequest;
import com.yanimetaxas.doubleentry.UnbalancedLegsException;

/**
 * @author yanimetaxas
 * @since 14-Nov-14
 */
public interface TransferValidator {

  void validateTransferRequest(TransferRequest transferRequest);

  void isTransactionBalanced(Iterable<TransactionLeg> legs) throws UnbalancedLegsException;

  void currenciesMatch(Iterable<TransactionLeg> legs)
      throws TransferValidationException, AccountNotFoundException;

  void validBalance(Iterable<TransactionLeg> legs) throws InsufficientFundsException;

}
