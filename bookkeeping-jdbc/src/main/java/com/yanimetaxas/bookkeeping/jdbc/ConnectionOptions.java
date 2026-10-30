package com.yanimetaxas.bookkeeping.jdbc;

import com.yanimetaxas.bookkeeping.StorageFactory;
import com.yanimetaxas.bookkeeping.StorageProvider;

/**
 * Immutable JDBC connection configuration for a bookkeeping database.
 *
 * <p>The embedded constants can be customized without affecting other callers because each
 * fluent method returns a new instance:
 *
 * <pre>{@code
 * new LedgerBuilder(chart)
 *     .options(ConnectionOptions.EMBEDDED_H2_CONNECTION)
 *     .build().init();
 * }</pre>
 *
 * @author yanimetaxas
 * @since 24-Mar-18
 */
public final class ConnectionOptions implements StorageFactory {

  public static final ConnectionOptions EMBEDDED_H2_CONNECTION =
      new ConnectionOptions(DataSourceDriver.EMBEDDED_H2);

  public static final ConnectionOptions EMBEDDED_DERBY_CONNECTION =
      new ConnectionOptions(DataSourceDriver.EMBEDDED_DERBY);

  public static final ConnectionOptions EMBEDDED_HSQL_CONNECTION =
      new ConnectionOptions(DataSourceDriver.EMBEDDED_HSQL);

  private final String driverClassName;
  private final String url;
  private final String username;
  private final String password;
  private final String schema;

  public ConnectionOptions(DataSourceDriver dataSourceDriver) {
    this(
        dataSourceDriver.getDriverClassName(),
        dataSourceDriver.getUrl(),
        dataSourceDriver.getUsername(),
        dataSourceDriver.getPassword(),
        dataSourceDriver.getSchema());
  }

  public ConnectionOptions(String driverClassName) {
    this(driverClassName, null, null, null, null);
  }

  private ConnectionOptions(
      String driverClassName, String url, String username, String password, String schema) {
    this.driverClassName = driverClassName;
    this.url = url;
    this.username = username;
    this.password = password;
    this.schema = schema;
  }

  @Override
  public StorageProvider createStorage() {
    return new JdbcStorageProvider(this);
  }

  public ConnectionOptions url(String url) {
    return new ConnectionOptions(driverClassName, url, username, password, schema);
  }

  public ConnectionOptions username(String username) {
    return new ConnectionOptions(driverClassName, url, username, password, schema);
  }

  public ConnectionOptions password(String password) {
    return new ConnectionOptions(driverClassName, url, username, password, schema);
  }

  public ConnectionOptions schema(String schema) {
    return new ConnectionOptions(driverClassName, url, username, password, schema);
  }

  public String driverClassName() {
    return driverClassName;
  }

  public String url() {
    return url;
  }

  public String username() {
    return username;
  }

  public String password() {
    return password;
  }

  public String schema() {
    return schema;
  }
}
