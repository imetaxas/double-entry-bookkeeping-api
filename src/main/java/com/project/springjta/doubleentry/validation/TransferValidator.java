package com.project.springjta.doubleentry.validation;

import com.project.springjta.doubleentry.AccountNotFoundException;
import com.project.springjta.doubleentry.InsufficientFundsException;
import com.project.springjta.doubleentry.TransactionLeg;
import com.project.springjta.doubleentry.TransferRequest;
import com.project.springjta.doubleentry.UnbalancedLegsException;

/**
 * @author yanimetaxas
 * @since 14-Nov-14
 */
public interface TransferValidator {

	void validateTransferRequest(TransferRequest transferRequest);

	void isTransactionBalanced(Iterable<TransactionLeg> legs) throws UnbalancedLegsException;

	void currenciesMatch(Iterable<TransactionLeg> legs) throws TransferValidationException, AccountNotFoundException;

	void validBalance(Iterable<TransactionLeg> legs) throws InsufficientFundsException;

}
