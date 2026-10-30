package com.yanimetaxas.bookkeeping.examples;

import com.yanimetaxas.bookkeeping.Ledger;
import com.yanimetaxas.bookkeeping.model.Money;
import com.yanimetaxas.bookkeeping.spring.BookkeepingAutoConfiguration;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.Banner;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * Starts the Spring Boot starter twice: once with the in-memory default, then with an embedded H2
 * ledger configured from {@code bookkeeping.*} properties.
 *
 * <p>Run: {@code mvn -pl bookkeeping-examples -am compile -Prun-example
 * -Dexec.mainClass=com.yanimetaxas.bookkeeping.examples.SpringBootLedgerExample}
 */
@Slf4j
public class SpringBootLedgerExample {

  public static void main(String... args) {
    post("in-memory", inMemoryProperties());
    post("jdbc", jdbcProperties());
  }

  private static void post(String label, Map<String, Object> properties) {
    SpringApplicationBuilder application =
        new SpringApplicationBuilder(BookkeepingAutoConfiguration.class)
            .web(WebApplicationType.NONE)
            .bannerMode(Banner.Mode.OFF)
            .properties(properties);
    try (ConfigurableApplicationContext context = application.run()) {
      Ledger ledger = context.getBean(Ledger.class);
      ledger.commit(
          ledger
              .createTransferRequest()
              .reference("T1")
              .type("sale")
              .account("Cash")
              .debit("50.00", "EUR")
              .account("Revenue")
              .credit("50.00", "EUR")
              .build());
      expect(ledger.getAccountBalance("Cash"), "950.00", "EUR");
      expect(ledger.getAccountBalance("Revenue"), "50.00", "EUR");
      if (!ledger.computeTrialBalance().isBalanced()) {
        throw new IllegalStateException(label + " trial balance did not balance");
      }
      log.info("=== Spring Boot {} ledger ===", label);
      ledger.printTrialBalance();
    }
  }

  private static Map<String, Object> inMemoryProperties() {
    Map<String, Object> properties = accounts();
    properties.put("bookkeeping.ledger-name", "Spring In-Memory");
    return properties;
  }

  private static Map<String, Object> jdbcProperties() {
    Map<String, Object> properties = accounts();
    properties.put("bookkeeping.ledger-name", "Spring JDBC");
    properties.put("bookkeeping.jdbc.driver-class-name", "org.h2.Driver");
    properties.put(
        "bookkeeping.jdbc.url",
        "jdbc:h2:mem:spring-example-" + System.nanoTime() + ";DB_CLOSE_DELAY=-1");
    properties.put("bookkeeping.jdbc.username", "sa");
    properties.put("bookkeeping.jdbc.password", "");
    properties.put("bookkeeping.jdbc.schema", "h2-schema.sql");
    return properties;
  }

  private static Map<String, Object> accounts() {
    Map<String, Object> properties = new LinkedHashMap<>();
    properties.put("bookkeeping.accounts.Cash.amount", "1000.00");
    properties.put("bookkeeping.accounts.Cash.currency", "EUR");
    properties.put("bookkeeping.accounts.Cash.side", "DEBIT");
    properties.put("bookkeeping.accounts.Revenue.amount", "0.00");
    properties.put("bookkeeping.accounts.Revenue.currency", "EUR");
    properties.put("bookkeeping.accounts.Revenue.side", "CREDIT");
    properties.put("bookkeeping.accounts.Equity.amount", "1000.00");
    properties.put("bookkeeping.accounts.Equity.currency", "EUR");
    properties.put("bookkeeping.accounts.Equity.side", "CREDIT");
    return properties;
  }

  private static void expect(Money actual, String amount, String currency) {
    Money expected = Money.toMoney(amount, currency);
    if (!actual.equals(expected)) {
      throw new IllegalStateException("Expected " + expected + " but was " + actual);
    }
  }
}
