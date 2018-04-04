package com.yanimetaxas.doubleentry.model;

import com.yanimetaxas.doubleentry.DataSourceDriver;

/**
 * @author yanimetaxas
 * @since 24-Mar-18
 */
public class ConnectionOptions {

  public static final ConnectionOptions NO_CONNECTION = new ConnectionOptions(
      DataSourceDriver.EMBEDDED_H2, DataSourceDriver.EMBEDDED_H2.getUrl(),
      DataSourceDriver.EMBEDDED_H2.getUsername(), DataSourceDriver.EMBEDDED_H2.getPassword());

  private DataSourceDriver driver;
  private String url;
  private String username;
  private String password;

  public ConnectionOptions(DataSourceDriver driver, String url, String username, String password) {
    this.driver = driver;
    this.url = url;
    this.username = username;
    this.password = password;
  }

  public DataSourceDriver getDriver() {
    return driver;
  }

  public String getUrl() {
    return url;
  }

  public String getUsername() {
    return username;
  }

  public String getPassword() {
    return password;
  }
}
