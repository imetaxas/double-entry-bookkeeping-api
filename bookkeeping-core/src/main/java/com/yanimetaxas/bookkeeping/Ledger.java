package com.yanimetaxas.bookkeeping;

import com.yanimetaxas.bookkeeping.exception.LedgerAccountException;
import com.yanimetaxas.bookkeeping.memory.InMemoryStorageProvider;
import com.yanimetaxas.bookkeeping.model.Account;
import com.yanimetaxas.bookkeeping.model.Money;
import com.yanimetaxas.bookkeeping.model.Transaction;
import com.yanimetaxas.bookkeeping.model.TransactionLeg;
import com.yanimetaxas.bookkeeping.model.TransferRequest;
import com.yanimetaxas.bookkeeping.model.TransferRequest.ReferenceStep;
import com.yanimetaxas.bookkeeping.model.TrialBalance;
import com.yanimetaxas.bookkeeping.report.LedgerReport;
import com.yanimetaxas.bookkeeping.service.AccountService;
import com.yanimetaxas.bookkeeping.service.TransferService;
import java.time.Clock;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

/**
 * A general ledger — the central record of all financial transactions.
 *
 * <h2>Quick Start (in-memory, zero dependencies)</h2>
 *
 * <pre>{@code
 * ChartOfAccounts chart = new ChartOfAccounts.ChartOfAccountsBuilder()
 *     .create("Cash", "1000.00", "EUR", AccountSide.DEBIT)
 *     .create("Revenue", "0.00", "EUR", AccountSide.CREDIT)
 *     .build();
 *
 * Ledger ledger = new LedgerBuilder(chart).build().init();
 *
 * ledger.commit(ledger.createTransferRequest()
 *     .reference("T1").type("sale")
 *     .account("Cash").debit("50.00", "EUR")
 *     .account("Revenue").credit("50.00", "EUR")
 *     .build());
 *
 * ledger.getAccountBalance("Cash");    // 950.00 EUR
 * ledger.getAccountBalance("Revenue"); // 50.00 EUR
 * }</pre>
 *
 * <h2>With JDBC Persistence</h2>
 *
 * <pre>{@code
 * // Add bookkeeping-jdbc to your POM, then:
 * Ledger ledger = new LedgerBuilder(chart)
 *     .options(ConnectionOptions.EMBEDDED_H2_CONNECTION)
 *     .build().init();
 * }</pre>
 *
 * <h2>Trial Balance</h2>
 *
 * <pre>{@code
 * TrialBalance tb = ledger.computeTrialBalance();
 * tb.isBalanced(); // true if total debits == total credits
 * }</pre>
 *
 * <h2>Thread Safety</h2>
 *
 * <p>All model objects returned by this ledger are immutable and thread-safe. The ledger itself is
 * thread-safe when backed by {@code InMemoryStorageProvider} (uses {@code ReentrantReadWriteLock})
 * or {@code JdbcStorageProvider} (uses {@code TRANSACTION_SERIALIZABLE} isolation).
 *
 * @author yanimetaxas
 * @since 02-Feb-18
 * @see ChartOfAccounts
 * @see StorageProvider
 * @see StorageFactory
 */
@Slf4j
public class Ledger {

  private final String name;
  private final ChartOfAccounts chartOfAccounts;
  private final StorageProvider storage;
  private final Clock clock;
  private AccountService accounts;
  private TransferService transfers;

  private Ledger(LedgerBuilder builder) {
    this.chartOfAccounts = builder.chartOfAccounts;
    this.name = builder.name;
    this.storage = builder.storage;
    this.clock = builder.clock;
  }

  /**
   * Drops all data. This is a destructive operation — all existing data is lost. Designed for
   * fluent chaining: {@code ledger.dropAll().init()}.
   *
   * @return this ledger instance for method chaining
   */
  public Ledger dropAll() {
    storage.dropAll();
    return this;
  }

  /**
   * Binds storage to this ledger's name and creates any missing opening accounts. Accounts and
   * transactions already stored under that name are left unchanged.
   *
   * @return this ledger instance for method chaining
   */
  public Ledger init() {
    StorageSession session = storage.open(name, clock);
    accounts = session.accounts();
    transfers = session.transfers();
    chartOfAccounts.get().forEach(account -> {
      if (account.getBalance().isNullMoney()) {
        return;
      }
      if (!accounts.accountExists(account.getAccountRef())) {
        accounts.createAccount(
            account.getAccountRef(), account.getBalance(), account.getNormalBalance());
      }
    });
    return this;
  }

  private AccountService accounts() {
    if (accounts == null) {
      throw new IllegalStateException("Call init() before using ledger '" + name + "'");
    }
    return accounts;
  }

  private TransferService transfers() {
    if (transfers == null) {
      throw new IllegalStateException("Call init() before using ledger '" + name + "'");
    }
    return transfers;
  }

  /**
   * Commits a balanced transfer to the ledger.
   *
   * @param transferRequest the transfer to commit
   * @return this ledger instance for method chaining
   */
  public Ledger commit(TransferRequest transferRequest) {
    validateAccountRefs(
        transferRequest.getLegs().stream()
            .map(TransactionLeg::getAccountRef)
            .toArray(String[]::new));
    transfers().transferFunds(transferRequest);
    return this;
  }

  private void validateAccountRefs(String... accountRefs) {
    Set<String> validRefs =
        this.chartOfAccounts.get().stream()
            .map(Account::getAccountRef)
            .collect(Collectors.toSet());

    for (String ref : accountRefs) {
      if (!validRefs.contains(ref)) {
        throw new LedgerAccountException(ref, validRefs);
      }
    }
  }

  public ReferenceStep createTransferRequest() {
    return TransferRequest.builder();
  }

  public List<Transaction> findTransactions(String accountRef) {
    validateAccountRefs(accountRef);
    return transfers().findTransactionsByAccountRef(accountRef);
  }

  public Transaction getTransactionByRef(String transactionRef) {
    return transfers().getTransactionByRef(transactionRef);
  }

  public Money getAccountBalance(String accountRef) {
    validateAccountRefs(accountRef);
    return accounts().getAccountBalance(accountRef);
  }

  /**
   * Returns all accounts in the ledger with their current balances.
   *
   * @return an unmodifiable list of accounts ordered by account reference
   */
  public List<Account> listAccounts() {
    return Collections.unmodifiableList(accounts().listAccounts());
  }

  /**
   * Computes a trial balance from the openings stored with the accounts and each committed
   * transaction once. A chart passed to a later {@link #init()} does not replace those openings.
   */
  public TrialBalance computeTrialBalance() {
    return TrialBalance.compute(accounts().listAccounts(), transfers().findAllTransactions(), clock);
  }

  public void printHistoryLog() {
    log.info("\n{}", this);
  }

  public void printTrialBalance() {
    log.info("\n{}", computeTrialBalance());
  }

  @Override
  public String toString() {
    return LedgerReport.format(name, accounts().listAccounts(), transfers().findAllTransactions());
  }

  // --------------- builder ---------------

  /**
   * Fluent builder for creating {@link Ledger} instances.
   *
   * <p><b>Minimal in-memory ledger:</b>
   *
   * <pre>{@code
   * Ledger ledger = new LedgerBuilder(chart).build().init();
   * }</pre>
   *
   * <p><b>Named ledger with JDBC persistence:</b>
   *
   * <pre>{@code
   * Ledger ledger = new LedgerBuilder(chart)
   *     .name("Sales Ledger")
   *     .options(ConnectionOptions.EMBEDDED_H2_CONNECTION)
   *     .build().init();
   * }</pre>
   *
   * <p><b>Reset and reinitialize:</b>
   *
   * <pre>{@code
   * ledger.dropAll().init();
   * }</pre>
   */
  public static class LedgerBuilder {

    private String name = "General Ledger";
    private final ChartOfAccounts chartOfAccounts;
    private StorageProvider storage;
    private Clock clock = Clock.systemUTC();

    /**
     * Creates a builder for a ledger backed by the given chart of accounts.
     *
     * @param chartOfAccounts the accounts this ledger will manage (must not be null)
     * @throws NullPointerException if chartOfAccounts is null
     */
    public LedgerBuilder(ChartOfAccounts chartOfAccounts) {
      this.chartOfAccounts =
          Objects.requireNonNull(
              chartOfAccounts,
              "ChartOfAccounts must not be null."
                  + " Build one with: new ChartOfAccounts.ChartOfAccountsBuilder()"
                  + ".create(\"Cash\", \"0.00\", \"USD\").build()");
    }

    /**
     * Sets a human-readable name for this ledger (shown in logs and history output).
     *
     * @param name the ledger name
     * @return this builder
     */
    public LedgerBuilder name(String name) {
      this.name = Objects.requireNonNull(name, "Ledger name must not be null.");
      return this;
    }

    /**
     * Configures JDBC persistence via a {@link StorageFactory}. This mirrors the v0.1.0 API:
     *
     * <pre>{@code .options(ConnectionOptions.EMBEDDED_H2_CONNECTION)}</pre>
     *
     * <p>If neither {@code options()} nor {@code storage()} is called, the ledger defaults to a
     * pure in-memory provider with zero external dependencies.
     *
     * @param factory the storage factory (typically a {@code ConnectionOptions} constant)
     * @return this builder
     */
    public LedgerBuilder options(StorageFactory factory) {
      Objects.requireNonNull(factory,
          "StorageFactory must not be null. Use ConnectionOptions.EMBEDDED_H2_CONNECTION"
              + " or create a custom ConnectionOptions.");
      this.storage = factory.createStorage();
      return this;
    }

    /**
     * Sets a custom {@link StorageProvider} directly — for advanced use cases such as a custom
     * storage backend or wrapping an externally managed {@code DataSource}.
     *
     * <p>Most users should use {@link #options(StorageFactory)} instead.
     *
     * @param storage the storage provider
     * @return this builder
     */
    public LedgerBuilder storage(StorageProvider storage) {
      this.storage = Objects.requireNonNull(storage, "StorageProvider must not be null.");
      return this;
    }

    /**
     * Sets the clock used for transaction timestamps and the trial-balance time.
     *
     * @param clock the clock (must not be null)
     * @return this builder
     */
    public LedgerBuilder clock(Clock clock) {
      this.clock = Objects.requireNonNull(clock, "Clock must not be null.");
      return this;
    }

    /**
     * Builds the ledger. Call {@link Ledger#init()} on the result to create the schema and seed
     * initial balances.
     *
     * @return a new {@link Ledger} instance (not yet initialized)
     */
    public Ledger build() {
      if (storage == null) {
        storage = new InMemoryStorageProvider();
      }
      return new Ledger(this);
    }
  }
}
