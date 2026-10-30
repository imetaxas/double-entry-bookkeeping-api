package com.yanimetaxas.bookkeeping.jdbc;

import com.yanimetaxas.bookkeeping.exception.AccountNotFoundException;
import com.yanimetaxas.bookkeeping.exception.InfrastructureException;
import com.yanimetaxas.bookkeeping.exception.InsufficientFundsException;
import com.yanimetaxas.bookkeeping.model.Account;
import com.yanimetaxas.bookkeeping.model.Transaction;
import com.yanimetaxas.bookkeeping.model.TransactionLeg;
import com.yanimetaxas.bookkeeping.model.TransferRequest;
import com.yanimetaxas.bookkeeping.service.TransferService;
import com.yanimetaxas.bookkeeping.validation.TransferValidator;
import com.yanimetaxas.bookkeeping.validation.TransferValidatorImpl;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.sql.DataSource;
import lombok.extern.slf4j.Slf4j;

/** Posts one transfer on a single database connection, then commits or rolls it back. */
@Slf4j
final class JdbcTransferService implements TransferService {

  private final DataSource dataSource;
  private final JdbcAccountRepository accounts;
  private final JdbcTransactionRepository transactions;
  private final TransferValidator validator;

  JdbcTransferService(
      DataSource dataSource,
      JdbcAccountRepository accounts,
      JdbcTransactionRepository transactions,
      TransferValidator validator) {
    this.dataSource = dataSource;
    this.accounts = accounts;
    this.transactions = transactions;
    this.validator = validator;
  }

  @Override
  public void transferFunds(TransferRequest transferRequest)
      throws InsufficientFundsException, AccountNotFoundException {
    validator.validateTransferRequest(transferRequest);
    validator.isTransactionBalanced(transferRequest.getLegs());

    try (Connection conn = dataSource.getConnection()) {
      conn.setAutoCommit(false);
      try {
        conn.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);
      } catch (SQLException e) {
        log.debug("SERIALIZABLE isolation not supported, using default");
      }
      try {
        transactions.rejectDuplicate(conn, transferRequest.getTransactionRef());
        postBalances(conn, transferRequest);
        transactions.insert(conn, transferRequest);
        conn.commit();
      } catch (Exception e) {
        rollback(conn, e);
        if (e instanceof RuntimeException runtime) {
          throw runtime;
        }
        throw new InfrastructureException(e);
      }
    } catch (SQLException e) {
      throw SqlExceptionTranslator.infrastructure(e);
    }
  }

  private void postBalances(Connection conn, TransferRequest request) {
    Map<String, Account> loaded = new LinkedHashMap<>();
    for (TransactionLeg leg : request.getLegs()) {
      loaded.computeIfAbsent(leg.getAccountRef(), ref -> accounts.load(conn, ref));
    }
    validator.currenciesMatch(request.getLegs(), loaded);
    validator.validBalance(request.getLegs(), loaded);
    for (Map.Entry<String, BigDecimal> net :
        TransferValidatorImpl.netByAccount(request.getLegs()).entrySet()) {
      Account account = loaded.get(net.getKey());
      BigDecimal projected = account.getBalance().getAmount().add(net.getValue());
      try {
        accounts.updateAmount(conn, net.getKey(), projected);
      } catch (SQLException e) {
        throw SqlExceptionTranslator.infrastructure(e);
      }
    }
  }

  private static void rollback(Connection conn, Exception original) {
    try {
      conn.rollback();
    } catch (SQLException rollbackFailure) {
      original.addSuppressed(rollbackFailure);
    }
  }

  @Override
  public List<Transaction> findTransactionsByAccountRef(String accountRef)
      throws AccountNotFoundException {
    if (!accounts.accountExists(accountRef)) {
      throw new AccountNotFoundException(accountRef);
    }
    Set<String> transactionRefs = transactions.getTransactionRefsForAccount(accountRef);
    if (transactionRefs.isEmpty()) {
      return new ArrayList<>();
    }
    return transactions.getTransactions(new ArrayList<>(transactionRefs));
  }

  @Override
  public List<Transaction> findAllTransactions() {
    return transactions.getAllTransactions();
  }

  @Override
  public Transaction getTransactionByRef(String transactionRef) {
    return transactions.getTransactionByRef(transactionRef);
  }
}
