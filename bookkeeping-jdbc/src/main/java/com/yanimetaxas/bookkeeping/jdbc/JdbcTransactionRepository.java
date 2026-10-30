package com.yanimetaxas.bookkeeping.jdbc;

import com.yanimetaxas.bookkeeping.dao.TransactionDao;
import com.yanimetaxas.bookkeeping.exception.TransferRequestExistsException;
import com.yanimetaxas.bookkeeping.model.Transaction;
import com.yanimetaxas.bookkeeping.model.TransactionLeg;
import com.yanimetaxas.bookkeeping.model.TransferRequest;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Clock;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.sql.DataSource;

/** Transaction history for one ledger name. */
final class JdbcTransactionRepository implements TransactionDao {

  private final DataSource dataSource;
  private final String clientRef;
  private final Clock clock;

  JdbcTransactionRepository(DataSource dataSource, String clientRef) {
    this(dataSource, clientRef, Clock.systemUTC());
  }

  JdbcTransactionRepository(DataSource dataSource, String clientRef, Clock clock) {
    this.dataSource = dataSource;
    this.clientRef = clientRef;
    this.clock = clock;
  }

  @Override
  public void storeTransaction(Transaction transaction) {
    throw new UnsupportedOperationException(
        "Transfers store history on the same connection as the balance update");
  }

  void rejectDuplicate(Connection conn, String transactionRef) throws SQLException {
    String sql =
        "SELECT transaction_ref FROM transaction_history"
            + " WHERE client_ref = ? AND transaction_ref = ?";
    try (PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, clientRef);
      ps.setString(2, transactionRef);
      try (ResultSet rs = ps.executeQuery()) {
        if (rs.next()) {
          throw new TransferRequestExistsException(transactionRef);
        }
      }
    }
  }

  void insert(Connection conn, TransferRequest request) throws SQLException {
    String txSql =
        "INSERT INTO transaction_history"
            + " (client_ref, transaction_ref, transaction_type, transaction_date)"
            + " VALUES (?, ?, ?, ?)";
    try (PreparedStatement ps = conn.prepareStatement(txSql)) {
      ps.setString(1, clientRef);
      ps.setString(2, request.getTransactionRef());
      ps.setString(3, request.getTransactionType());
      ps.setTimestamp(4, java.sql.Timestamp.from(clock.instant()));
      ps.executeUpdate();
    } catch (SQLException e) {
      if (SqlExceptionTranslator.isConstraintViolation(e)) {
        throw new TransferRequestExistsException(request.getTransactionRef());
      }
      throw e;
    }

    String legSql =
        "INSERT INTO transaction_leg"
            + " (client_ref, transaction_ref, account_ref, amount, currency)"
            + " VALUES (?, ?, ?, ?, ?)";
    try (PreparedStatement ps = conn.prepareStatement(legSql)) {
      for (TransactionLeg leg : request.getLegs()) {
        ps.setString(1, clientRef);
        ps.setString(2, request.getTransactionRef());
        ps.setString(3, leg.getAccountRef());
        ps.setString(4, leg.getAmount().getAmount().toPlainString());
        ps.setString(5, leg.getAmount().getCurrency().getCurrencyCode());
        ps.addBatch();
      }
      ps.executeBatch();
    }
  }

  @Override
  public Set<String> getTransactionRefsForAccount(String accountRef) {
    String sql =
        "SELECT transaction_ref FROM transaction_leg WHERE client_ref = ? AND account_ref = ?";
    try (Connection conn = dataSource.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, clientRef);
      ps.setString(2, accountRef);
      try (ResultSet rs = ps.executeQuery()) {
        Set<String> refs = new HashSet<>();
        while (rs.next()) {
          refs.add(rs.getString(1));
        }
        return refs;
      }
    } catch (SQLException e) {
      throw SqlExceptionTranslator.infrastructure(e);
    }
  }

  @Override
  public List<Transaction> getTransactions(List<String> transactionRefs) {
    if (transactionRefs.isEmpty()) {
      return new ArrayList<>();
    }
    String placeholders = transactionRefs.stream().map(ref -> "?").collect(Collectors.joining(","));
    String sql =
        "SELECT transaction_ref, transaction_type, transaction_date"
            + " FROM transaction_history WHERE client_ref = ? AND transaction_ref IN ("
            + placeholders
            + ")";
    try (Connection conn = dataSource.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, clientRef);
      for (int i = 0; i < transactionRefs.size(); i++) {
        ps.setString(i + 2, transactionRefs.get(i));
      }
      try (ResultSet rs = ps.executeQuery()) {
        List<Transaction> transactions = new ArrayList<>();
        while (rs.next()) {
          String txRef = rs.getString("transaction_ref");
          transactions.add(
              new Transaction(
                  txRef,
                  rs.getString("transaction_type"),
                  JdbcMappers.transactionDate(rs),
                  legs(txRef)));
        }
        return transactions;
      }
    } catch (SQLException e) {
      throw SqlExceptionTranslator.infrastructure(e);
    }
  }

  @Override
  public List<Transaction> getAllTransactions() {
    String historySql =
        "SELECT transaction_ref, transaction_type, transaction_date"
            + " FROM transaction_history WHERE client_ref = ?"
            + " ORDER BY transaction_date, transaction_ref";
    String legSql =
        "SELECT transaction_ref, account_ref, amount, currency FROM transaction_leg"
            + " WHERE client_ref = ?";
    try (Connection conn = dataSource.getConnection()) {
      Map<String, List<TransactionLeg>> legsByTransaction = new LinkedHashMap<>();
      try (PreparedStatement ps = conn.prepareStatement(legSql)) {
        ps.setString(1, clientRef);
        try (ResultSet rs = ps.executeQuery()) {
          while (rs.next()) {
            legsByTransaction
                .computeIfAbsent(rs.getString("transaction_ref"), ref -> new ArrayList<>())
                .add(JdbcMappers.leg(rs));
          }
        }
      }
      try (PreparedStatement ps = conn.prepareStatement(historySql)) {
        ps.setString(1, clientRef);
        try (ResultSet rs = ps.executeQuery()) {
          List<Transaction> transactions = new ArrayList<>();
          while (rs.next()) {
            String transactionRef = rs.getString("transaction_ref");
            transactions.add(
                new Transaction(
                    transactionRef,
                    rs.getString("transaction_type"),
                    JdbcMappers.transactionDate(rs),
                    legsByTransaction.getOrDefault(transactionRef, List.of())));
          }
          return transactions;
        }
      }
    } catch (SQLException e) {
      throw SqlExceptionTranslator.infrastructure(e);
    }
  }

  @Override
  public Transaction getTransactionByRef(String transactionRef) {
    List<Transaction> list = getTransactions(List.of(transactionRef));
    return list.isEmpty() ? null : list.get(0);
  }

  private List<TransactionLeg> legs(String transactionRef) {
    String sql =
        "SELECT account_ref, amount, currency FROM transaction_leg"
            + " WHERE client_ref = ? AND transaction_ref = ?";
    try (Connection conn = dataSource.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, clientRef);
      ps.setString(2, transactionRef);
      try (ResultSet rs = ps.executeQuery()) {
        List<TransactionLeg> legs = new ArrayList<>();
        while (rs.next()) {
          legs.add(JdbcMappers.leg(rs));
        }
        return legs;
      }
    } catch (SQLException e) {
      throw SqlExceptionTranslator.infrastructure(e);
    }
  }

  @Override
  public String getClientRef() {
    return clientRef;
  }
}
