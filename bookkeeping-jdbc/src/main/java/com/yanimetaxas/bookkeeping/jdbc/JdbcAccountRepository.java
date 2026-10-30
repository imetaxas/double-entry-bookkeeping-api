package com.yanimetaxas.bookkeeping.jdbc;

import com.yanimetaxas.bookkeeping.dao.AccountDao;
import com.yanimetaxas.bookkeeping.exception.AccountNotFoundException;
import com.yanimetaxas.bookkeeping.model.Account;
import com.yanimetaxas.bookkeeping.model.AccountSide;
import com.yanimetaxas.bookkeeping.model.Money;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import javax.sql.DataSource;

/** Accounts for one ledger name. Reads and writes include {@code client_ref}. */
final class JdbcAccountRepository implements AccountDao {

  private final DataSource dataSource;
  private final String clientRef;

  JdbcAccountRepository(DataSource dataSource, String clientRef) {
    this.dataSource = dataSource;
    this.clientRef = clientRef;
  }

  @Override
  public boolean accountExists(String accountRef) {
    String sql = "SELECT id FROM account WHERE client_ref = ? AND account_ref = ?";
    try (Connection conn = dataSource.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, clientRef);
      ps.setString(2, accountRef);
      try (ResultSet rs = ps.executeQuery()) {
        return rs.next();
      }
    } catch (SQLException e) {
      throw SqlExceptionTranslator.infrastructure(e);
    }
  }

  @Override
  public void createAccount(
      String clientRef, String accountRef, Money initialAmount, AccountSide normalBalance) {
    String sql =
        "INSERT INTO account (client_ref, account_ref, currency, amount, opening_amount, normal_balance)"
            + " VALUES (?, ?, ?, ?, ?, ?)";
    try (Connection conn = dataSource.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, clientRef);
      ps.setString(2, accountRef);
      ps.setString(3, initialAmount.getCurrency().getCurrencyCode());
      String amount = initialAmount.getAmount().toPlainString();
      ps.setString(4, amount);
      ps.setString(5, amount);
      ps.setString(6, normalBalance == null ? null : normalBalance.name());
      ps.executeUpdate();
    } catch (SQLException e) {
      throw SqlExceptionTranslator.infrastructure(e);
    }
  }

  @Override
  public Account getAccount(String accountRef) {
    try (Connection conn = dataSource.getConnection()) {
      return load(conn, accountRef);
    } catch (SQLException e) {
      throw SqlExceptionTranslator.infrastructure(e);
    }
  }

  @Override
  public List<Account> getAccounts() {
    String sql =
        "SELECT account_ref, amount, opening_amount, currency, normal_balance FROM account"
            + " WHERE client_ref = ? ORDER BY account_ref";
    try (Connection conn = dataSource.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, clientRef);
      try (ResultSet rs = ps.executeQuery()) {
        List<Account> accounts = new ArrayList<>();
        while (rs.next()) {
          accounts.add(JdbcMappers.account(rs));
        }
        return accounts;
      }
    } catch (SQLException e) {
      throw SqlExceptionTranslator.infrastructure(e);
    }
  }

  Account load(Connection conn, String accountRef) {
    String sql =
        "SELECT account_ref, amount, opening_amount, currency, normal_balance FROM account"
            + " WHERE client_ref = ? AND account_ref = ?";
    try (PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, clientRef);
      ps.setString(2, accountRef);
      try (ResultSet rs = ps.executeQuery()) {
        return rs.next() ? JdbcMappers.account(rs) : Account.nullAccount();
      }
    } catch (SQLException e) {
      throw SqlExceptionTranslator.infrastructure(e);
    }
  }

  void updateAmount(Connection conn, String accountRef, BigDecimal amount) throws SQLException {
    String sql = "UPDATE account SET amount = ? WHERE client_ref = ? AND account_ref = ?";
    try (PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, amount.toPlainString());
      ps.setString(2, clientRef);
      ps.setString(3, accountRef);
      if (ps.executeUpdate() != 1) {
        throw new AccountNotFoundException(accountRef);
      }
    }
  }

  @Override
  public String getClientRef() {
    return clientRef;
  }
}
