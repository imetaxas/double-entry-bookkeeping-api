package com.yanimetaxas.bookkeeping.jdbc;

import static io.github.imetaxas.realitycheck.RealityAssertions.assertThat;
import static io.github.imetaxas.realitycheck.RealityAssertions.assertThatThrownBy;

import com.yanimetaxas.bookkeeping.ChartOfAccounts;
import com.yanimetaxas.bookkeeping.ChartOfAccounts.ChartOfAccountsBuilder;
import com.yanimetaxas.bookkeeping.Ledger;
import com.yanimetaxas.bookkeeping.StorageSession;
import com.yanimetaxas.bookkeeping.Ledger.LedgerBuilder;
import com.yanimetaxas.bookkeeping.exception.AccountNotFoundException;
import com.yanimetaxas.bookkeeping.exception.InfrastructureException;
import com.yanimetaxas.bookkeeping.exception.TransferRequestExistsException;
import com.yanimetaxas.bookkeeping.model.Account;
import com.yanimetaxas.bookkeeping.model.AccountSide;
import com.yanimetaxas.bookkeeping.model.Money;
import com.yanimetaxas.bookkeeping.model.TransferRequest;
import com.yanimetaxas.bookkeeping.validation.TransferValidatorImpl;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.sql.SQLFeatureNotSupportedException;
import java.util.List;
import java.util.logging.Logger;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class JdbcStorageDetailsTest {

  @Test
  void embeddedDerbyUsesTheDerbySchema() {
    assertThat(ConnectionOptions.EMBEDDED_DERBY_CONNECTION.schema()).isEqualTo("derby-schema.sql");
  }

  @Test
  void embeddedHsqlUsesTheDefaultCredentials() {
    assertThat(ConnectionOptions.EMBEDDED_HSQL_CONNECTION.url()).contains("hsqldb");
    assertThat(ConnectionOptions.EMBEDDED_HSQL_CONNECTION.username()).isEqualTo("sa");
    assertThat(ConnectionOptions.EMBEDDED_HSQL_CONNECTION.password()).isEmpty();
  }

  @Test
  void embeddedH2UsesTheH2Driver() {
    assertThat(ConnectionOptions.EMBEDDED_H2_CONNECTION.driverClassName()).isEqualTo("org.h2.Driver");
  }

  @Test
  void customConnectionCreatesJdbcStorage() {
    ConnectionOptions options = h2("jdbc:h2:mem:custom-options;DB_CLOSE_DELAY=-1");

    assertThat(options.createStorage()).isInstanceOf(JdbcStorageProvider.class);
    assertThat(options.driverClassName()).isEqualTo("org.h2.Driver");
  }

  @Test
  void initializationRejectsABlankClient() {
    JdbcStorageProvider provider = new JdbcStorageProvider(h2("jdbc:h2:mem:blank-client;DB_CLOSE_DELAY=-1"));

    assertThatThrownBy(() -> provider.initialize(null)).isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> provider.initialize(" ")).isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void aMissingSchemaScriptIsReported() {
    assertThatThrownBy(
            () ->
                new JdbcStorageProvider(
                        new ConnectionOptions("org.h2.Driver")
                            .url("jdbc:h2:mem:missing-schema;DB_CLOSE_DELAY=-1")
                            .username("sa")
                            .password("")
                            .schema("no-such-schema.sql"))
                    .initialize("client"))
        .isInstanceOf(InfrastructureException.class)
        .hasMessageContaining("Resource not found");
  }

  @Test
  void openingTheSameClientTwiceKeepsTheAccount() {
    String url = "jdbc:h2:mem:reopen-client;DB_CLOSE_DELAY=-1";
    JdbcStorageProvider provider = new JdbcStorageProvider(h2(url));
    provider.dropAll();
    provider.initialize("client");
    provider.accountService().createAccount("CASH", Money.toMoney("8.00", "EUR"), AccountSide.DEBIT);

    provider.initialize("client");

    assertThat(provider.accountService().getAccountBalance("CASH"))
        .isEqualTo(Money.toMoney("8.00", "EUR"));
  }

  @Test
  void creatingAnExistingAccountIsRejected() {
    String url = "jdbc:h2:mem:duplicate-account;DB_CLOSE_DELAY=-1";
    JdbcStorageProvider provider = new JdbcStorageProvider(h2(url));
    provider.dropAll();
    provider.initialize("client");
    provider.accountService().createAccount("CASH", Money.toMoney("8.00", "EUR"), AccountSide.DEBIT);

    assertThatThrownBy(
            () -> provider.accountService().createAccount("CASH", Money.toMoney("8.00", "EUR")))
        .isInstanceOf(InfrastructureException.class)
        .hasMessageContaining("already exists");
  }

  @Test
  void postingKeepsTheOpeningAmount() {
    String url = "jdbc:h2:mem:opening;DB_CLOSE_DELAY=-1";
    ConnectionOptions options = h2(url);
    new JdbcStorageProvider(options).dropAll();
    ChartOfAccounts chart =
        new ChartOfAccountsBuilder()
            .create("CASH", "10.00", "EUR", AccountSide.DEBIT)
            .create("REVENUE", "0.00", "EUR", AccountSide.CREDIT)
            .build();
    Ledger ledger = new LedgerBuilder(chart).name("client").options(options).build().init();
    ledger.commit(
        ledger
            .createTransferRequest()
            .reference("T1")
            .type("sale")
            .account("CASH")
            .debit("3.00", "EUR")
            .account("REVENUE")
            .credit("3.00", "EUR")
            .build());

    Account cash =
        ledger.listAccounts().stream()
            .filter(account -> account.getAccountRef().equals("CASH"))
            .findFirst()
            .orElseThrow();
    assertThat(cash.getBalance()).isEqualTo(Money.toMoney("7.00", "EUR"));
    assertThat(cash.getOpening()).isEqualTo(Money.toMoney("10.00", "EUR"));
  }

  @Test
  void aLegWithoutItsTransactionIsRejected() throws Exception {
    String url = "jdbc:h2:mem:orphan-leg;DB_CLOSE_DELAY=-1";
    SimpleDataSource dataSource = new SimpleDataSource("org.h2.Driver", url, "sa", "");
    JdbcStorageProvider provider = new JdbcStorageProvider(h2(url));
    provider.dropAll();
    provider.initialize("client");
    provider
        .accountService()
        .createAccount("CASH", Money.toMoney("10.00", "EUR"), AccountSide.DEBIT);

    try (Connection connection = dataSource.getConnection();
        Statement statement = connection.createStatement()) {
      assertThatThrownBy(
              () ->
                  statement.execute(
                      "INSERT INTO transaction_leg"
                          + " (client_ref, transaction_ref, account_ref, amount, currency)"
                          + " VALUES ('client', 'MISSING', 'CASH', '1.00', 'EUR')"))
          .isInstanceOf(SQLException.class);
    }
  }

  @Test
  void concurrentInitializationKeepsEachClientOnItsOwnAccounts() throws Exception {
    String url = "jdbc:h2:mem:concurrent-init;DB_CLOSE_DELAY=-1";
    JdbcStorageProvider provider = new JdbcStorageProvider(h2(url));
    provider.dropAll();
    provider.initialize("warmup");
    ExecutorService pool = Executors.newFixedThreadPool(2);
    CyclicBarrier bothInitialized = new CyclicBarrier(2);
    try {
      Future<List<String>> alpha =
          pool.submit(() -> openClient(provider, "alpha", "CASH", bothInitialized));
      Future<List<String>> beta =
          pool.submit(() -> openClient(provider, "beta", "BANK", bothInitialized));
      assertThat(alpha.get()).containsExactly("CASH");
      assertThat(beta.get()).containsExactly("BANK");
    } finally {
      pool.shutdownNow();
    }
  }

  private static List<String> openClient(
      JdbcStorageProvider provider, String name, String account, CyclicBarrier bothInitialized)
      throws Exception {
    StorageSession session = provider.open(name);
    bothInitialized.await();
    session.accounts().createAccount(account, Money.toMoney("10.00", "EUR"), AccountSide.DEBIT);
    return session.accounts().listAccounts().stream().map(Account::getAccountRef).toList();
  }

  @Test
  void anotherThreadSeesTheMostRecentlyOpenedClient() throws Exception {
    String url = "jdbc:h2:mem:cross-thread;DB_CLOSE_DELAY=-1";
    JdbcStorageProvider provider = new JdbcStorageProvider(h2(url));
    provider.dropAll();
    StorageSession alpha = provider.open("alpha");
    alpha.accounts().createAccount("CASH", Money.toMoney("4.00", "EUR"), AccountSide.DEBIT);
    StorageSession beta = provider.open("beta");
    beta.accounts().createAccount("BANK", Money.toMoney("6.00", "EUR"), AccountSide.DEBIT);

    ExecutorService pool = Executors.newSingleThreadExecutor();
    try {
      Future<List<String>> seen =
          pool.submit(
              () ->
                  provider.accountService().listAccounts().stream()
                      .map(Account::getAccountRef)
                      .toList());
      assertThat(seen.get()).containsExactly("BANK");
    } finally {
      pool.shutdownNow();
    }
  }

  @Test
  void theSessionReturnedByOpenStaysOnThatClient() {
    String url = "jdbc:h2:mem:session-client;DB_CLOSE_DELAY=-1";
    JdbcStorageProvider provider = new JdbcStorageProvider(h2(url));
    provider.dropAll();
    StorageSession alpha = provider.open("alpha");
    alpha.accounts().createAccount("CASH", Money.toMoney("4.00", "EUR"), AccountSide.DEBIT);
    StorageSession beta = provider.open("beta");
    beta.accounts().createAccount("BANK", Money.toMoney("6.00", "EUR"), AccountSide.DEBIT);

    assertThat(alpha.accounts().listAccounts().stream().map(Account::getAccountRef).toList())
        .containsExactly("CASH");
  }

  @Test
  void transactionDaoRejectsDirectWritesAndEmptyLookups() {
    String url = "jdbc:h2:mem:tx-dao;DB_CLOSE_DELAY=-1";
    new JdbcStorageProvider(h2(url)).initialize("client");
    JdbcTransactionRepository transactions =
        new JdbcTransactionRepository(
            new SimpleDataSource("org.h2.Driver", url, "sa", ""), "client");

    assertThat(transactions.getClientRef()).isEqualTo("client");
    assertThat(transactions.getTransactions(java.util.List.of())).isEmpty();
    assertThat(transactions.getTransactionByRef("ABSENT")).isNull();
    assertThat(transactions.getTransactionRefsForAccount("CASH")).isEmpty();
    assertThatThrownBy(() -> transactions.storeTransaction(null))
        .isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  void repeatedInsertOfTheSameReferenceIsRejected() throws Exception {
    String url = "jdbc:h2:mem:duplicate-insert;DB_CLOSE_DELAY=-1";
    SimpleDataSource dataSource = new SimpleDataSource("org.h2.Driver", url, "sa", "");
    new JdbcStorageProvider(h2(url)).initialize("client");
    JdbcTransactionRepository transactions = new JdbcTransactionRepository(dataSource, "client");
    TransferRequest request =
        TransferRequest.builder()
            .reference("T1")
            .type("sale")
            .account("CASH")
            .debit("1.00", "EUR")
            .account("REVENUE")
            .credit("1.00", "EUR")
            .build();

    JdbcAccountRepository accounts = new JdbcAccountRepository(dataSource, "client");
    accounts.createAccount("client", "CASH", Money.toMoney("10.00", "EUR"), null);
    accounts.createAccount("client", "REVENUE", Money.toMoney("0.00", "EUR"), null);
    try (Connection connection = dataSource.getConnection()) {
      transactions.insert(connection, request);
      assertThatThrownBy(() -> transactions.insert(connection, request))
          .isInstanceOf(TransferRequestExistsException.class);
    }
  }

  @Test
  void updatingAMissingAccountReportsThatItIsAbsent() throws Exception {
    String url = "jdbc:h2:mem:missing-update;DB_CLOSE_DELAY=-1";
    SimpleDataSource dataSource = new SimpleDataSource("org.h2.Driver", url, "sa", "");
    new JdbcStorageProvider(h2(url)).initialize("client");
    JdbcAccountRepository accounts = new JdbcAccountRepository(dataSource, "client");

    try (Connection connection = dataSource.getConnection()) {
      assertThatThrownBy(() -> accounts.updateAmount(connection, "MISSING", java.math.BigDecimal.ONE))
          .isInstanceOf(AccountNotFoundException.class);
    }
  }

  @Test
  void readingLegsFailsWhenTheSecondConnectionIsDown() {
    String url = "jdbc:h2:mem:legs-down;DB_CLOSE_DELAY=-1";
    JdbcStorageProvider provider = new JdbcStorageProvider(h2(url));
    provider.dropAll();
    provider.initialize("client");
    provider.accountService().createAccount("CASH", Money.toMoney("10.00", "EUR"));
    provider.accountService().createAccount("REVENUE", Money.toMoney("0.00", "EUR"));
    provider
        .transferService()
        .transferFunds(
            TransferRequest.builder()
                .reference("T1")
                .type("sale")
                .account("CASH")
                .debit("1.00", "EUR")
                .account("REVENUE")
                .credit("1.00", "EUR")
                .build());

    JdbcTransactionRepository transactions =
        new JdbcTransactionRepository(
            new FailAfter(new SimpleDataSource("org.h2.Driver", url, "sa", ""), 1), "client");

    assertThatThrownBy(() -> transactions.getTransactions(List.of("T1")))
        .isInstanceOf(InfrastructureException.class);
  }

  @Test
  void jdbcStorageBeforeInitIsRejected() {
    JdbcStorageProvider provider =
        new JdbcStorageProvider(h2("jdbc:h2:mem:before-init;DB_CLOSE_DELAY=-1"));

    assertThatThrownBy(provider::accountService).isInstanceOf(IllegalStateException.class);
    assertThatThrownBy(provider::transferService).isInstanceOf(IllegalStateException.class);
  }

  @Test
  void anEmptyStatementInASchemaScriptIsSkipped() {
    JdbcSchema schema =
        new JdbcSchema(
            new SimpleDataSource("org.h2.Driver", "jdbc:h2:mem:blank-sql;DB_CLOSE_DELAY=-1", "sa", ""),
            "blank-command.sql");

    schema.ensureSchema();
  }

  @Test
  void aFailedInsertThatIsNotADuplicateIsRethrown() throws Exception {
    String url = "jdbc:h2:mem:bad-insert;DB_CLOSE_DELAY=-1";
    SimpleDataSource dataSource = new SimpleDataSource("org.h2.Driver", url, "sa", "");
    new JdbcStorageProvider(h2(url)).initialize("client");
    JdbcTransactionRepository transactions = new JdbcTransactionRepository(dataSource, "client");
    TransferRequest request =
        TransferRequest.builder()
            .reference("T1")
            .type("sale")
            .account("CASH")
            .debit("1.00", "EUR")
            .account("REVENUE")
            .credit("1.00", "EUR")
            .build();

    try (Connection connection = dataSource.getConnection()) {
      connection.close();
      assertThatThrownBy(() -> transactions.insert(connection, request))
          .isInstanceOf(SQLException.class)
          .matches(error -> !(error instanceof TransferRequestExistsException));
    }
  }

  @Test
  void aDownDatabaseCannotCheckWhetherTheClientExists() {
    JdbcSchema schema = new JdbcSchema(failingDataSource(), "h2-schema.sql");

    assertThatThrownBy(() -> schema.insertClientIfAbsent("client", Instant.EPOCH))
        .isInstanceOf(InfrastructureException.class);
  }

  @Test
  void simpleDataSourceOpensConnectionsAndRejectsAnUnknownDriver() throws Exception {
    SimpleDataSource dataSource =
        new SimpleDataSource("org.h2.Driver", "jdbc:h2:mem:simple-ds;DB_CLOSE_DELAY=-1", "sa", "");

    try (Connection connection = dataSource.getConnection()) {
      assertThat(connection.isClosed()).isFalse();
    }
    try (Connection connection = dataSource.getConnection("sa", "")) {
      assertThat(connection.getMetaData()).isNotNull();
    }
    assertThat(dataSource.getLogWriter()).isNull();
    dataSource.setLogWriter(null);
    dataSource.setLoginTimeout(5);
    assertThat(dataSource.getLoginTimeout()).isZero();
    assertThat(dataSource.isWrapperFor(Connection.class)).isFalse();
    assertThatThrownBy(() -> dataSource.unwrap(Connection.class)).isInstanceOf(SQLException.class);
    assertThatThrownBy(dataSource::getParentLogger).isInstanceOf(SQLFeatureNotSupportedException.class);
    assertThatThrownBy(() -> new SimpleDataSource("com.missing.Driver", "jdbc:h2:mem:x", "sa", ""))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("JDBC driver not found");
  }

  @Test
  void constraintViolationsAreRecognizedBySqlStateAndSqliteCode() {
    assertThat(SqlExceptionTranslator.isConstraintViolation(new SQLException("dup", "23505", 0)))
        .isTrue();
    assertThat(SqlExceptionTranslator.isConstraintViolation(new SQLException("dup", "23000", 0)))
        .isTrue();
    assertThat(SqlExceptionTranslator.isConstraintViolation(new SQLException("dup", "HY000", 19)))
        .isTrue();
    assertThat(SqlExceptionTranslator.isConstraintViolation(new SQLException("other", "42000", 0)))
        .isFalse();
  }

  @Test
  void includedAccountIsNotCreatedAndCannotBeRead() {
    ChartOfAccounts chart =
        new ChartOfAccountsBuilder()
            .create("CASH", "10.00", "EUR")
            .includeExisted("OLD")
            .build();
    Ledger ledger =
        new LedgerBuilder(chart)
            .name("included")
            .options(h2("jdbc:h2:mem:included;DB_CLOSE_DELAY=-1"))
            .build()
            .init();

    assertThatThrownBy(() -> ledger.findTransactions("OLD")).isInstanceOf(AccountNotFoundException.class);
  }

  @Test
  void aDownDatabaseRejectsProviderUse() {
    DataSource down = failingDataSource();
    JdbcStorageProvider provider = new JdbcStorageProvider(down, "h2-schema.sql");
    TransferRequest request =
        TransferRequest.builder()
            .reference("T1")
            .type("sale")
            .account("CASH")
            .debit("1.00", "EUR")
            .account("REVENUE")
            .credit("1.00", "EUR")
            .build();

    assertThatThrownBy(() -> provider.initialize("client")).isInstanceOf(InfrastructureException.class);
    assertThatThrownBy(() -> transfers(down).transferFunds(request))
        .isInstanceOf(InfrastructureException.class);
  }

  @Test
  void aDownDatabaseRejectsAccountRepositoryUse() {
    JdbcAccountRepository accounts = new JdbcAccountRepository(failingDataSource(), "client");

    assertThatThrownBy(() -> accounts.accountExists("CASH")).isInstanceOf(InfrastructureException.class);
    assertThatThrownBy(
            () -> accounts.createAccount("client", "CASH", Money.toMoney("1.00", "EUR"), null))
        .isInstanceOf(InfrastructureException.class);
    assertThatThrownBy(() -> accounts.getAccount("CASH")).isInstanceOf(InfrastructureException.class);
    assertThatThrownBy(accounts::getAccounts).isInstanceOf(InfrastructureException.class);
  }

  @Test
  void aDownDatabaseRejectsTransactionRepositoryUse() {
    JdbcTransactionRepository transactions =
        new JdbcTransactionRepository(failingDataSource(), "client");

    assertThatThrownBy(() -> transactions.getTransactionRefsForAccount("CASH"))
        .isInstanceOf(InfrastructureException.class);
    assertThatThrownBy(() -> transactions.getTransactions(List.of("T1")))
        .isInstanceOf(InfrastructureException.class);
  }

  @Test
  void transferContinuesWhenSerializableIsolationIsRejected() {
    String url = "jdbc:h2:mem:isolation;DB_CLOSE_DELAY=-1";
    SimpleDataSource real = new SimpleDataSource("org.h2.Driver", url, "sa", "");
    JdbcStorageProvider provider = new JdbcStorageProvider(h2(url));
    provider.initialize("client");
    provider.accountService().createAccount("CASH", Money.toMoney("10.00", "EUR"));
    provider.accountService().createAccount("REVENUE", Money.toMoney("0.00", "EUR"));
    TransferRequest request =
        TransferRequest.builder()
            .reference("T1")
            .type("sale")
            .account("CASH")
            .debit("1.00", "EUR")
            .account("REVENUE")
            .credit("1.00", "EUR")
            .build();

    transfers(new IsolationRejectingDataSource(real)).transferFunds(request);

    assertThat(provider.accountService().getAccountBalance("CASH"))
        .isEqualTo(Money.toMoney("9.00", "EUR"));
  }

  @Test
  void aFailedRollbackIsReportedWithTheOriginalError() {
    TransferRequest request =
        TransferRequest.builder()
            .reference("T1")
            .type("sale")
            .account("CASH")
            .debit("1.00", "EUR")
            .account("REVENUE")
            .credit("1.00", "EUR")
            .build();

    assertThatThrownBy(() -> transfers(new RollbackFailingDataSource()).transferFunds(request))
        .isInstanceOf(InfrastructureException.class)
        .cause()
        .isInstanceOf(SQLException.class)
        .hasMessage("statement failed")
        .hasSuppressedCount(1)
        .suppressedException(0)
        .hasMessage("rollback failed");
  }

  @Test
  void anUnreadableSchemaScriptIsReported() {
    InputStream unreadable =
        new InputStream() {
          @Override
          public int read() throws IOException {
            throw new IOException("unreadable");
          }
        };

    assertThatThrownBy(() -> JdbcSchema.readScript(unreadable))
        .isInstanceOf(InfrastructureException.class)
        .cause()
        .isInstanceOf(IOException.class)
        .hasMessage("unreadable");
  }

  @Test
  void connectionOptionsReturnACopyInsteadOfChangingTheEmbeddedDefault() {
    String originalUrl = ConnectionOptions.EMBEDDED_H2_CONNECTION.url();

    ConnectionOptions changed = ConnectionOptions.EMBEDDED_H2_CONNECTION.url("jdbc:h2:mem:changed");

    assertThat(changed.url()).isEqualTo("jdbc:h2:mem:changed");
    assertThat(ConnectionOptions.EMBEDDED_H2_CONNECTION.url()).isEqualTo(originalUrl);
  }

  private static JdbcTransferService transfers(DataSource dataSource) {
    JdbcAccountRepository accounts = new JdbcAccountRepository(dataSource, "client");
    JdbcTransactionRepository transactions = new JdbcTransactionRepository(dataSource, "client");
    TransferValidatorImpl validator = new TransferValidatorImpl();
    validator.setAccountDao(accounts);
    validator.setTransactionDao(transactions);
    return new JdbcTransferService(dataSource, accounts, transactions, validator);
  }

  private static DataSource failingDataSource() {
    return new FailingDataSource();
  }

  /** Data source whose connections are unavailable. */
  private static class FailingDataSource implements DataSource {

    @Override
    public Connection getConnection() throws SQLException {
      throw new SQLException("down");
    }

    @Override
    public Connection getConnection(String username, String password) throws SQLException {
      return getConnection();
    }

    @Override
    public PrintWriter getLogWriter() {
      return null;
    }

    @Override
    public void setLogWriter(PrintWriter out) {}

    @Override
    public void setLoginTimeout(int seconds) {}

    @Override
    public int getLoginTimeout() {
      return 0;
    }

    @Override
    public Logger getParentLogger() {
      throw new UnsupportedOperationException();
    }

    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
      throw new SQLException("not a wrapper");
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) {
      return false;
    }
  }

  /** Opens a real connection whose serializable isolation setting is rejected. */
  private static final class IsolationRejectingDataSource extends FailingDataSource {

    private final DataSource delegate;

    IsolationRejectingDataSource(DataSource delegate) {
      this.delegate = delegate;
    }

    @Override
    public Connection getConnection() throws SQLException {
      Connection connection = delegate.getConnection();
      return (Connection)
          Proxy.newProxyInstance(
              Connection.class.getClassLoader(),
              new Class<?>[] {Connection.class},
              (proxy, method, args) -> {
                if (method.getDeclaringClass() == Object.class) {
                  return objectMethod(proxy, method.getName(), args);
                }
                if ("setTransactionIsolation".equals(method.getName())) {
                  throw new SQLException("isolation");
                }
                try {
                  return method.invoke(connection, args);
                } catch (InvocationTargetException e) {
                  if (e.getCause() instanceof RuntimeException runtime) {
                    throw runtime;
                  }
                  if (e.getCause() instanceof SQLException sql) {
                    throw sql;
                  }
                  throw new IllegalStateException(e.getCause());
                }
              });
    }
  }

  /** Connection that fails the first statement and then fails the rollback. */
  private static final class RollbackFailingDataSource extends FailingDataSource {

    @Override
    public Connection getConnection() {
      return (Connection)
          Proxy.newProxyInstance(
              Connection.class.getClassLoader(),
              new Class<?>[] {Connection.class},
              (proxy, method, args) -> {
                if (method.getDeclaringClass() == Object.class) {
                  return objectMethod(proxy, method.getName(), args);
                }
                return switch (method.getName()) {
                  case "setAutoCommit", "close", "setTransactionIsolation" -> null;
                  case "prepareStatement" -> throw new SQLException("statement failed");
                  case "rollback" -> throw new SQLException("rollback failed");
                  default -> throw new UnsupportedOperationException(method.getName());
                };
              });
    }
  }

  private static Object objectMethod(Object proxy, String name, Object[] args) {
    return switch (name) {
      case "toString" -> "connection";
      case "hashCode" -> System.identityHashCode(proxy);
      case "equals" -> proxy == args[0];
      default -> null;
    };
  }

  /** Opens a fixed number of connections, then reports that the database is down. */
  private static final class FailAfter extends FailingDataSource {

    private final DataSource delegate;
    private int remaining;

    FailAfter(DataSource delegate, int successes) {
      this.delegate = delegate;
      this.remaining = successes;
    }

    @Override
    public Connection getConnection() throws SQLException {
      if (remaining == 0) {
        return super.getConnection();
      }
      remaining--;
      return delegate.getConnection();
    }
  }

  @Test
  void aFailedClientInsertIsRethrownWhenTheClientIsStillAbsent() {
    DataSource database = embeddedH2("jdbc:h2:mem:client-absent;DB_CLOSE_DELAY=-1");
    new JdbcSchema(database, "h2-schema.sql").ensureSchema();

    assertThatThrownBy(
            () ->
                new JdbcSchema(clientInsertFails(database, false), "h2-schema.sql")
                    .insertClientIfAbsent("client", Instant.EPOCH))
        .isInstanceOf(InfrastructureException.class)
        .cause()
        .hasMessage("insert failed");
  }

  @Test
  void aFailedClientInsertIsIgnoredWhenAnotherConnectionCreatedTheClient() throws Exception {
    DataSource database = embeddedH2("jdbc:h2:mem:client-present;DB_CLOSE_DELAY=-1");
    new JdbcSchema(database, "h2-schema.sql").ensureSchema();

    new JdbcSchema(clientInsertFails(database, true), "h2-schema.sql")
        .insertClientIfAbsent("client", Instant.EPOCH);

    try (Connection connection = database.getConnection();
        Statement statement = connection.createStatement();
        ResultSet rows = statement.executeQuery("SELECT COUNT(*) FROM client")) {
      assertThat(rows.next()).isTrue();
      assertThat(rows.getInt(1)).isEqualTo(1);
    }
  }

  @Test
  void aSchemaStatementThatFailsIsRethrown() {
    DataSource database = embeddedH2("jdbc:h2:mem:broken-schema;DB_CLOSE_DELAY=-1");

    assertThatThrownBy(() -> new JdbcSchema(database, "broken-schema.sql").ensureSchema())
        .isInstanceOf(InfrastructureException.class);
  }

  @Test
  void aDropStatementThatFailsIsRethrown() {
    DataSource database = embeddedH2("jdbc:h2:mem:broken-drop;DB_CLOSE_DELAY=-1");

    assertThatThrownBy(() -> new JdbcSchema(database, "broken-schema.sql").dropAll())
        .isInstanceOf(InfrastructureException.class);
  }

  @Test
  void derbyDropAllSucceedsWhenTheSchemaIsMissing() {
    JdbcStorageProvider provider = new JdbcStorageProvider(derby("empty-drop"));

    provider.dropAll();
    provider.initialize("client");

    assertThat(provider.accountService().listAccounts()).isEmpty();
  }

  @Test
  void derbyDropAllSucceedsWhenTheSchemaExists() {
    JdbcStorageProvider provider = new JdbcStorageProvider(derby("existing-drop"));
    provider.initialize("client");
    provider.accountService().createAccount("CASH", Money.toMoney("1.00", "EUR"), AccountSide.DEBIT);

    provider.dropAll();
    provider.initialize("client");

    assertThat(provider.accountService().listAccounts()).isEmpty();
  }

  @Test
  void derbyRejectsAnOrphanLeg() {
    rejectOrphanLeg(derby("fk-derby"));
  }

  @Test
  void hsqlRejectsAnOrphanLeg() {
    rejectOrphanLeg(hsql("fk-hsql"));
  }

  @Test
  void anAbandonedClockDoesNotStickToTheThreadThatSetIt() throws Exception {
    JdbcStorageProvider provider = new JdbcStorageProvider(h2("jdbc:h2:mem:abandoned-clock;DB_CLOSE_DELAY=-1"));
    provider.dropAll();
    Clock abandoned = Clock.fixed(Instant.parse("2020-01-01T00:00:00Z"), ZoneOffset.UTC);
    Clock current = Clock.fixed(Instant.parse("2024-05-06T07:08:09Z"), ZoneOffset.UTC);
    ExecutorService pool = Executors.newSingleThreadExecutor();
    try {
      pool.submit(() -> provider.setClock(abandoned)).get();
      provider.setClock(current);

      StorageSession session = pool.submit(() -> provider.open("ledger")).get();

      assertThat(postSale(session)).isEqualTo(current.instant());
    } finally {
      pool.shutdownNow();
    }
  }

  @Test
  void concurrentLedgersKeepTheClockTheyWereOpenedWith() throws Exception {
    JdbcStorageProvider provider = new JdbcStorageProvider(h2("jdbc:h2:mem:clock-race;DB_CLOSE_DELAY=-1"));
    provider.dropAll();
    Instant alphaPosted = Instant.parse("2024-01-01T00:00:00Z");
    Instant betaPosted = Instant.parse("2024-06-01T12:00:00Z");
    ExecutorService pool = Executors.newFixedThreadPool(2);
    CyclicBarrier bothReady = new CyclicBarrier(2);
    try {
      Future<Instant> alpha = pool.submit(() -> stamp(provider, "alpha", alphaPosted, bothReady));
      Future<Instant> beta = pool.submit(() -> stamp(provider, "beta", betaPosted, bothReady));

      assertThat(alpha.get()).isEqualTo(alphaPosted);
      assertThat(beta.get()).isEqualTo(betaPosted);
    } finally {
      pool.shutdownNow();
    }
  }

  @Test
  void sqliteForeignKeysApplyToAnExternalDataSource(@TempDir Path directory)
      throws Exception {
    Path file = directory.resolve("books.db");
    org.sqlite.SQLiteDataSource raw = new org.sqlite.SQLiteDataSource();
    raw.setUrl("jdbc:sqlite:" + file.toAbsolutePath());
    JdbcStorageProvider provider = new JdbcStorageProvider(raw, "sqlite-schema.sql");
    provider.initialize("client");
    provider.accountService().createAccount("CASH", Money.toMoney("1.00", "EUR"), AccountSide.DEBIT);

    assertThatThrownBy(() -> insertOrphan(SqliteForeignKeys.enable(raw), "client"))
        .isInstanceOf(SQLException.class);
    insertOrphan(raw, "client");
  }

  private static void rejectOrphanLeg(ConnectionOptions options) {
    new LedgerBuilder(new ChartOfAccountsBuilder().create("CASH", "10.00", "EUR").build())
        .options(options)
        .name("books")
        .build()
        .init();

    assertThatThrownBy(
            () ->
                insertOrphan(
                    new SimpleDataSource(
                        options.driverClassName(),
                        options.url(),
                        options.username(),
                        options.password()),
                    "books"))
        .isInstanceOf(SQLException.class);
  }

  private static Instant stamp(
      JdbcStorageProvider provider, String name, Instant posted, CyclicBarrier bothReady)
      throws Exception {
    bothReady.await();
    Ledger ledger =
        new LedgerBuilder(
                new ChartOfAccountsBuilder()
                    .create("CASH", "10.00", "EUR")
                    .create("REVENUE", "0.00", "EUR")
                    .build())
            .name(name)
            .storage(provider)
            .clock(Clock.fixed(posted, ZoneOffset.UTC))
            .build()
            .init();
    ledger.commit(
        TransferRequest.builder()
            .reference("T1")
            .type("sale")
            .account("CASH")
            .debit("1.00", "EUR")
            .account("REVENUE")
            .credit("1.00", "EUR")
            .build());
    return ledger.getTransactionByRef("T1").getTransactionDate();
  }

  private static Instant postSale(StorageSession session) {
    session.accounts().createAccount("CASH", Money.toMoney("10.00", "EUR"), AccountSide.DEBIT);
    session.accounts().createAccount("REVENUE", Money.toMoney("0.00", "EUR"), AccountSide.CREDIT);
    session
        .transfers()
        .transferFunds(
            TransferRequest.builder()
                .reference("T1")
                .type("sale")
                .account("CASH")
                .debit("1.00", "EUR")
                .account("REVENUE")
                .credit("1.00", "EUR")
                .build());
    return session.transfers().getTransactionByRef("T1").getTransactionDate();
  }

  private static ConnectionOptions derby(String name) {
    return new ConnectionOptions("org.apache.derby.jdbc.EmbeddedDriver")
        .url("jdbc:derby:memory:" + name + ";create=true")
        .username("sa")
        .password("")
        .schema("derby-schema.sql");
  }

  private static ConnectionOptions hsql(String name) {
    return new ConnectionOptions("org.hsqldb.jdbcDriver")
        .url("jdbc:hsqldb:mem:" + name)
        .username("sa")
        .password("")
        .schema("h2-schema.sql");
  }

  private static void insertOrphan(DataSource dataSource, String clientRef) throws SQLException {
    try (Connection connection = dataSource.getConnection();
        Statement statement = connection.createStatement()) {
      statement.execute(
          "INSERT INTO transaction_leg"
              + " (client_ref, transaction_ref, account_ref, amount, currency)"
              + " VALUES ('"
              + clientRef
              + "', 'MISSING', 'CASH', '1.00', 'EUR')");
    }
  }

  private static DataSource clientInsertFails(DataSource database, boolean createClientFirst) {
    return (DataSource)
        Proxy.newProxyInstance(
            DataSource.class.getClassLoader(),
            new Class<?>[] {DataSource.class},
            (proxy, method, args) -> {
              Object builtin = objectMethod(proxy, method.getName(), args);
              if (builtin != null || "toString".equals(method.getName())) {
                return builtin != null ? builtin : "datasource";
              }
              if ("getConnection".equals(method.getName()) && (args == null || args.length == 0)) {
                Connection real = database.getConnection();
                return Proxy.newProxyInstance(
                    Connection.class.getClassLoader(),
                    new Class<?>[] {Connection.class},
                    (connection, connectionMethod, connectionArgs) -> {
                      Object connectionBuiltin =
                          objectMethod(connection, connectionMethod.getName(), connectionArgs);
                      if (connectionBuiltin != null
                          || "toString".equals(connectionMethod.getName())) {
                        return connectionBuiltin != null ? connectionBuiltin : "connection";
                      }
                      if ("prepareStatement".equals(connectionMethod.getName())
                          && connectionArgs != null
                          && connectionArgs.length > 0
                          && String.valueOf(connectionArgs[0]).startsWith("INSERT INTO client")) {
                        if (createClientFirst) {
                          insertClient(database);
                        }
                        throw new SQLException("insert failed");
                      }
                      try {
                        return connectionMethod.invoke(real, connectionArgs);
                      } catch (InvocationTargetException failure) {
                        if (failure.getCause() instanceof RuntimeException runtime) {
                          throw runtime;
                        }
                        if (failure.getCause() instanceof Error error) {
                          throw error;
                        }
                        if (failure.getCause() instanceof SQLException sql) {
                          throw sql;
                        }
                        throw failure;
                      }
                    });
              }
              return method.invoke(database, args);
            });
  }

  private static void insertClient(DataSource database) throws SQLException {
    try (Connection connection = database.getConnection();
        PreparedStatement statement =
            connection.prepareStatement("INSERT INTO client (ref, creation_date) VALUES (?, ?)")) {
      statement.setString(1, "client");
      statement.setTimestamp(2, Timestamp.from(Instant.EPOCH));
      statement.executeUpdate();
    }
  }

  private static DataSource embeddedH2(String url) {
    return new SimpleDataSource("org.h2.Driver", url, "sa", "");
  }

  private static ConnectionOptions h2(String url) {
    return new ConnectionOptions("org.h2.Driver").url(url).username("sa").password("").schema("h2-schema.sql");
  }

}
