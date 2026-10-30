package com.yanimetaxas.bookkeeping;

import static io.github.imetaxas.realitycheck.RealityAssertions.assertThat;
import static io.github.imetaxas.realitycheck.RealityAssertions.assertThatThrownBy;

import com.yanimetaxas.bookkeeping.ChartOfAccounts.ChartOfAccountsBuilder;
import com.yanimetaxas.bookkeeping.Ledger.LedgerBuilder;
import com.yanimetaxas.bookkeeping.exception.AccountNotFoundException;
import com.yanimetaxas.bookkeeping.exception.InfrastructureException;
import com.yanimetaxas.bookkeeping.exception.InsufficientFundsException;
import com.yanimetaxas.bookkeeping.exception.LedgerAccountException;
import com.yanimetaxas.bookkeeping.memory.InMemoryStorageProvider;
import com.yanimetaxas.bookkeeping.model.Account;
import com.yanimetaxas.bookkeeping.model.AccountSide;
import com.yanimetaxas.bookkeeping.model.Money;
import com.yanimetaxas.bookkeeping.model.Transaction;
import com.yanimetaxas.bookkeeping.model.TransactionLeg;
import com.yanimetaxas.bookkeeping.model.TransferRequest;
import com.yanimetaxas.bookkeeping.model.TrialBalanceEntry;
import com.yanimetaxas.bookkeeping.report.LedgerReport;
import com.yanimetaxas.bookkeeping.service.AccountService;
import com.yanimetaxas.bookkeeping.service.TransferService;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.TimeZone;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class InMemoryLedgerTest {

  @Test
  void multiLeggedTransfer() {
    ChartOfAccounts chart =
        new ChartOfAccountsBuilder()
            .create("CASH", "1000.00", "EUR")
            .create("REV1", "0.00", "EUR")
            .create("REV2", "0.00", "EUR")
            .build();

    Ledger ledger = new LedgerBuilder(chart).build().init();

    ledger.commit(
        ledger
            .createTransferRequest()
            .reference("T1")
            .type("split")
            .account("CASH")
            .amount(Money.toMoney("-100.00", "EUR"))
            .account("REV1")
            .amount(Money.toMoney("60.00", "EUR"))
            .account("REV2")
            .amount(Money.toMoney("40.00", "EUR"))
            .build());

    assertThat(ledger.getAccountBalance("CASH")).isEqualTo(Money.toMoney("900.00", "EUR"));
    assertThat(ledger.getAccountBalance("REV1")).isEqualTo(Money.toMoney("60.00", "EUR"));
    assertThat(ledger.getAccountBalance("REV2")).isEqualTo(Money.toMoney("40.00", "EUR"));
  }

  @Test
  void multiCurrencyTransfer() {
    ChartOfAccounts chart =
        new ChartOfAccountsBuilder()
            .create("CASH_EUR", "1000.00", "EUR")
            .create("REV_EUR", "0.00", "EUR")
            .create("CASH_SEK", "5000.00", "SEK")
            .create("REV_SEK", "0.00", "SEK")
            .build();

    Ledger ledger = new LedgerBuilder(chart).build().init();

    ledger.commit(
        ledger
            .createTransferRequest()
            .reference("MC1")
            .type("multi")
            .account("CASH_EUR")
            .amount(Money.toMoney("-100.00", "EUR"))
            .account("REV_EUR")
            .amount(Money.toMoney("100.00", "EUR"))
            .account("CASH_SEK")
            .amount(Money.toMoney("-500.00", "SEK"))
            .account("REV_SEK")
            .amount(Money.toMoney("500.00", "SEK"))
            .build());

    assertThat(ledger.getAccountBalance("CASH_EUR")).isEqualTo(Money.toMoney("900.00", "EUR"));
    assertThat(ledger.getAccountBalance("CASH_SEK")).isEqualTo(Money.toMoney("4500.00", "SEK"));
  }

  @Test
  void listAccountsReturnsAllWithBalances() {
    ChartOfAccounts chart =
        new ChartOfAccountsBuilder()
            .create("CASH", "1000.00", "EUR")
            .create("REVENUE", "0.00", "EUR")
            .build();

    Ledger ledger = new LedgerBuilder(chart).build().init();

    List<Account> accounts = ledger.listAccounts();
    assertThat(accounts).hasSize(2);
    assertThat(accounts.stream().map(account -> account.getAccountRef() + " " + account.getBalance()).toList())
        .containsExactly("CASH 1000.00 EUR", "REVENUE 0.00 EUR");
  }

  @Test
  void findTransactionsReturnsCommittedTransactions() {
    ChartOfAccounts chart =
        new ChartOfAccountsBuilder()
            .create("CASH", "1000.00", "EUR")
            .create("REVENUE", "0.00", "EUR")
            .build();

    Ledger ledger = new LedgerBuilder(chart).build().init();

    ledger.commit(
        ledger
            .createTransferRequest()
            .reference("T1")
            .type("sale")
            .account("CASH")
            .debit("50.00", "EUR")
            .account("REVENUE")
            .credit("50.00", "EUR")
            .build());

    List<Transaction> txns = ledger.findTransactions("CASH");
    assertThat(txns).hasSize(1);
    assertThat(txns.get(0).getTransactionRef()).isEqualTo("T1");
    assertThat(ledger.getTransactionByRef("T1")).isNotNull();
    assertThat(ledger.toString()).contains("Ledger: General Ledger").contains("T1").contains("sale").contains("CASH");
    ledger.printHistoryLog();
    ledger.printTrialBalance();
  }

  @Test
  void dropAllClearsAllDataAndAllowsReinit() {
    ChartOfAccounts chart =
        new ChartOfAccountsBuilder()
            .create("CASH", "500.00", "EUR")
            .create("REVENUE", "0.00", "EUR")
            .build();

    Ledger ledger = new LedgerBuilder(chart).build().init();

    ledger.commit(
        ledger
            .createTransferRequest()
            .reference("T1")
            .type("sale")
            .account("CASH")
            .debit("100.00", "EUR")
            .account("REVENUE")
            .credit("100.00", "EUR")
            .build());

    assertThat(ledger.getAccountBalance("CASH")).isEqualTo(Money.toMoney("400.00", "EUR"));

    ledger.dropAll().init();

    assertThat(ledger.getAccountBalance("CASH")).isEqualTo(Money.toMoney("500.00", "EUR"));
    assertThat(ledger.findTransactions("CASH")).isEmpty();
  }

  @Test
  void commitToUnknownAccountThrowsException() {
    ChartOfAccounts chart =
        new ChartOfAccountsBuilder()
            .create("CASH", "1000.00", "EUR")
            .create("REVENUE", "0.00", "EUR")
            .build();

    Ledger ledger = new LedgerBuilder(chart).build().init();

    TransferRequest request =
        ledger
            .createTransferRequest()
            .reference("T1")
            .type("sale")
            .account("UNKNOWN")
            .debit("50.00", "EUR")
            .account("REVENUE")
            .credit("50.00", "EUR")
            .build();

    assertThatThrownBy(() -> ledger.commit(request)).isInstanceOf(LedgerAccountException.class);
  }

  @Test
  void initPreservesCommittedBalance() {
    ChartOfAccounts chart =
        new ChartOfAccountsBuilder()
            .create("CASH", "1000.00", "EUR")
            .create("REVENUE", "0.00", "EUR")
            .build();

    Ledger ledger = new LedgerBuilder(chart).build().init();
    ledger.commit(
        ledger
            .createTransferRequest()
            .reference("T1")
            .type("payment")
            .account("CASH")
            .debit("50.00", "EUR")
            .account("REVENUE")
            .credit("50.00", "EUR")
            .build());

    ledger.init();

    assertThat(ledger.getAccountBalance("CASH")).isEqualTo(Money.toMoney("950.00", "EUR"));
    assertThat(ledger.getTransactionByRef("T1")).isNotNull();
  }

  @Test
  void debitDecreasesStoredBalanceRegardlessOfNormalSide() {
    ChartOfAccounts chart =
        new ChartOfAccountsBuilder()
            .create("CASH", "100.00", "EUR", AccountSide.DEBIT)
            .create("REVENUE", "0.00", "EUR", AccountSide.CREDIT)
            .build();

    Ledger ledger = new LedgerBuilder(chart).build().init();
    ledger.commit(
        ledger
            .createTransferRequest()
            .reference("PAY")
            .type("payment")
            .account("CASH")
            .debit("25.00", "EUR")
            .account("REVENUE")
            .credit("25.00", "EUR")
            .build());

    assertThat(ledger.getAccountBalance("CASH")).isEqualTo(Money.toMoney("75.00", "EUR"));
    assertThat(ledger.getAccountBalance("REVENUE")).isEqualTo(Money.toMoney("25.00", "EUR"));
  }

  @Test
  void concurrentOverdraftsCannotDriveBalanceNegative() throws Exception {
    ChartOfAccounts chart =
        new ChartOfAccountsBuilder()
            .create("CASH", "50.00", "EUR")
            .create("REVENUE", "0.00", "EUR")
            .build();
    Ledger ledger = new LedgerBuilder(chart).build().init();
    ExecutorService pool = Executors.newFixedThreadPool(2);
    CountDownLatch start = new CountDownLatch(1);
    AtomicInteger ids = new AtomicInteger();

    try {
      Future<Boolean> first = pool.submit(() -> attemptDebit(ledger, start, ids));
      Future<Boolean> second = pool.submit(() -> attemptDebit(ledger, start, ids));
      start.countDown();

      assertThat(List.of(first.get(), second.get())).containsExactlyInAnyOrder(true, false);
    } finally {
      pool.shutdownNow();
    }

    assertThat(ledger.getAccountBalance("CASH")).isEqualTo(Money.toMoney("10.00", "EUR"));
    assertThat(ledger.getAccountBalance("REVENUE")).isEqualTo(Money.toMoney("40.00", "EUR"));
  }

  private static boolean attemptDebit(Ledger ledger, CountDownLatch start, AtomicInteger ids)
      throws InterruptedException {
    start.await();
    try {
      ledger.commit(
          ledger
              .createTransferRequest()
              .reference("C" + ids.incrementAndGet())
              .type("payment")
              .account("CASH")
              .debit("40.00", "EUR")
              .account("REVENUE")
              .credit("40.00", "EUR")
              .build());
      return true;
    } catch (InsufficientFundsException exception) {
      return false;
    }
  }

  @Test
  void accountSideIsPreservedThroughChartOfAccounts() {
    ChartOfAccounts chart =
        new ChartOfAccountsBuilder()
            .create("CASH", "1000.00", "EUR", AccountSide.DEBIT)
            .create("REVENUE", "0.00", "EUR", AccountSide.CREDIT)
            .build();

    Ledger ledger = new LedgerBuilder(chart).build().init();

    assertThat(
            ledger.listAccounts().stream()
                .map(account -> account.getAccountRef() + " " + account.getNormalBalance())
                .toList())
        .containsExactly("CASH DEBIT", "REVENUE CREDIT");
  }

  @Test
  void accountSideIsNullWhenNotSpecified() {
    ChartOfAccounts chart =
        new ChartOfAccountsBuilder().create("CASH", "1000.00", "EUR").build();

    assertThat(chart.get()).allMatch(account -> account.getNormalBalance() == null);
  }

  private static ChartOfAccounts cashAndRevenue(String cashAmount) {
    return new ChartOfAccountsBuilder()
        .create("CASH", cashAmount, "EUR")
        .create("REVENUE", "0.00", "EUR")
        .build();
  }

  private static TransferRequest payment(Ledger ledger, String reference, String amount) {
    return ledger
        .createTransferRequest()
        .reference(reference)
        .type("sale")
        .account("CASH")
        .debit(amount, "EUR")
        .account("REVENUE")
        .credit(amount, "EUR")
        .build();
  }

  @Test
  void historyOfAnIdleAccountShowsNoTransaction() {
    Ledger ledger = new LedgerBuilder(cashAndRevenue("0.00")).name("Idle").build().init();

    assertThat(ledger.toString())
        .contains("| CASH    | -           | -    | -    |")
        .contains("| REVENUE | -           | -    | -    |");
  }

  @Test
  void ledgersThatShareAProviderKeepSeparateBalances() {
    InMemoryStorageProvider provider = new InMemoryStorageProvider();
    Ledger first =
        new LedgerBuilder(cashAndRevenue("1000.00")).name("one").storage(provider).build().init();
    first.commit(payment(first, "T1", "50.00"));

    Ledger second =
        new LedgerBuilder(cashAndRevenue("1000.00")).name("two").storage(provider).build().init();

    assertThat(first.getAccountBalance("CASH")).isEqualTo(Money.toMoney("950.00", "EUR"));
    assertThat(second.getAccountBalance("CASH")).isEqualTo(Money.toMoney("1000.00", "EUR"));
    assertThat(second.getTransactionByRef("T1")).isNull();
  }

  @Test
  void includedAccountIsNotSeededAndCannotBePosted() {
    ChartOfAccounts chart =
        new ChartOfAccountsBuilder()
            .create("CASH", "10.00", "EUR")
            .includeExisted("OLD")
            .build();
    InMemoryStorageProvider provider = new InMemoryStorageProvider();
    Ledger ledger = new LedgerBuilder(chart).storage(provider).build().init();

    assertThat(ledger.listAccounts().stream().map(Account::getAccountRef).toList())
        .containsExactly("CASH");
    assertThat(ledger.toString()).contains("CASH").doesNotContain("OLD");
    assertThatThrownBy(() -> ledger.findTransactions("OLD"))
        .isInstanceOf(AccountNotFoundException.class);
  }

  @Test
  void aStoredAccountAbsentFromTheChartIsReported() {
    ChartOfAccounts chart =
        new ChartOfAccountsBuilder()
            .create("CASH", "10.00", "EUR")
            .includeExisted("OLD")
            .build();
    InMemoryStorageProvider provider = new InMemoryStorageProvider();
    Ledger ledger = new LedgerBuilder(chart).storage(provider).build().init();

    provider
        .accountService()
        .createAccount("EXTRA", Money.toMoney("2.00", "EUR"), AccountSide.CREDIT);

    assertThat(ledger.toString()).contains("EXTRA").contains("2.00");
    assertThat(
            ledger.computeTrialBalance().getEntries().stream()
                .map(TrialBalanceEntry::getAccountRef)
                .toList())
        .contains("EXTRA");
  }

  @Test
  void aNullChartIsRejected() {
    assertThatThrownBy(() -> new LedgerBuilder(null)).isInstanceOf(NullPointerException.class);
  }

  @Test
  void aNullLedgerNameIsRejected() {
    ChartOfAccounts chart = new ChartOfAccountsBuilder().create("CASH", "1.00", "EUR").build();

    assertThatThrownBy(() -> new LedgerBuilder(chart).name(null))
        .isInstanceOf(NullPointerException.class);
  }

  @Test
  void aNullStorageProviderIsRejected() {
    ChartOfAccounts chart = new ChartOfAccountsBuilder().create("CASH", "1.00", "EUR").build();

    assertThatThrownBy(() -> new LedgerBuilder(chart).storage(null))
        .isInstanceOf(NullPointerException.class);
  }

  @Test
  void nullConnectionOptionsAreRejected() {
    ChartOfAccounts chart = new ChartOfAccountsBuilder().create("CASH", "1.00", "EUR").build();

    assertThatThrownBy(() -> new LedgerBuilder(chart).options(null))
        .isInstanceOf(NullPointerException.class);
  }

  @Test
  void aNullClockIsRejected() {
    ChartOfAccounts chart = new ChartOfAccountsBuilder().create("CASH", "1.00", "EUR").build();

    assertThatThrownBy(() -> new LedgerBuilder(chart).clock(null))
        .isInstanceOf(NullPointerException.class);
  }

  @Test
  void ledgerStampsTransactionsWithTheSuppliedClock() {
    Instant posted = Instant.parse("2024-05-06T07:08:09Z");
    Ledger ledger =
        new LedgerBuilder(cashAndRevenue("10.00"))
            .name("clock")
            .clock(Clock.fixed(posted, ZoneOffset.UTC))
            .build()
            .init();
    ledger.commit(payment(ledger, "T1", "1.00"));

    assertThat(ledger.getTransactionByRef("T1").getTransactionDate()).isEqualTo(posted);
    assertThat(ledger.computeTrialBalance().getTimestamp()).isEqualTo(posted);
  }

  @Test
  void historyRendersTheSuppliedInstantInUtc() {
    TimeZone previous = TimeZone.getDefault();
    TimeZone.setDefault(TimeZone.getTimeZone("America/New_York"));
    try {
      Instant posted = Instant.parse("2024-05-06T07:08:09Z");
      Ledger ledger =
          new LedgerBuilder(cashAndRevenue("10.00"))
              .name("clock")
              .clock(Clock.fixed(posted, ZoneOffset.UTC))
              .build()
              .init();
      ledger.commit(payment(ledger, "T1", "1.00"));

      assertThat(ledger.toString())
          .contains("2024-05-06 07:08")
          .contains("-1.00")
          .doesNotContain("03:08");
    } finally {
      TimeZone.setDefault(previous);
    }
  }

  @Test
  void anAbandonedClockDoesNotStickToTheThreadThatSetIt() throws Exception {
    InMemoryStorageProvider provider = new InMemoryStorageProvider();
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
    InMemoryStorageProvider provider = new InMemoryStorageProvider();
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

  private static Instant stamp(
      InMemoryStorageProvider provider, String name, Instant posted, CyclicBarrier bothReady)
      throws Exception {
    bothReady.await();
    Ledger ledger =
        new LedgerBuilder(cashAndRevenue("10.00"))
            .name(name)
            .storage(provider)
            .clock(Clock.fixed(posted, ZoneOffset.UTC))
            .build()
            .init();
    ledger.commit(payment(ledger, "T1", "1.00"));
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

  @Test
  void deprecatedConnectionOptionsDelegatesToTheWrappedFactory() {
    InMemoryStorageProvider provider = new InMemoryStorageProvider();
    ConnectionOptions options = new ConnectionOptions(() -> provider);
    assertThat(options.createStorage()).isSameAs(provider);

    Ledger ledger =
        new LedgerBuilder(cashAndRevenue("5.00")).options(options).build().init();

    assertThat(ledger.getAccountBalance("CASH")).isEqualTo(Money.toMoney("5.00", "EUR"));
  }

  @Test
  void usingALedgerBeforeInitIsRejected() {
    Ledger ledger = new LedgerBuilder(cashAndRevenue("10.00")).build();

    assertThatThrownBy(() -> ledger.getAccountBalance("CASH"))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("init()");
    assertThatThrownBy(() -> ledger.findTransactions("CASH"))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("init()");
  }

  @Test
  void accountServiceBeforeInitializeIsRejected() {
    InMemoryStorageProvider provider = new InMemoryStorageProvider();

    assertThatThrownBy(provider::accountService).isInstanceOf(IllegalStateException.class);
  }

  @Test
  void theDefaultClockStampsATransfer() {
    InMemoryStorageProvider provider = new InMemoryStorageProvider();
    provider.initialize("ledger");
    provider.accountService().createAccount("CASH", Money.toMoney("5.00", "EUR"), AccountSide.DEBIT);
    provider.accountService().createAccount("REVENUE", Money.toMoney("0.00", "EUR"), null);
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

    assertThat(provider.transferService().getTransactionByRef("T1").getTransactionDate())
        .isNotNull();
  }

  @Test
  void historyReadsTheTransactionListOnce() {
    CountingStorage storage = new CountingStorage();
    Ledger ledger =
        new LedgerBuilder(cashAndRevenue("10.00")).name("history").storage(storage).build().init();
    ledger.commit(payment(ledger, "T1", "1.00"));

    assertThat(ledger.toString()).contains("T1").contains("CASH").contains("REVENUE");
    assertThat(storage.historyReads).isEqualTo(1);
    assertThat(storage.accountHistoryReads).isZero();

    Transaction repeated =
        new Transaction(
            "T2",
            "move",
            Instant.EPOCH,
            List.of(
                new TransactionLeg("CASH", Money.toMoney("-1.00", "EUR")),
                new TransactionLeg("CASH", Money.toMoney("1.00", "EUR"))));
    assertThat(
            LedgerReport.format("history", cashAndRevenue("10.00").get(), List.of(repeated)))
        .contains("T2");
  }

  @Test
  void reopenedLedgerUsesTheStoredOpening() {
    InMemoryStorageProvider provider = new InMemoryStorageProvider();
    ChartOfAccounts original =
        new ChartOfAccountsBuilder()
            .create("CASH", "1000.00", "EUR", AccountSide.DEBIT)
            .create("REVENUE", "0.00", "EUR", AccountSide.CREDIT)
            .build();
    Ledger first = new LedgerBuilder(original).name("books").storage(provider).build().init();
    first.commit(payment(first, "T1", "50.00"));

    ChartOfAccounts replacement =
        new ChartOfAccountsBuilder()
            .create("CASH", "1.00", "EUR", AccountSide.DEBIT)
            .create("REVENUE", "1.00", "EUR", AccountSide.CREDIT)
            .build();
    Ledger again = new LedgerBuilder(replacement).name("books").storage(provider).build().init();

    assertThat(again.getAccountBalance("CASH")).isEqualTo(Money.toMoney("950.00", "EUR"));
    assertThat(
            again.computeTrialBalance().getEntries().stream()
                .map(
                    entry ->
                        entry.getAccountRef()
                            + " "
                            + entry.getDebitTotal()
                            + " "
                            + entry.getCreditTotal())
                .toList())
        .containsExactly("CASH 1050.00 0", "REVENUE 0 50.00");
  }

  @Test
  void concurrentInitializationKeepsEachLedgerOnItsOwnAccounts() throws Exception {
    InMemoryStorageProvider provider = new InMemoryStorageProvider();
    ExecutorService pool = Executors.newFixedThreadPool(2);
    CyclicBarrier bothInitialized = new CyclicBarrier(2);
    try {
      Future<List<String>> alpha =
          pool.submit(() -> openAccount(provider, "alpha", "CASH", "10.00", bothInitialized));
      Future<List<String>> beta =
          pool.submit(() -> openAccount(provider, "beta", "BANK", "20.00", bothInitialized));

      assertThat(alpha.get()).containsExactly("CASH");
      assertThat(beta.get()).containsExactly("BANK");
    } finally {
      pool.shutdownNow();
    }
  }

  private static List<String> openAccount(
      InMemoryStorageProvider provider,
      String name,
      String account,
      String amount,
      CyclicBarrier bothInitialized)
      throws Exception {
    StorageSession session = provider.open(name);
    bothInitialized.await();
    session.accounts().createAccount(account, Money.toMoney(amount, "EUR"), AccountSide.DEBIT);
    return session.accounts().listAccounts().stream().map(Account::getAccountRef).toList();
  }

  @Test
  void anotherThreadSeesTheMostRecentlyOpenedClient() throws Exception {
    InMemoryStorageProvider provider = new InMemoryStorageProvider();
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
    InMemoryStorageProvider provider = new InMemoryStorageProvider();
    StorageSession alpha = provider.open("alpha");
    alpha.accounts().createAccount("CASH", Money.toMoney("4.00", "EUR"), AccountSide.DEBIT);
    StorageSession beta = provider.open("beta");
    beta.accounts().createAccount("BANK", Money.toMoney("6.00", "EUR"), AccountSide.DEBIT);

    assertThat(alpha.accounts().listAccounts().stream().map(Account::getAccountRef).toList())
        .containsExactly("CASH");
  }

  @Test
  void creatingTheSameAccountTwiceIsRejected() {
    InMemoryStorageProvider provider = new InMemoryStorageProvider();
    provider.initialize("ledger");
    provider.accountService().createAccount("CASH", Money.toMoney("1.00", "EUR"), AccountSide.DEBIT);

    assertThatThrownBy(
            () -> provider.accountService().createAccount("CASH", Money.toMoney("1.00", "EUR")))
        .isInstanceOf(InfrastructureException.class)
        .hasMessageContaining("already exists");
    assertThatThrownBy(
            () ->
                provider
                    .accountService()
                    .createAccount("CASH", Money.toMoney("1.00", "EUR"), AccountSide.DEBIT))
        .isInstanceOf(InfrastructureException.class);
  }

  @Test
  void aNullClientNameIsRejected() {
    InMemoryStorageProvider provider = new InMemoryStorageProvider();

    assertThatThrownBy(() -> provider.initialize(null)).isInstanceOf(NullPointerException.class);
  }

  @Test
  void aBlankClientNameIsRejected() {
    InMemoryStorageProvider provider = new InMemoryStorageProvider();

    assertThatThrownBy(() -> provider.initialize(" ")).isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void aMissingAccountIsRejected() {
    InMemoryStorageProvider provider = new InMemoryStorageProvider();
    provider.initialize("ledger");

    assertThatThrownBy(() -> provider.accountService().getAccountBalance("MISSING"))
        .isInstanceOf(AccountNotFoundException.class);
  }

  private static final class CountingStorage implements StorageProvider {

    private final InMemoryStorageProvider delegate = new InMemoryStorageProvider();
    private int historyReads;
    private int accountHistoryReads;

    @Override
    public AccountService accountService() {
      return delegate.accountService();
    }

    @Override
    public StorageSession open(String clientRef) {
      return counted(delegate.open(clientRef));
    }

    @Override
    public StorageSession open(String clientRef, Clock transactionClock) {
      return counted(delegate.open(clientRef, transactionClock));
    }

    private StorageSession counted(StorageSession session) {
      return new StorageSession(session.accounts(), counted(session.transfers()));
    }

    private TransferService counted(TransferService inner) {
      return new TransferService() {
        @Override
        public void transferFunds(TransferRequest transferRequest) {
          inner.transferFunds(transferRequest);
        }

        @Override
        public List<Transaction> findTransactionsByAccountRef(String accountRef) {
          accountHistoryReads++;
          return inner.findTransactionsByAccountRef(accountRef);
        }

        @Override
        public List<Transaction> findAllTransactions() {
          historyReads++;
          return inner.findAllTransactions();
        }

        @Override
        public Transaction getTransactionByRef(String transactionRef) {
          return inner.getTransactionByRef(transactionRef);
        }
      };
    }

    @Override
    public TransferService transferService() {
      return delegate.transferService();
    }

    @Override
    public void setClock(Clock clock) {
      delegate.setClock(clock);
    }

    @Override
    public void dropAll() {
      delegate.dropAll();
    }
  }
}
