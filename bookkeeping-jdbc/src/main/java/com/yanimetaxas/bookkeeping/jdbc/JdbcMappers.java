package com.yanimetaxas.bookkeeping.jdbc;

import com.yanimetaxas.bookkeeping.model.Account;
import com.yanimetaxas.bookkeeping.model.AccountSide;
import com.yanimetaxas.bookkeeping.model.Money;
import com.yanimetaxas.bookkeeping.model.TransactionLeg;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Currency;

final class JdbcMappers {

  private JdbcMappers() {}

  static Account account(ResultSet rs) throws SQLException {
    String normalBalance = rs.getString("normal_balance");
    Currency currency = Currency.getInstance(rs.getString("currency"));
    return new Account(
        rs.getString("account_ref"),
        new Money(amount(rs, "amount"), currency),
        new Money(amount(rs, "opening_amount"), currency),
        normalBalance == null ? null : AccountSide.valueOf(normalBalance));
  }

  static TransactionLeg leg(ResultSet rs) throws SQLException {
    return new TransactionLeg(
        rs.getString("account_ref"),
        new Money(amount(rs), Currency.getInstance(rs.getString("currency"))));
  }

  static BigDecimal amount(ResultSet rs) throws SQLException {
    return amount(rs, "amount");
  }

  static BigDecimal amount(ResultSet rs, String column) throws SQLException {
    return new BigDecimal(rs.getString(column));
  }

  /** Reads a timestamp. SQLite stores {@code setTimestamp} as epoch milliseconds in a text column. */
  static Instant transactionDate(ResultSet rs) throws SQLException {
    String raw = rs.getString("transaction_date");
    if (raw != null && raw.chars().allMatch(Character::isDigit)) {
      return Instant.ofEpochMilli(Long.parseLong(raw));
    }
    return rs.getTimestamp("transaction_date").toInstant();
  }
}
