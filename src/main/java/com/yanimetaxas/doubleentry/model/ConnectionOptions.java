package com.yanimetaxas.doubleentry.model;

import static com.yanimetaxas.doubleentry.DataSourceDriver.EMBEDDED_H2;

/**
 * @author yanimetaxas
 * @since 24-Mar-18
 */
public class ConnectionOptions {

  public static final ConnectionOptions NO_CONNECTION = new ConnectionOptions(
      EMBEDDED_H2.getDriverClassName(),
      EMBEDDED_H2.getUrl(),
      EMBEDDED_H2.getUsername(),
      EMBEDDED_H2.getPassword());

  private String driverClassName;
  private String url;
  private String username;
  private String password;

  public ConnectionOptions(String driverClassName, String url, String username, String password) {
    this.driverClassName = driverClassName;
    this.url = url;
    this.username = username;
    this.password = password;
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
}
