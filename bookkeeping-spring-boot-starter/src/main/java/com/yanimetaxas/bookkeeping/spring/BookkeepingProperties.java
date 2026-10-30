package com.yanimetaxas.bookkeeping.spring;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for the bookkeeping starter, bound from
 * {@code bookkeeping.*} in {@code application.yml}.
 *
 * <p>Minimal example (in-memory):
 * <pre>{@code
 * bookkeeping:
 *   ledger-name: "My Ledger"
 *   accounts:
 *     Cash:    { amount: "10000.00", currency: EUR, side: DEBIT }
 *     Revenue: { amount: "0.00",     currency: EUR, side: CREDIT }
 * }</pre>
 *
 * <p>With JDBC persistence:
 * <pre>{@code
 * bookkeeping:
 *   ledger-name: "Production Ledger"
 *   jdbc:
 *     driver-class-name: org.h2.Driver
 *     url: jdbc:h2:mem:bookkeeping;DB_CLOSE_DELAY=-1
 *     username: sa
 *     password: ""
 *     schema: h2-schema.sql
 *   accounts:
 *     Cash:    { amount: "10000.00", currency: EUR, side: DEBIT }
 *     Revenue: { amount: "0.00",     currency: EUR, side: CREDIT }
 * }</pre>
 */
@ConfigurationProperties(prefix = "bookkeeping")
public class BookkeepingProperties {

  private String ledgerName = "General Ledger";
  private Map<String, AccountProperties> accounts = new LinkedHashMap<>();
  private JdbcProperties jdbc;

  public String getLedgerName() {
    return ledgerName;
  }

  public void setLedgerName(String ledgerName) {
    this.ledgerName = ledgerName;
  }

  public Map<String, AccountProperties> getAccounts() {
    return accounts;
  }

  public void setAccounts(Map<String, AccountProperties> accounts) {
    this.accounts = accounts;
  }

  public JdbcProperties getJdbc() {
    return jdbc;
  }

  public void setJdbc(JdbcProperties jdbc) {
    this.jdbc = jdbc;
  }

  public static class AccountProperties {
    private String amount = "0.00";
    private String currency = "USD";
    private String side;

    public String getAmount() {
      return amount;
    }

    public void setAmount(String amount) {
      this.amount = amount;
    }

    public String getCurrency() {
      return currency;
    }

    public void setCurrency(String currency) {
      this.currency = currency;
    }

    public String getSide() {
      return side;
    }

    public void setSide(String side) {
      this.side = side;
    }
  }

  public static class JdbcProperties {
    private String driverClassName;
    private String url;
    private String username = "";
    private String password = "";
    private String schema = "h2-schema.sql";

    public String getDriverClassName() {
      return driverClassName;
    }

    public void setDriverClassName(String driverClassName) {
      this.driverClassName = driverClassName;
    }

    public String getUrl() {
      return url;
    }

    public void setUrl(String url) {
      this.url = url;
    }

    public String getUsername() {
      return username;
    }

    public void setUsername(String username) {
      this.username = username;
    }

    public String getPassword() {
      return password;
    }

    public void setPassword(String password) {
      this.password = password;
    }

    public String getSchema() {
      return schema;
    }

    public void setSchema(String schema) {
      this.schema = schema;
    }
  }
}
