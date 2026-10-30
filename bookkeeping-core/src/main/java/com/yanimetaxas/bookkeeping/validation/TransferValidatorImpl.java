package com.yanimetaxas.bookkeeping.validation;

import com.yanimetaxas.bookkeeping.dao.AccountDao;
import com.yanimetaxas.bookkeeping.dao.TransactionDao;
import com.yanimetaxas.bookkeeping.exception.AccountNotFoundException;
import com.yanimetaxas.bookkeeping.exception.InsufficientFundsException;
import com.yanimetaxas.bookkeeping.exception.TransferRequestExistsException;
import com.yanimetaxas.bookkeeping.exception.UnbalancedLegsException;
import com.yanimetaxas.bookkeeping.model.Account;
import com.yanimetaxas.bookkeeping.model.TransactionLeg;
import com.yanimetaxas.bookkeeping.model.TransferRequest;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Currency;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Validates business rules for fund transfers.
 *
 * @author yanimetaxas
 * @since 14-Nov-14
 */
public class TransferValidatorImpl implements TransferValidator {

  private AccountDao accountDao;
  private TransactionDao transactionDao;

  @Override
  public void validateTransferRequest(TransferRequest transferRequest) {
    if (transferRequest.getTransactionRef() == null
        || transferRequest.getTransactionType() == null
        || transferRequest.getLegs().size() < 2) {
      throw new IllegalArgumentException("Incomplete transaction request");
    }
  }

  @Override
  public void transferRequestExists(String transactionRef) {
    if (transactionDao.getTransactionByRef(transactionRef) != null) {
      throw new TransferRequestExistsException(transactionRef);
    }
  }

  @Override
  public void isTransactionBalanced(Iterable<TransactionLeg> legs) throws UnbalancedLegsException {
    Map<Currency, List<TransactionLeg>> legsByCurrency = sortByCurrency(legs);
    for (List<TransactionLeg> currencyLegs : legsByCurrency.values()) {
      checkTransactionLegsBalanced(currencyLegs);
    }
  }

  @Override
  public void currenciesMatch(Iterable<TransactionLeg> legs)
      throws TransferValidationException, AccountNotFoundException {
    currenciesMatch(legs, loadAccounts(legs));
  }

  @Override
  public void currenciesMatch(Iterable<TransactionLeg> legs, Map<String, Account> accounts)
      throws TransferValidationException, AccountNotFoundException {
    for (TransactionLeg leg : legs) {
      Account account = accounts.get(leg.getAccountRef());
      if (account == null || account.isNullAccount()) {
        throw new AccountNotFoundException(leg.getAccountRef());
      }
      if (!account.getCurrency().equals(leg.getAmount().getCurrency())) {
        throw new TransferValidationException(
            "Currency from transaction leg does not match account's currency for one or more legs");
      }
    }
  }

  @Override
  public void validBalance(Iterable<TransactionLeg> legs) throws InsufficientFundsException {
    validBalance(legs, loadAccounts(legs));
  }

  @Override
  public void validBalance(Iterable<TransactionLeg> legs, Map<String, Account> accounts)
      throws InsufficientFundsException {
    for (Map.Entry<String, BigDecimal> net : netByAccount(legs).entrySet()) {
      Account account = accounts.get(net.getKey());
      if (account == null || account.isNullAccount()) {
        throw new AccountNotFoundException(net.getKey());
      }
      BigDecimal projected = account.getBalance().getAmount().add(net.getValue());
      if (projected.compareTo(BigDecimal.ZERO) < 0) {
        throw new InsufficientFundsException(net.getKey());
      }
    }
  }

  private Map<String, Account> loadAccounts(Iterable<TransactionLeg> legs) {
    Map<String, Account> accounts = new LinkedHashMap<>();
    for (TransactionLeg leg : legs) {
      accounts.computeIfAbsent(leg.getAccountRef(), accountDao::getAccount);
    }
    return accounts;
  }

  /**
   * Returns the signed net change each account would receive from {@code legs}.
   */
  public static Map<String, BigDecimal> netByAccount(Iterable<TransactionLeg> legs) {
    Map<String, BigDecimal> net = new LinkedHashMap<>();
    for (TransactionLeg leg : legs) {
      net.merge(leg.getAccountRef(), leg.getAmount().getAmount(), BigDecimal::add);
    }
    return Map.copyOf(net);
  }

  private void checkTransactionLegsBalanced(Iterable<TransactionLeg> legs)
      throws UnbalancedLegsException {
    BigDecimal sum = BigDecimal.ZERO;
    for (TransactionLeg leg : legs) {
      sum = sum.add(leg.getAmount().getAmount());
    }
    if (sum.compareTo(BigDecimal.ZERO) != 0) {
      throw new UnbalancedLegsException("Transaction legs not balanced");
    }
  }

  private Map<Currency, List<TransactionLeg>> sortByCurrency(Iterable<TransactionLeg> legs) {
    Map<Currency, List<TransactionLeg>> legsByCurrency = new HashMap<>();
    for (TransactionLeg leg : legs) {
      legsByCurrency
          .computeIfAbsent(leg.getAmount().getCurrency(), k -> new ArrayList<>())
          .add(leg);
    }
    return legsByCurrency;
  }

  public void setAccountDao(AccountDao accountDao) {
    this.accountDao = accountDao;
  }

  public void setTransactionDao(TransactionDao transactionDao) {
    this.transactionDao = transactionDao;
  }
}
