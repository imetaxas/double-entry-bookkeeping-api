package com.yanimetaxas.bookkeeping.spring;

import static io.github.imetaxas.realitycheck.RealityAssertions.assertThat;

import com.yanimetaxas.bookkeeping.Ledger;
import com.yanimetaxas.bookkeeping.StorageProvider;
import com.yanimetaxas.bookkeeping.memory.InMemoryStorageProvider;
import com.yanimetaxas.bookkeeping.model.Money;
import java.lang.reflect.Method;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

class BookkeepingAutoConfigurationTest {

  @Test
  void autoConfigurationDisabledWhenNoLedgerNameDefined() {
    run(
        context -> {
          assertThat(context.getBeansOfType(Ledger.class)).isEmpty();
          assertThat(context.getBeansOfType(StorageProvider.class)).isEmpty();
        });
  }

  @Test
  void inMemoryLedgerCreatedWhenAccountsConfigured() {
    run(
        context -> {
          assertThat(context.getBeansOfType(Ledger.class)).hasSize(1);
          assertThat(context.getBeansOfType(StorageProvider.class)).hasSize(1);
          assertThat(context.getBean(StorageProvider.class))
              .isInstanceOf(InMemoryStorageProvider.class);
        },
        "bookkeeping.ledger-name=Test Ledger",
        "bookkeeping.accounts.Cash.amount=1000.00",
        "bookkeeping.accounts.Cash.currency=EUR",
        "bookkeeping.accounts.Cash.side=DEBIT",
        "bookkeeping.accounts.Revenue.amount=0.00",
        "bookkeeping.accounts.Revenue.currency=EUR",
        "bookkeeping.accounts.Revenue.side=CREDIT");
  }

  @Test
  void accountsWithoutSideAreSupported() {
    run(
        context -> assertThat(context.getBeansOfType(Ledger.class)).hasSize(1),
        "bookkeeping.ledger-name=Simple Ledger",
        "bookkeeping.accounts.Cash.amount=500.00",
        "bookkeeping.accounts.Cash.currency=USD",
        "bookkeeping.accounts.Expenses.amount=0.00",
        "bookkeeping.accounts.Expenses.currency=USD");
  }

  @Test
  void jdbcUrlWithoutDriverFailsWithAClearMessage() {
    Throwable root =
        rootCause(
            failure(
                "bookkeeping.ledger-name=JDBC Ledger",
                "bookkeeping.jdbc.url=jdbc:h2:mem:bookkeeping",
                "bookkeeping.accounts.Cash.amount=1.00",
                "bookkeeping.accounts.Cash.currency=EUR"));

    assertThat(root).isInstanceOf(IllegalStateException.class);
    assertThat(root.getMessage()).contains("bookkeeping.jdbc.driver-class-name");
  }

  @Test
  void blankJdbcDriverFailsWithAClearMessage() {
    Throwable root =
        rootCause(
            failure(
                "bookkeeping.ledger-name=JDBC Ledger",
                "bookkeeping.jdbc.url=jdbc:h2:mem:blank-driver",
                "bookkeeping.jdbc.driver-class-name= ",
                "bookkeeping.accounts.Cash.amount=1.00",
                "bookkeeping.accounts.Cash.currency=EUR"));

    assertThat(root.getMessage()).contains("bookkeeping.jdbc.driver-class-name");
  }

  @Test
  void jdbcLedgerIsCreatedWhenUrlAndDriverAreSet() {
    run(
        context -> {
          Ledger ledger = context.getBean(Ledger.class);
          assertThat(ledger.getAccountBalance("Cash")).isEqualTo(Money.toMoney("10.00", "EUR"));
          assertThat(ledger.getAccountBalance("Revenue")).isEqualTo(Money.toMoney("0.00", "EUR"));
        },
        "bookkeeping.ledger-name=JDBC Ledger",
        "bookkeeping.jdbc.driver-class-name=org.h2.Driver",
        "bookkeeping.jdbc.url=jdbc:h2:mem:spring-ledger;DB_CLOSE_DELAY=-1",
        "bookkeeping.jdbc.username=sa",
        "bookkeeping.jdbc.password=",
        "bookkeeping.jdbc.schema=h2-schema.sql",
        "bookkeeping.accounts.Cash.amount=10.00",
        "bookkeeping.accounts.Cash.currency=EUR",
        "bookkeeping.accounts.Cash.side=debit",
        "bookkeeping.accounts.Revenue.amount=0.00",
        "bookkeeping.accounts.Revenue.currency=EUR");
  }

  @Test
  void unknownJdbcDriverIsReportedAsAMissingDriver() {
    Throwable failure =
        failure(
            "bookkeeping.ledger-name=JDBC Ledger",
            "bookkeeping.jdbc.driver-class-name=com.missing.Driver",
            "bookkeeping.jdbc.url=jdbc:h2:mem:missing-driver",
            "bookkeeping.accounts.Cash.amount=1.00",
            "bookkeeping.accounts.Cash.currency=EUR");

    assertThat(failure).isNotNull();
    assertThat(failure.getMessage()).contains("JDBC driver not found");
  }

  @Test
  void ledgerUsesTheApplicationClock() {
    Instant posted = Instant.parse("2024-05-06T07:08:09Z");
    try (ConfigurableApplicationContext context =
        open(null, Clock.fixed(posted, ZoneOffset.UTC),
            "bookkeeping.ledger-name=Clock Ledger",
            "bookkeeping.accounts.Cash.amount=10.00",
            "bookkeeping.accounts.Cash.currency=EUR",
            "bookkeeping.accounts.Revenue.amount=0.00",
            "bookkeeping.accounts.Revenue.currency=EUR")) {
      context.refresh();
      Ledger ledger = context.getBean(Ledger.class);
      ledger.commit(
          ledger
              .createTransferRequest()
              .reference("T1")
              .type("sale")
              .account("Cash")
              .debit("1.00", "EUR")
              .account("Revenue")
              .credit("1.00", "EUR")
              .build());
      assertThat(ledger.getTransactionByRef("T1").getTransactionDate()).isEqualTo(posted);
    }
  }

  @Test
  void aDriverThatFailsToInitializeIsReportedAsAnError() {
    assertThat(
            rootCause(
                failure(
                    "bookkeeping.ledger-name=Boom Ledger",
                    "bookkeeping.jdbc.driver-class-name=com.yanimetaxas.bookkeeping.spring.StaticFailureDriver",
                    "bookkeeping.jdbc.url=jdbc:h2:mem:boom",
                    "bookkeeping.accounts.Cash.amount=1.00",
                    "bookkeeping.accounts.Cash.currency=EUR")))
        .isInstanceOf(ExceptionInInitializerError.class);
  }

  @Test
  void jdbcTypesLoadThroughTheConfigurationClassLoaderWhenTheContextLoaderIsAbsent()
      throws Exception {
    ClassLoader previous = Thread.currentThread().getContextClassLoader();
    Thread.currentThread().setContextClassLoader(null);
    try {
      Method load = BookkeepingAutoConfiguration.class.getDeclaredMethod("load", String.class);
      load.setAccessible(true);
      Class<?> type =
          (Class<?>) load.invoke(null, "com.yanimetaxas.bookkeeping.jdbc.ConnectionOptions");
      assertThat(type.getName()).isEqualTo("com.yanimetaxas.bookkeeping.jdbc.ConnectionOptions");
    } finally {
      Thread.currentThread().setContextClassLoader(previous);
    }
  }

  @Test
  void missingJdbcModuleReportsThatTheDependencyIsAbsent() {
    Throwable failure =
        failure(
            new FilteredClassLoader("com.yanimetaxas.bookkeeping.jdbc"),
            "bookkeeping.ledger-name=Hidden Ledger",
            "bookkeeping.jdbc.driver-class-name=org.h2.Driver",
            "bookkeeping.jdbc.url=jdbc:h2:mem:hidden",
            "bookkeeping.accounts.Cash.amount=1.00",
            "bookkeeping.accounts.Cash.currency=EUR");

    assertThat(failure).isNotNull();
    assertThat(failure.getMessage()).contains("bookkeeping-jdbc");
  }

  private static void run(Consumer<ConfigurableApplicationContext> check, String... properties) {
    try (ConfigurableApplicationContext context = open(null, null, properties)) {
      context.refresh();
      check.accept(context);
    }
  }

  private static Throwable failure(String... properties) {
    return failure(null, properties);
  }

  private static Throwable failure(ClassLoader loader, String... properties) {
    ClassLoader previous = Thread.currentThread().getContextClassLoader();
    if (loader != null) {
      Thread.currentThread().setContextClassLoader(loader);
    }
    try (ConfigurableApplicationContext context = open(loader, null, properties)) {
      context.refresh();
      return null;
    } catch (Throwable thrown) {
      return thrown;
    } finally {
      Thread.currentThread().setContextClassLoader(previous);
    }
  }

  private static AnnotationConfigApplicationContext open(
      ClassLoader loader, Clock clock, String... properties) {
    AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
    if (loader != null) {
      context.setClassLoader(loader);
    }
    if (clock != null) {
      context.registerBean(Clock.class, () -> clock);
    }
    if (properties.length > 0) {
      TestPropertyValues.of(properties).applyTo(context);
    }
    context.register(BookkeepingAutoConfiguration.class);
    return context;
  }

  private static Throwable rootCause(Throwable failure) {
    Throwable current = failure;
    while (current.getCause() != null && current.getCause() != current) {
      current = current.getCause();
    }
    return current;
  }
}
