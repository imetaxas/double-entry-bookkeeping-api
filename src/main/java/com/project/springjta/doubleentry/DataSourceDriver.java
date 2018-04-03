package com.project.springjta.doubleentry;

/**
 * @author yanimetaxas
 * @since 24-Mar-18
 */
public enum DataSourceDriver {

  IN_MEMORY(null),
  H2_EMBEDDED("org.h2.Driver"),
  MYSQL("com.mysql.cj.jdbc.Driver"),
  POSTGRES("org.postgresql.Driver");

  private final String driverClassName;

  DataSourceDriver(String driverClassName) {
    this.driverClassName = driverClassName;
  }

  public String getDriverClassName() {
    return driverClassName;
  }
}
