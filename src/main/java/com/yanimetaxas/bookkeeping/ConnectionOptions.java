package com.yanimetaxas.bookkeeping;

import static com.yanimetaxas.bookkeeping.DataSourceDriver.EMBEDDED_DERBY;
import static com.yanimetaxas.bookkeeping.DataSourceDriver.EMBEDDED_H2;
import static com.yanimetaxas.bookkeeping.DataSourceDriver.EMBEDDED_HSQL;

/**
 * @author yanimetaxas
 * @since 24-Mar-18
 */
public class ConnectionOptions {

  public static final ConnectionOptions EMBEDDED_H2_CONNECTION = new ConnectionOptions(EMBEDDED_H2);

  public static final ConnectionOptions EMBEDDED_DERBY_CONNECTION = new ConnectionOptions(
      EMBEDDED_DERBY);

  public static final ConnectionOptions EMBEDDED_HSQL_CONNECTION = new ConnectionOptions(
      EMBEDDED_HSQL);

  private String driverClassName;
  private String url;
  private String username;
  private String password;
  private String schema;

  public ConnectionOptions(DataSourceDriver dataSourceDriver) {
    this.driverClassName = dataSourceDriver.getDriverClassName();
    this.url = dataSourceDriver.getUrl();
    this.username = dataSourceDriver.getUsername();
    this.password = dataSourceDriver.getPassword();
    this.schema = dataSourceDriver.getSchema();
  }

  public ConnectionOptions(String driverClassName) {
    this.driverClassName = driverClassName;
  }

  public ConnectionOptions url(String url) {
    this.url = url;
    return this;
  }

  public ConnectionOptions username(String username) {
    this.username = username;
    return this;
  }

  public ConnectionOptions password(String password) {
    this.password = password;
    return this;
  }

  public ConnectionOptions schema(String schema) {
    this.schema = schema;
    return this;
  }

  public String driverClassName() {
    return this.driverClassName;
  }

  public String url() {
    return this.url;
  }

  public String username() {
    return this.username;
  }

  public String password() {
    return this.password;
  }

  public String schema() {
    return schema;
  }
}
