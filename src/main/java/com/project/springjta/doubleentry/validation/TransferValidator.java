package com.project.springjta.doubleentry.validation;

import com.project.springjta.doubleentry.AccountNotFoundException;
import com.project.springjta.doubleentry.InsufficientFundsException;
import com.project.springjta.doubleentry.TransactionLeg;
import com.project.springjta.doubleentry.TransferRequest;

public interface TransferValidator {

	void validateTransferRequest(TransferRequest transferRequest);

	void isTransactionBalanced(Iterable<TransactionLeg> legs) throws TransferValidationException;

	void currenciesMatch(Iterable<TransactionLeg> legs) throws TransferValidationException, AccountNotFoundException;

	void validBalance(Iterable<TransactionLeg> legs) throws InsufficientFundsException;

}
