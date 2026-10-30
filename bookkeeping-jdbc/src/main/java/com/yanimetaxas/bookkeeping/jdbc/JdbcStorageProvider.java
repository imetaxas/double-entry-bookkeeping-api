package com.yanimetaxas.bookkeeping.jdbc;

import com.yanimetaxas.bookkeeping.StorageProvider;
import com.yanimetaxas.bookkeeping.StorageSession;
import com.yanimetaxas.bookkeeping.service.AccountService;
import com.yanimetaxas.bookkeeping.service.TransferService;
import com.yanimetaxas.bookkeeping.validation.TransferValidatorImpl;
import java.time.Clock;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import javax.sql.DataSource;
import lombok.extern.slf4j.Slf4j;

/**
 * JDBC {@link StorageProvider}. {@link #open(String)} returns services for that ledger name.
 * Services already returned stay bound to the name they were created for. {@link #accountService()}
 * returns the most recently opened session on every thread.
 *
 * @author yanimetaxas
 * @since 1.0.0
 */
@Slf4j
public class JdbcStorageProvider implements StorageProvider {

  private final DataSource dataSource;
  private final JdbcSchema schema;
  private final AtomicReference<StorageSession> current = new AtomicReference<>();
  private volatile Clock clock = Clock.systemUTC();

  public JdbcStorageProvider(ConnectionOptions options) {
    this(
        new SimpleDataSource(
            options.driverClassName(), options.url(), options.username(), options.password()),
        options.schema());
  }

  /**
   * Creates a provider with an externally managed {@link DataSource}.
   *
   * @param dataSource the JDBC DataSource to use
   * @param schemaScript the schema script filename, for example {@code h2-schema.sql}
   */
  public JdbcStorageProvider(DataSource dataSource, String schemaScript) {
    this.dataSource = SqliteForeignKeys.enable(dataSource);
    this.schema = new JdbcSchema(this.dataSource, schemaScript);
  }

  @Override
  public StorageSession open(String clientRef) {
    return open(clientRef, clock);
  }

  @Override
  public StorageSession open(String clientRef, Clock transactionClock) {
    String ref = Objects.requireNonNull(clientRef, "clientRef must not be null");
    if (ref.isBlank()) {
      throw new IllegalArgumentException("clientRef must not be blank");
    }
    Clock active = Objects.requireNonNull(transactionClock, "clock");
    schema.ensureSchema();
    schema.insertClientIfAbsent(ref, active.instant());
    JdbcAccountRepository accounts = new JdbcAccountRepository(dataSource, ref);
    JdbcTransactionRepository transactions = new JdbcTransactionRepository(dataSource, ref, active);
    TransferValidatorImpl validator = new TransferValidatorImpl();
    validator.setAccountDao(accounts);
    validator.setTransactionDao(transactions);
    StorageSession session =
        new StorageSession(
            new JdbcAccountService(accounts),
            new JdbcTransferService(dataSource, accounts, transactions, validator));
    current.set(session);
    log.debug("Initialized client: {}", ref);
    return session;
  }

  @Override
  public AccountService accountService() {
    return requireCurrent().accounts();
  }

  @Override
  public TransferService transferService() {
    return requireCurrent().transfers();
  }

  @Override
  public void setClock(Clock clock) {
    this.clock = Objects.requireNonNull(clock, "clock");
  }

  @Override
  public void dropAll() {
    schema.dropAll();
  }

  private StorageSession requireCurrent() {
    StorageSession session = current.get();
    if (session == null) {
      throw new IllegalStateException("Call initialize() before using JDBC storage");
    }
    return session;
  }

}
