package com.yanimetaxas.bookkeeping.jdbc;

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.logging.Logger;
import javax.sql.DataSource;

/**
 * Minimal {@link DataSource} that delegates to {@link DriverManager}. Suitable for embedded
 * databases and lightweight usage. For production workloads with connection pooling, pass your own
 * DataSource (e.g., HikariCP) to {@link JdbcStorageProvider}.
 */
final class SimpleDataSource implements DataSource {

  private final String url;
  private final String username;
  private final String password;

  SimpleDataSource(String driverClassName, String url, String username, String password) {
    try {
      Class.forName(driverClassName);
    } catch (ClassNotFoundException e) {
      throw new IllegalArgumentException("JDBC driver not found: " + driverClassName, e);
    }
    this.url = url;
    this.username = username;
    this.password = password;
  }

  @Override
  public Connection getConnection() throws SQLException {
    return open(DriverManager.getConnection(url, username, password));
  }

  @Override
  public Connection getConnection(String username, String password) throws SQLException {
    return open(DriverManager.getConnection(url, username, password));
  }

  private Connection open(Connection connection) throws SQLException {
    return SqliteForeignKeys.on(connection);
  }

  @Override
  public PrintWriter getLogWriter() {
    return null;
  }

  @Override
  public void setLogWriter(PrintWriter out) {}

  @Override
  public void setLoginTimeout(int seconds) {}

  @Override
  public int getLoginTimeout() {
    return 0;
  }

  @Override
  public Logger getParentLogger() throws SQLFeatureNotSupportedException {
    throw new SQLFeatureNotSupportedException();
  }

  @Override
  public <T> T unwrap(Class<T> iface) throws SQLException {
    throw new SQLException("Not a wrapper");
  }

  @Override
  public boolean isWrapperFor(Class<?> iface) {
    return false;
  }
}
