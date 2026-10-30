package com.yanimetaxas.bookkeeping.jdbc;

import com.yanimetaxas.bookkeeping.exception.InfrastructureException;
import java.sql.SQLException;

/** Translates driver-specific SQL errors into bookkeeping exceptions. */
final class SqlExceptionTranslator {

  private SqlExceptionTranslator() {}

  static boolean isConstraintViolation(SQLException exception) {
    String state = exception.getSQLState();
    return "23505".equals(state) || "23000".equals(state) || exception.getErrorCode() == 19;
  }

  static InfrastructureException infrastructure(SQLException exception) {
    return new InfrastructureException(exception);
  }
}
