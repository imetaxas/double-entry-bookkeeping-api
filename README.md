# Double-Entry Bookkeeping API

[![CI](https://github.com/imetaxas/double-entry-bookkeeping-api/actions/workflows/ci.yml/badge.svg)](https://github.com/imetaxas/double-entry-bookkeeping-api/actions/workflows/ci.yml)
[![codecov](https://codecov.io/gh/imetaxas/double-entry-bookkeeping-api/branch/master/graph/badge.svg)](https://codecov.io/gh/imetaxas/double-entry-bookkeeping-api)
[![Maven Central](https://img.shields.io/maven-central/v/com.yanimetaxas/bookkeeping-core)](https://search.maven.org/artifact/com.yanimetaxas/bookkeeping-core)
[![License: MIT](https://img.shields.io/badge/License-MIT-green.svg)](https://opensource.org/licenses/MIT)
[![javadoc](https://javadoc.io/badge2/com.yanimetaxas/bookkeeping-core/javadoc.svg)](https://javadoc.io/doc/com.yanimetaxas/bookkeeping-core)

A lightweight, embeddable **double-entry bookkeeping engine** for the JVM — designed for library consumers, not framework lock-in.

Supports multi-currency, multi-legged transactions, debit/credit account sides, trial balance reporting, and optional JDBC persistence. Works **out of the box with zero dependencies** (in-memory mode) or with any major SQL database via the JDBC module.

## Quick Start

**5 lines** — create accounts, build a ledger, commit a transaction, check balances:

```java
Ledger ledger = new Ledger.LedgerBuilder(new ChartOfAccounts.ChartOfAccountsBuilder()
    .create("Cash", "1000.00", "EUR").create("Revenue", "0.00", "EUR").build()).build().init();
ledger.commit(ledger.createTransferRequest().reference("T1").type("sale")
    .account("Cash").debit("50.00", "EUR").account("Revenue").credit("50.00", "EUR").build());
ledger.getAccountBalance("Cash");    // → 950.00 EUR
ledger.getAccountBalance("Revenue"); // → 50.00 EUR
```

### In-Memory (zero dependencies)

Add a single Maven dependency — no Spring, no JDBC, no database:

```xml
<dependency>
  <groupId>com.yanimetaxas</groupId>
  <artifactId>bookkeeping-core</artifactId>
  <version>1.0.0</version>
</dependency>
```

```java
import com.yanimetaxas.bookkeeping.*;
import com.yanimetaxas.bookkeeping.model.*;

ChartOfAccounts chart = new ChartOfAccounts.ChartOfAccountsBuilder()
    .create("Cash",    "1000.00", "EUR", AccountSide.DEBIT)
    .create("Revenue", "0.00",    "EUR", AccountSide.CREDIT)
    .build();

Ledger ledger = new Ledger.LedgerBuilder(chart)
    .name("My Ledger")
    .build()   // defaults to in-memory
    .init();

ledger.commit(ledger.createTransferRequest()
    .reference("T1").type("sale")
    .account("Cash").debit("50.00", "EUR")
    .account("Revenue").credit("50.00", "EUR")
    .build());

ledger.getAccountBalance("Cash");    // → 950.00 EUR
ledger.getAccountBalance("Revenue"); // → 50.00 EUR
```

### With JDBC Persistence

Add both modules and the driver you need:

```xml
<dependency>
  <groupId>com.yanimetaxas</groupId>
  <artifactId>bookkeeping-jdbc</artifactId>
  <version>1.0.0</version>
</dependency>
<dependency>
  <groupId>com.h2database</groupId>
  <artifactId>h2</artifactId>
  <version>2.5.252</version>
</dependency>
```

```java
import com.yanimetaxas.bookkeeping.jdbc.ConnectionOptions;

Ledger ledger = new Ledger.LedgerBuilder(chart)
    .name("JDBC H2")
    .options(ConnectionOptions.EMBEDDED_H2_CONNECTION)
    .build()
    .init();
```

## Features

| Feature | Module |
|---|---|
| Multi-currency, multi-legged transactions | `bookkeeping-core` |
| Step-builder API for transfer requests | `bookkeeping-core` |
| Debit/credit account sides (`AccountSide`) | `bookkeeping-core` |
| Trial balance computation and reporting | `bookkeeping-core` |
| Account listing with balances | `bookkeeping-core` |
| Formatted transaction history log | `bookkeeping-core` |
| Explicit `dropAll()` for database resets | `bookkeeping-core` |
| JDBC persistence (H2, SQLite, HSQL, Derby, MySQL, PostgreSQL) | `bookkeeping-jdbc` |
| Zero-framework JDBC with explicit transactions | `bookkeeping-jdbc` |
| Spring Boot auto-configuration | `bookkeeping-spring-boot-starter` |

## Signed balances

The stored balance is a signed net amount. `.credit()` adds to it and `.debit()` subtracts from it. A rejected transfer leaves both balances and history unchanged. `InsufficientFundsException` means the posting would make a stored balance negative.

`AccountSide` records the normal-balance classification used for chart setup and trial-balance presentation. It does not reverse posting arithmetic: debiting Cash decreases the stored Cash balance whether or not Cash is marked `AccountSide.DEBIT`. The quick start's `sale` label is application data; debiting Cash there reduces it from 1000.00 to 950.00.

Accounts without an `AccountSide` follow the same rules. A negative raw amount is a debit, and a positive raw amount is a credit.

| Account Type | Normal-balance label | Examples |
|---|---|---|
| Assets | `AccountSide.DEBIT` | Cash, Accounts Receivable, Equipment |
| Expenses | `AccountSide.DEBIT` | Rent, Salaries, Utilities |
| Liabilities | `AccountSide.CREDIT` | Accounts Payable, Loans |
| Equity | `AccountSide.CREDIT` | Owner's Equity, Retained Earnings |
| Revenue | `AccountSide.CREDIT` | Sales Revenue, Service Income |

```java
ChartOfAccounts chart = new ChartOfAccounts.ChartOfAccountsBuilder()
    .create("Cash",           "10000.00", "EUR", AccountSide.DEBIT)
    .create("AccountsPayable", "0.00",    "EUR", AccountSide.CREDIT)
    .create("Revenue",         "0.00",    "EUR", AccountSide.CREDIT)
    .create("Rent",            "0.00",    "EUR", AccountSide.DEBIT)
    .build();
```

## Trial Balance

A trial balance lists every account with its total debits and credits, verifying that the books balance:

```java
TrialBalance tb = ledger.computeTrialBalance();

tb.isBalanced(); // true
tb.toString();   // formatted table:
// Trial Balance (2026-03-31T12:00:00Z)
//
// +------------------+----------+---------+
// | Account          |    Debit |  Credit |
// +------------------+----------+---------+
// | Cash             | 10000.00 |  200.00 |
// | Revenue          |     0.00 | 9800.00 |
// +------------------+----------+---------+
// | TOTAL            | 10000.00 | 10000.00|
// +------------------+----------+---------+
// Status: BALANCED

ledger.printTrialBalance(); // logs via SLF4J
```

## Transfer Request Builder

The step-builder provides compile-time guidance. Use either explicit debit/credit or raw amounts:

```java
// Explicit debit/credit (recommended)
ledger.commit(ledger.createTransferRequest()
    .reference("T1").type("sale")
    .account("Cash").debit("100.00", "EUR")
    .account("Revenue").credit("100.00", "EUR")
    .build());

// Raw amounts (legacy — negative = debit, positive = credit)
ledger.commit(ledger.createTransferRequest()
    .reference("T2").type("expense")
    .account("Cash").amount(Money.toMoney("-50.00", "EUR"))
    .account("Rent").amount(Money.toMoney("50.00", "EUR"))
    .build());

// Multi-legged
ledger.commit(ledger.createTransferRequest()
    .reference("T3").type("split-payment")
    .account("Cash").debit("150.00", "EUR")
    .account("Revenue").credit("100.00", "EUR")
    .account("Tax").credit("50.00", "EUR")
    .build());
```

## Database Reset

Use `dropAll()` to explicitly destroy all data. `init()` preserves accounts and transactions already stored for the ledger name. That name is the JDBC client identity: reopening the same name reloads its data, while a different name in the same database is a separate set of accounts. [`ReopenLedgerExample`](bookkeeping-examples/src/main/java/com/yanimetaxas/bookkeeping/examples/ReopenLedgerExample.java) keeps a posted balance when the chart uses `includeExisted`, and isolates a second ledger name. The trial balance classifies the opening stored with each account, so a later chart with different amounts does not change that report. History and the trial balance list stored accounts and print the exact stored decimal text. History timestamps are UTC. [`FixedClockExample`](bookkeeping-examples/src/main/java/com/yanimetaxas/bookkeeping/examples/FixedClockExample.java) stamps a posting with a fixed clock. An external SQLite `DataSource` has foreign keys enabled.

```java
// Drops all tables, then re-creates the schema and initial accounts
ledger.dropAll().init();
```

## Account Listing

```java
List<Account> accounts = ledger.listAccounts();
accounts.forEach(a -> log.info("{}: {}", a.getAccountRef(), a.getBalance()));
```

## Transaction History

```java
ledger.printHistoryLog();
```

Produces a neatly formatted box-drawn table:

```
Ledger: My Ledger

+----------+---------+----------+
| Account  |  Amount | Currency |
+----------+---------+----------+
| Cash     | 1000.00 | EUR      |
| Revenue  |   50.00 | EUR      |
+----------+---------+----------+

+-----------+-------------+------+------------------+
| Account   | Transaction | Type | Date             |
+-----------+-------------+------+------------------+
| Cash      | T1          | sale | 2026-03-31 12:00 |
| Revenue   | T1          | sale | 2026-03-31 12:00 |
+-----------+-------------+------+------------------+
```

## Supported Databases (JDBC Module)

| Database | Driver | `DataSourceDriver` Constant | Notes |
|---|---|---|---|
| H2 (embedded) | `org.h2.Driver` | `EMBEDDED_H2` | Default. Bundled with `bookkeeping-jdbc` |
| H2 (server) | `org.h2.Driver` | `JDBC_H2` | Provide URL, user, password |
| SQLite | `org.sqlite.JDBC` | `JDBC_SQLITE` | File-based: `jdbc:sqlite:ledger.db`. Optional dependency |
| HSQLDB | `org.hsqldb.jdbcDriver` | `EMBEDDED_HSQL` | Optional dependency |
| Apache Derby | `org.apache.derby.jdbc.EmbeddedDriver` | `EMBEDDED_DERBY` | Optional dependency |
| MySQL 8+ | `com.mysql.cj.jdbc.Driver` | `JDBC_MYSQL` | Optional dependency |
| PostgreSQL | `org.postgresql.Driver` | `JDBC_POSTGRES` | Optional dependency |

For optional databases, add the JDBC driver to your project. [`SupportedDatabasesExample`](bookkeeping-examples/src/main/java/com/yanimetaxas/bookkeeping/examples/SupportedDatabasesExample.java) posts to embedded H2, HSQLDB, and Derby. [`SqliteFileLedgerExample`](bookkeeping-examples/src/main/java/com/yanimetaxas/bookkeeping/examples/SqliteFileLedgerExample.java) writes a SQLite file and reloads it through an external `DataSource`.

```xml
<dependency>
  <groupId>org.xerial</groupId>
  <artifactId>sqlite-jdbc</artifactId>
  <version>3.53.4.0</version>
</dependency>
```

## Spring Boot Starter

For Spring Boot applications, the starter auto-wires a `Ledger` bean from `application.yml`:

```xml
<dependency>
  <groupId>com.yanimetaxas</groupId>
  <artifactId>bookkeeping-spring-boot-starter</artifactId>
  <version>1.0.0</version>
</dependency>
```

```yaml
bookkeeping:
  ledger-name: "Production Ledger"
  accounts:
    Cash:    { amount: "10000.00", currency: EUR, side: DEBIT }
    Revenue: { amount: "0.00",     currency: EUR, side: CREDIT }
  jdbc:
    driver-class-name: org.h2.Driver
    url: jdbc:h2:mem:bookkeeping;DB_CLOSE_DELAY=-1
    username: sa
    schema: h2-schema.sql
```

Then inject the ledger:

```java
@RestController
public class PaymentController {
    private final Ledger ledger;

    public PaymentController(Ledger ledger) {
        this.ledger = ledger;
    }

    @PostMapping("/payments")
    public void processPayment(@RequestBody PaymentRequest req) {
        ledger.commit(ledger.createTransferRequest()
            .reference(req.id()).type("payment")
            .account("Cash").debit(req.amount(), req.currency())
            .account("Revenue").credit(req.amount(), req.currency())
            .build());
    }
}
```

Without `bookkeeping.jdbc` properties, the starter defaults to an in-memory ledger. [`SpringBootLedgerExample`](bookkeeping-examples/src/main/java/com/yanimetaxas/bookkeeping/examples/SpringBootLedgerExample.java) starts both modes.

## Module Structure

```
bookkeeping-parent/
├── bookkeeping-core/                  Pure in-memory engine (no dependencies beyond SLF4J)
├── bookkeeping-jdbc/                  Zero-framework JDBC persistence (plain java.sql)
├── bookkeeping-spring-boot-starter/   Spring Boot auto-configuration
└── bookkeeping-examples/              Cookbook examples (not published)
```

| Module | Dependencies | Size |
|---|---|---|
| `bookkeeping-core` | SLF4J only | ~50 KB |
| `bookkeeping-jdbc` | `bookkeeping-core` + JDBC driver(s) | ~30 KB + drivers |
| `bookkeeping-spring-boot-starter` | `bookkeeping-core` + Spring Boot | Optional integration |
| `bookkeeping-examples` | All modules | Not published to Maven Central |

If you only need in-memory bookkeeping, depend on `bookkeeping-core` alone — no Spring, no JDBC, no database drivers. The JDBC module uses **plain `java.sql`** with zero framework dependencies.

## Logging

This library uses [SLF4J](https://www.slf4j.org/) for logging. Add a binding to see output:

```xml
<dependency>
  <groupId>ch.qos.logback</groupId>
  <artifactId>logback-classic</artifactId>
  <version>1.5.18</version>
</dependency>
```

## Thread Safety

All model objects (`Money`, `Account`, `Transaction`, `TransactionLeg`, `TrialBalance`, `TrialBalanceEntry`) are **immutable value objects** and are safe to share across threads without synchronization.

The `Ledger` itself is a **stateful service** — its thread safety depends on the underlying `StorageProvider`:

| Provider | Thread Safety | Mechanism |
|---|---|---|
| `InMemoryStorageProvider` | Thread-safe | A write lock covers validation and posting. A rejected transfer does not change balances. |
| `JdbcStorageProvider` | Thread-safe | Duplicate, currency, balance, and history checks run in one JDBC transaction scoped by ledger name. The provider requests `TRANSACTION_SERIALIZABLE` isolation. |

**Safe patterns:**
```java
// Multiple threads can commit concurrently — the storage provider serializes internally
executor.submit(() -> ledger.commit(transferRequest1));
executor.submit(() -> ledger.commit(transferRequest2));
```

**Unsafe patterns:**
```java
// Don't check-then-act across separate calls without external synchronization
Money balance = ledger.getAccountBalance("Cash");   // Thread A reads
// Thread B commits between these two calls
if (balance.getAmount().compareTo(threshold) > 0) { // Stale check
    ledger.commit(withdrawal);                       // May overdraw
}
```

For read-then-write patterns, rely on the built-in overdraft protection (`InsufficientFundsException`) rather than pre-checking balances. [`RejectedTransferExample`](bookkeeping-examples/src/main/java/com/yanimetaxas/bookkeeping/examples/RejectedTransferExample.java) shows an overdraft, an unbalanced entry, and a currency mismatch, each leaving balances and history unchanged.

## Multi-Currency

Each leg must use its account's currency. One transfer may include several currencies when the signed amounts in each currency sum to zero.

For **multi-currency accounting**, use separate ledgers per currency:

```java
Ledger eurLedger = new Ledger.LedgerBuilder(eurChart).name("EUR").build().init();
Ledger usdLedger = new Ledger.LedgerBuilder(usdChart).name("USD").build().init();
```

Record **FX trades** as paired journal entries linked by a common reference prefix:

```java
// Sell 1,850 EUR, Buy 2,000 USD (rate: 1.0811)
eurLedger.commit(eurLedger.createTransferRequest()
    .reference("FX-001-EUR").type("fx-sell")
    .account("EUR:Cash").debit("1850.00", "EUR")
    .account("EUR:FXOutflow").credit("1850.00", "EUR")
    .build());

usdLedger.commit(usdLedger.createTransferRequest()
    .reference("FX-001-USD").type("fx-buy")
    .account("USD:Cash").debit("2000.00", "USD")
    .account("USD:Revenue").credit("2000.00", "USD")
    .build());
```

This library intentionally does **not** include a built-in exchange rate service. Exchange rates are volatile, source-dependent (ECB, market feeds, internal rates), and application-specific. The library focuses on recording the results of conversions, not performing them. Your application should:

1. Look up the rate from your preferred source
2. Compute the converted amount
3. Record both sides of the FX trade as shown above

See [`MultiCurrencyTransferExample`](bookkeeping-examples/src/main/java/com/yanimetaxas/bookkeeping/examples/MultiCurrencyTransferExample.java) for one transfer that posts EUR and SEK together, and [`MultiCurrencyFXExample`](bookkeeping-examples/src/main/java/com/yanimetaxas/bookkeeping/examples/MultiCurrencyFXExample.java) for separate ledgers per currency.

## Immutability

All model objects are **immutable value objects** with proper `equals()`, `hashCode()`, and `toString()`:

| Class | Immutable | `equals`/`hashCode` | Notes |
|---|---|---|---|
| `Money` | Yes | By amount + currency | `Comparable<Money>`, `Serializable` |
| `Account` | Yes | By ref + balance + normalBalance | `final` class |
| `Transaction` | Yes | By transactionRef (identity) | Legs list is unmodifiable; date is defensively copied |
| `TransactionLeg` | Yes | By accountRef + amount + side | `Serializable` |
| `TransferRequest` | Effectively | — | Built via step-builder; legs list is unmodifiable on read |
| `TrialBalance` | Yes | — | Entries list is unmodifiable |
| `TrialBalanceEntry` | Yes | By all fields | `BigDecimal` comparison uses `compareTo` |

The `Ledger` class is a stateful service (not a value object). Its `commit()`, `init()`, and `dropAll()` methods return `this` for fluent chaining but mutate the underlying storage.

## Accounting Glossary

| API Term | Standard Accounting Term | Description |
|---|---|---|
| `Ledger` | General Ledger | The principal book of record that contains all accounts |
| `ChartOfAccounts` | Chart of Accounts | The organized listing of all accounts in the ledger |
| `Account` | Account | A record of increases and decreases in a specific asset, liability, equity, revenue, or expense |
| `.debit()` | Debit posting | Subtracts from the stored signed balance |
| `.credit()` | Credit posting | Adds to the stored signed balance |
| `AccountSide.DEBIT` | Normal-balance label | Marks assets and expenses for reporting; does not change posting arithmetic |
| `AccountSide.CREDIT` | Normal-balance label | Marks liabilities, equity, and revenue for reporting; does not change posting arithmetic |
| `normalBalance` | Normal Balance | The side (debit or credit) that increases an account's value |
| `TransferRequest` | Journal Entry | A record of a transaction affecting two or more accounts |
| `TransactionLeg` | Line Item / Entry | A single debit or credit within a journal entry |
| `Transaction` | Posted Journal Entry | A finalized journal entry with a date and reference number |
| `commit()` | Posting | The process of transferring a journal entry to the ledger accounts |
| `TrialBalance` | Trial Balance | A listing of all accounts and their balances to verify that total debits equal total credits |
| `dropAll()` | — | Resets the ledger (no direct accounting equivalent; similar to starting a new fiscal period from scratch) |
| `Money` | Monetary Amount | A value combining an amount with a currency |
| `printHistoryLog()` | Transaction Register | A chronological listing of all posted transactions |
| `listAccounts()` | Account Listing | A view of all accounts with their current balances |
| `getAccountBalance()` | Account Balance | The current amount in an account after all debits and credits |

## Migration from v0.1.0

> **Full migration guide with before/after diffs**: [MIGRATION.md](MIGRATION.md)

### Breaking Changes in v1.0.0

1. **Multi-module**: The single artifact is now split into `bookkeeping-core` (in-memory) and `bookkeeping-jdbc` (database). Change your dependency from `com.yanimetaxas:bookkeeping` to the module(s) you need.

2. **Builder API**: `.options()` still works — just update the import:

   ```java
   // v0.1.0
   import com.yanimetaxas.bookkeeping.ConnectionOptions;
   new LedgerBuilder(chart).options(ConnectionOptions.EMBEDDED_H2_CONNECTION).build().init();

   // v1.0.0 — same method name, different import
   import com.yanimetaxas.bookkeeping.jdbc.ConnectionOptions;
   new LedgerBuilder(chart).options(ConnectionOptions.EMBEDDED_H2_CONNECTION).build().init();
   ```

3. **Package changes**: JDBC classes moved from `com.yanimetaxas.bookkeeping` to `com.yanimetaxas.bookkeeping.jdbc`:
   - `ConnectionOptions` → `com.yanimetaxas.bookkeeping.jdbc.ConnectionOptions`
   - `DataSourceDriver` → `com.yanimetaxas.bookkeeping.jdbc.DataSourceDriver`

4. **Removed classes**: `BankFactory`, `BankFactoryImpl`, `AbstractAccountingConcept`, and `Initializable` are removed. Use `StorageProvider` / `JdbcStorageProvider` instead.

5. **Default is in-memory**: `LedgerBuilder.build()` without `.storage()` creates an in-memory ledger (no database). In v0.1.0, a database was always required.

## Building from Source

The cookbook in `bookkeeping-examples` runs with:

```bash
mvn -pl bookkeeping-examples -am compile -Prun-example \
  -Dexec.mainClass=com.yanimetaxas.bookkeeping.examples.ECommerceLedgerExample
```

Replace the class with `ExpenseTrackerExample`, `MultiCurrencyFXExample`, `JdbcPersistenceExample`, `SpringBootLedgerExample`, `SqliteFileLedgerExample`, `RejectedTransferExample`, `ReopenLedgerExample`, `FixedClockExample`, `MultiCurrencyTransferExample`, or `SupportedDatabasesExample`.

Tests assert with RealityCheck 1.1.0 and run on JUnit Jupiter 6.1.3. The Spring Boot starter targets Spring Boot 4.1.1.

```bash
mvn -T 1C verify                 # Compile, test, and run integration tests
mvn test                         # Unit tests only
mvn -T 1C verify -DskipITs       # Full validation without Docker
mvn -T 1C verify -pl bookkeeping-core -am   # Core module only
```

## Publishing to Maven Central

See [RELEASE.md](RELEASE.md) for the release process.

## Contributing

Please read [CONTRIBUTING.md](CONTRIBUTING.md) for details on the code of conduct and the process for submitting pull requests.

## License

This project is licensed under the MIT License — see the [LICENSE](LICENSE) file for details.
