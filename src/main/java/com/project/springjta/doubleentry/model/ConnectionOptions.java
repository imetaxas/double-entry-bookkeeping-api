package com.project.springjta.doubleentry.model;

import com.project.springjta.doubleentry.DataSourceDriver;

/**
 * @author yanimetaxas
 * @since 24-Mar-18
 */
public class ConnectionOptions {

  public static final ConnectionOptions NO_CONNECTION = new ConnectionOptions(DataSourceDriver.IN_MEMORY, "", "", "");

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
