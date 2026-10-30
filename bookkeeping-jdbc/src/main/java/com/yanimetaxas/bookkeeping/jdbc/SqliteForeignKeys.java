package com.yanimetaxas.bookkeeping.jdbc;

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.Statement;
import java.util.logging.Logger;
import javax.sql.DataSource;

/** Turns on SQLite foreign keys for every connection taken from a delegated {@link DataSource}. */
final class SqliteForeignKeys implements DataSource {

  private final DataSource delegate;

  private SqliteForeignKeys(DataSource delegate) {
    this.delegate = delegate;
  }

  static DataSource enable(DataSource dataSource) {
    if (dataSource instanceof SqliteForeignKeys) {
      return dataSource;
    }
    return new SqliteForeignKeys(dataSource);
  }

  static Connection on(Connection connection) throws SQLException {
    String url = connection.getMetaData().getURL();
    if (url != null && url.regionMatches(true, 0, "jdbc:sqlite:", 0, "jdbc:sqlite:".length())) {
      try (Statement statement = connection.createStatement()) {
        statement.execute("PRAGMA foreign_keys = ON");
      }
    }
    return connection;
  }

  @Override
  public Connection getConnection() throws SQLException {
    return on(delegate.getConnection());
  }

  @Override
  public Connection getConnection(String username, String password) throws SQLException {
    return on(delegate.getConnection(username, password));
  }

  @Override
  public PrintWriter getLogWriter() throws SQLException {
    return delegate.getLogWriter();
  }

  @Override
  public void setLogWriter(PrintWriter out) throws SQLException {
    delegate.setLogWriter(out);
  }

  @Override
  public void setLoginTimeout(int seconds) throws SQLException {
    delegate.setLoginTimeout(seconds);
  }

  @Override
  public int getLoginTimeout() throws SQLException {
    return delegate.getLoginTimeout();
  }

  @Override
  public Logger getParentLogger() throws SQLFeatureNotSupportedException {
    return delegate.getParentLogger();
  }

  @Override
  public <T> T unwrap(Class<T> iface) throws SQLException {
    return delegate.unwrap(iface);
  }

  @Override
  public boolean isWrapperFor(Class<?> iface) throws SQLException {
    return delegate.isWrapperFor(iface);
  }
}
