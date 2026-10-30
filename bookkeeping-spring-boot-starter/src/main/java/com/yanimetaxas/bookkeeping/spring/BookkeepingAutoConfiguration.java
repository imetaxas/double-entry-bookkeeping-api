package com.yanimetaxas.bookkeeping.spring;

import com.yanimetaxas.bookkeeping.ChartOfAccounts;
import com.yanimetaxas.bookkeeping.ChartOfAccounts.ChartOfAccountsBuilder;
import com.yanimetaxas.bookkeeping.Ledger;
import com.yanimetaxas.bookkeeping.Ledger.LedgerBuilder;
import com.yanimetaxas.bookkeeping.StorageProvider;
import com.yanimetaxas.bookkeeping.spring.BookkeepingProperties.AccountProperties;
import com.yanimetaxas.bookkeeping.spring.BookkeepingProperties.JdbcProperties;
import com.yanimetaxas.bookkeeping.model.AccountSide;
import java.lang.reflect.InvocationTargetException;
import java.time.Clock;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * Spring Boot auto-configuration for the double-entry bookkeeping engine.
 *
 * <p>Activated when {@code bookkeeping.ledger-name} is present in application properties.
 * Creates a {@link Ledger} bean backed by either in-memory or JDBC storage, depending on
 * whether {@code bookkeeping.jdbc.url} is configured.
 */
@Slf4j
@AutoConfiguration
@ConditionalOnClass(Ledger.class)
@ConditionalOnProperty(prefix = "bookkeeping", name = "ledger-name")
@EnableConfigurationProperties(BookkeepingProperties.class)
public class BookkeepingAutoConfiguration {

  @Bean
  @ConditionalOnMissingBean
  public ChartOfAccounts chartOfAccounts(BookkeepingProperties properties) {
    ChartOfAccountsBuilder builder = new ChartOfAccountsBuilder();
    for (Map.Entry<String, AccountProperties> entry : properties.getAccounts().entrySet()) {
      AccountProperties acct = entry.getValue();
      if (acct.getSide() != null) {
        builder.create(
            entry.getKey(),
            acct.getAmount(),
            acct.getCurrency(),
            AccountSide.valueOf(acct.getSide().toUpperCase()));
      } else {
        builder.create(entry.getKey(), acct.getAmount(), acct.getCurrency());
      }
    }
    return builder.build();
  }

  @Bean
  @ConditionalOnMissingBean
  public Ledger ledger(
      ChartOfAccounts chartOfAccounts,
      BookkeepingProperties properties,
      @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
          StorageProvider storageProvider,
      ObjectProvider<Clock> clocks) {
    log.info(
        "Creating bookkeeping ledger '{}' with {} accounts",
        properties.getLedgerName(),
        properties.getAccounts().size());
    LedgerBuilder builder =
        new LedgerBuilder(chartOfAccounts)
            .name(properties.getLedgerName())
            .storage(storageProvider);
    Clock clock = clocks.getIfAvailable();
    if (clock != null) {
      builder.clock(clock);
    }
    return builder.build().init();
  }

  @Bean
  @ConditionalOnMissingBean(StorageProvider.class)
  @ConditionalOnProperty(prefix = "bookkeeping.jdbc", name = "url")
  public StorageProvider jdbcStorageProvider(BookkeepingProperties properties) {
    JdbcProperties jdbc = properties.getJdbc();
    if (jdbc.getDriverClassName() == null || jdbc.getDriverClassName().isBlank()) {
      throw new IllegalStateException(
          "bookkeeping.jdbc.driver-class-name is required when bookkeeping.jdbc.url is set");
    }
    log.info("Configuring JDBC bookkeeping storage: {}", jdbc.getUrl());
    try {
      var optionsClass = load("com.yanimetaxas.bookkeeping.jdbc.ConnectionOptions");
      Object options =
          optionsClass
              .getConstructor(String.class)
              .newInstance(jdbc.getDriverClassName());

      options =
          optionsClass
              .getMethod("url", String.class)
              .invoke(options, jdbc.getUrl());
      options =
          optionsClass
              .getMethod("username", String.class)
              .invoke(options, jdbc.getUsername());
      options =
          optionsClass
              .getMethod("password", String.class)
              .invoke(options, jdbc.getPassword());
      options =
          optionsClass
              .getMethod("schema", String.class)
              .invoke(options, jdbc.getSchema());

      var providerClass = load("com.yanimetaxas.bookkeeping.jdbc.JdbcStorageProvider");
      return (StorageProvider) providerClass.getConstructor(optionsClass).newInstance(options);
    } catch (ReflectiveOperationException e) {
      Throwable cause = e instanceof InvocationTargetException && e.getCause() != null ? e.getCause() : e;
      if (cause instanceof RuntimeException runtime) {
        throw runtime;
      }
      if (cause instanceof Error error) {
        throw error;
      }
      throw new IllegalStateException(
          "bookkeeping-jdbc module is not on the classpath. "
              + "Add bookkeeping-jdbc as a dependency to use JDBC persistence.",
          cause);
    }
  }

  private static Class<?> load(String name) throws ClassNotFoundException {
    ClassLoader loader = Thread.currentThread().getContextClassLoader();
    if (loader == null) {
      loader = BookkeepingAutoConfiguration.class.getClassLoader();
    }
    return Class.forName(name, true, loader);
  }

  @Bean
  @ConditionalOnMissingBean(StorageProvider.class)
  public StorageProvider inMemoryStorageProvider() {
    log.info("Configuring in-memory bookkeeping storage");
    return new com.yanimetaxas.bookkeeping.memory.InMemoryStorageProvider();
  }
}
