package com.yanimetaxas.bookkeeping.memory;

import com.yanimetaxas.bookkeeping.StorageProvider;
import com.yanimetaxas.bookkeeping.StorageSession;
import com.yanimetaxas.bookkeeping.exception.AccountNotFoundException;
import com.yanimetaxas.bookkeeping.exception.InfrastructureException;
import com.yanimetaxas.bookkeeping.exception.InsufficientFundsException;
import com.yanimetaxas.bookkeeping.model.Account;
import com.yanimetaxas.bookkeeping.model.AccountSide;
import com.yanimetaxas.bookkeeping.model.Money;
import com.yanimetaxas.bookkeeping.model.Transaction;
import com.yanimetaxas.bookkeeping.model.TransactionLeg;
import com.yanimetaxas.bookkeeping.model.TransferRequest;
import com.yanimetaxas.bookkeeping.service.AccountService;
import com.yanimetaxas.bookkeeping.service.TransferService;
import com.yanimetaxas.bookkeeping.validation.TransferValidator;
import com.yanimetaxas.bookkeeping.validation.TransferValidatorImpl;
import java.time.Clock;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * In-memory {@link StorageProvider}. Each ledger name has its own accounts and transactions.
 * Transfers take the write lock; reads take the read lock, so a transfer is not visible halfway
 * through.
 *
 * @author yanimetaxas
 * @since 1.0.0
 */
public class InMemoryStorageProvider implements StorageProvider {

  private final Map<String, InMemoryLedgerData> ledgers = new LinkedHashMap<>();
  private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
  private final AtomicReference<StorageSession> current = new AtomicReference<>();
  private volatile Clock clock = Clock.systemUTC();

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
    lock.writeLock().lock();
    try {
      InMemoryLedgerData data = ledgers.computeIfAbsent(ref, InMemoryLedgerData::new);
      InMemoryAccountDao accounts = new InMemoryAccountDao(data);
      InMemoryTransactionDao transactions = new InMemoryTransactionDao(data);
      TransferValidatorImpl validator = new TransferValidatorImpl();
      validator.setAccountDao(accounts);
      validator.setTransactionDao(transactions);
      StorageSession session =
          new StorageSession(
              new MemAccountService(data, lock),
              new MemTransferService(accounts, transactions, validator, lock, active));
      current.set(session);
      return session;
    } finally {
      lock.writeLock().unlock();
    }
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
    lock.writeLock().lock();
    try {
      ledgers.values().forEach(InMemoryLedgerData::clear);
    } finally {
      lock.writeLock().unlock();
    }
  }

  private StorageSession requireCurrent() {
    StorageSession session = current.get();
    if (session == null) {
      throw new IllegalStateException("Call initialize() before using in-memory storage");
    }
    return session;
  }

  private static final class MemAccountService implements AccountService {

    private final InMemoryAccountDao accounts;
    private final ReentrantReadWriteLock lock;

    MemAccountService(InMemoryLedgerData data, ReentrantReadWriteLock lock) {
      this.accounts = new InMemoryAccountDao(data);
      this.lock = lock;
    }

    @Override
    public void createAccount(String accountRef, Money amount) {
      createAccount(accountRef, amount, null);
    }

    @Override
    public void createAccount(String accountRef, Money amount, AccountSide normalBalance) {
      lock.writeLock().lock();
      try {
        if (accounts.accountExists(accountRef)) {
          throw new InfrastructureException("Account already exists: " + accountRef);
        }
        accounts.createAccount(accounts.getClientRef(), accountRef, amount, normalBalance);
      } finally {
        lock.writeLock().unlock();
      }
    }

    @Override
    public boolean accountExists(String accountRef) {
      lock.readLock().lock();
      try {
        return accounts.accountExists(accountRef);
      } finally {
        lock.readLock().unlock();
      }
    }

    @Override
    public Money getAccountBalance(String accountRef) throws AccountNotFoundException {
      lock.readLock().lock();
      try {
        Account account = accounts.getAccount(accountRef);
        if (account.isNullAccount()) {
          throw new AccountNotFoundException(accountRef);
        }
        return account.getBalance();
      } finally {
        lock.readLock().unlock();
      }
    }

    @Override
    public List<Account> listAccounts() {
      lock.readLock().lock();
      try {
        return accounts.getAccounts();
      } finally {
        lock.readLock().unlock();
      }
    }
  }

  private static final class MemTransferService implements TransferService {

    private final InMemoryAccountDao accounts;
    private final InMemoryTransactionDao transactions;
    private final TransferValidator validator;
    private final ReentrantReadWriteLock lock;
    private final Clock clock;

    MemTransferService(
        InMemoryAccountDao accounts,
        InMemoryTransactionDao transactions,
        TransferValidator validator,
        ReentrantReadWriteLock lock,
        Clock clock) {
      this.accounts = accounts;
      this.transactions = transactions;
      this.validator = validator;
      this.lock = lock;
      this.clock = clock;
    }

    @Override
    public void transferFunds(TransferRequest transferRequest)
        throws InsufficientFundsException, AccountNotFoundException {
      lock.writeLock().lock();
      try {
        validator.validateTransferRequest(transferRequest);
        validator.isTransactionBalanced(transferRequest.getLegs());
        validator.transferRequestExists(transferRequest.getTransactionRef());
        validator.currenciesMatch(transferRequest.getLegs());
        validator.validBalance(transferRequest.getLegs());

        Map<String, Account> snapshot = accounts.copyAccounts();
        try {
          for (TransactionLeg leg : transferRequest.getLegs()) {
            accounts.updateBalance(leg);
          }
          transactions.storeTransaction(
              new Transaction(
                  transferRequest.getTransactionRef(),
                  transferRequest.getTransactionType(),
                  clock.instant(),
                  transferRequest.getLegs()));
        } catch (RuntimeException exception) {
          accounts.replaceAccounts(snapshot);
          throw exception;
        }
      } finally {
        lock.writeLock().unlock();
      }
    }

    @Override
    public List<Transaction> findTransactionsByAccountRef(String accountRef)
        throws AccountNotFoundException {
      lock.readLock().lock();
      try {
        if (!accounts.accountExists(accountRef)) {
          throw new AccountNotFoundException(accountRef);
        }
        Set<String> transactionRefs = transactions.getTransactionRefsForAccount(accountRef);
        if (transactionRefs.isEmpty()) {
          return new ArrayList<>();
        }
        return transactions.getTransactions(new ArrayList<>(transactionRefs));
      } finally {
        lock.readLock().unlock();
      }
    }

    @Override
    public List<Transaction> findAllTransactions() {
      lock.readLock().lock();
      try {
        return transactions.getAllTransactions();
      } finally {
        lock.readLock().unlock();
      }
    }

    @Override
    public Transaction getTransactionByRef(String transactionRef) {
      lock.readLock().lock();
      try {
        return transactions.getTransactionByRef(transactionRef);
      } finally {
        lock.readLock().unlock();
      }
    }
  }
}
