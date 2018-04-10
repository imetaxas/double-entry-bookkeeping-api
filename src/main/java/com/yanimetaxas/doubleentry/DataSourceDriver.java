package com.yanimetaxas.doubleentry;

/**
 * @author yanimetaxas
 * @since 24-Mar-18
 */
public enum DataSourceDriver {

  // EmbeddedDatabaseType.H2
  EMBEDDED_H2("org.h2.Driver", "jdbc:h2:mem:dataSource;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=false", "sa", ""),
  JDBC_H2("org.h2.Driver", "", "", ""),
  JDBC_MYSQL("com.mysql.cj.jdbc.Driver", "", "", ""),
  JDBC_POSTGRES("org.postgresql.Driver", "", "", "");

  private final String driverClassName;
  private final String url;
  private final String username;
  private final String password;

  DataSourceDriver(String driverClassName, String url, String username, String password) {
    this.driverClassName = driverClassName;
    this.url = url;
    this.username = username;
    this.password = password;
  }

  public String getDriverClassName() {
    return driverClassName;
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
