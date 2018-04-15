package com.yanimetaxas.doubleentry;

/**
 * @author yanimetaxas
 * @since 24-Mar-18
 */
public enum DataSourceDriver {

  // EmbeddedDatabaseType.H2
  EMBEDDED_H2("org.h2.Driver", "jdbc:h2:mem:dataSource;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=false",
      "sa", "", "h2-schema.sql"),
  EMBEDDED_HSQL("org.hsqldb.jdbcDriver", "jdbc:hsqldb:mem:dataSource", "sa", "", "h2-schema.sql"),
  EMBEDDED_DERBY("org.apache.derby.jdbc.EmbeddedDriver", "jdbc:derby:memory:dataSource;create=true",
      "sa", "", "derby-schema.sql"),
  JDBC_H2("org.h2.Driver", "", "", "", "h2-schema.sql"),
  JDBC_MYSQL("com.mysql.cj.jdbc.Driver", "", "", "", "h2-schema.sql"),
  JDBC_POSTGRES("org.postgresql.Driver", "", "", "", "h2-schema.sql");

  private final String driverClassName;
  private final String url;
  private final String username;
  private final String password;
  private final String schema;

  DataSourceDriver(String driverClassName, String url, String username, String password,
      String schema) {
    this.driverClassName = driverClassName;
    this.url = url;
    this.username = username;
    this.password = password;
    this.schema = schema;
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

  public String getSchema() {
    return schema;
  }

  @Override
  public String toString() {
    return driverClassName;
  }
}

