package com.yanimetaxas.bookkeeping.model;

import static com.yanimetaxas.bookkeeping.DataSourceDriver.EMBEDDED_DERBY;
import static com.yanimetaxas.bookkeeping.DataSourceDriver.EMBEDDED_H2;
import static com.yanimetaxas.bookkeeping.DataSourceDriver.EMBEDDED_HSQL;

import com.yanimetaxas.bookkeeping.DataSourceDriver;

/**
 * @author yanimetaxas
 * @since 24-Mar-18
 */
public class ConnectionOptions {

  public static final ConnectionOptions EMBEDDED_H2_CONNECTION = new ConnectionOptions(
      EMBEDDED_H2.getDriverClassName(),
      EMBEDDED_H2.getUrl(),
      EMBEDDED_H2.getUsername(),
      EMBEDDED_H2.getPassword(),
      EMBEDDED_H2.getSchema());

  public static final ConnectionOptions EMBEDDED_DERBY_CONNECTION = new ConnectionOptions(
      EMBEDDED_DERBY.getDriverClassName(),
      EMBEDDED_DERBY.getUrl(),
      EMBEDDED_DERBY.getUsername(),
      EMBEDDED_DERBY.getPassword(),
      EMBEDDED_DERBY.getSchema());

  public static final ConnectionOptions EMBEDDED_HSQL_CONNECTION = new ConnectionOptions(
      EMBEDDED_HSQL.getDriverClassName(),
      EMBEDDED_HSQL.getUrl(),
      EMBEDDED_HSQL.getUsername(),
      EMBEDDED_HSQL.getPassword(),
      EMBEDDED_HSQL.getSchema());

  private String driverClassName;
  private String url;
  private String username;
  private String password;
  private String schema;

  public ConnectionOptions(DataSourceDriver dataSourceDriver, String url, String username,
      String password) {
    this.driverClassName = dataSourceDriver.getDriverClassName();
    this.url = url;
    this.username = username;
    this.password = password;
    this.schema = dataSourceDriver.getSchema();
  }

  public ConnectionOptions(String driverClassName, String url, String username, String password,
      String schema) {
    this.driverClassName = driverClassName;
    this.url = url;
    this.username = username;
    this.password = password;
    this.schema = schema;
  }

  public String getDriverClassName() {
    return this.driverClassName;
  }

  public String getUrl() {
    return this.url;
  }

  public String getUsername() {
    return this.username;
  }

  public String getPassword() {
    return this.password;
  }

  public String getSchema() {
    return schema;
  }
}
