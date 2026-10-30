# Changelog

All notable changes to this project will be documented in this file.

## [1.0.0] - 2026-10-14

### Breaking Changes

- **Java 17+ required** (was Java 8+ in 0.1.0)
- **Multi-module split**: Single artifact `com.yanimetaxas:bookkeeping` is now four Maven modules:
  - `bookkeeping-core` — pure in-memory engine with zero runtime dependencies (beyond SLF4J)
  - `bookkeeping-jdbc` — zero-framework JDBC persistence using plain `java.sql` (no Spring)
  - `bookkeeping-spring-boot-starter` — Spring Boot auto-configuration (optional)
  - `bookkeeping-examples` — cookbook-style examples (not published)
- **`LedgerBuilder.options()` preserved** — same method name, just update the `ConnectionOptions` import to `com.yanimetaxas.bookkeeping.jdbc.ConnectionOptions`. For custom providers, use `.storage(StorageProvider)`
- **Default mode is in-memory** — `LedgerBuilder.build()` without `.storage()` creates a zero-dependency in-memory ledger. In 0.1.0, a database connection was always required.
- **JDBC classes relocated** — `ConnectionOptions`, `DataSourceDriver` moved to `com.yanimetaxas.bookkeeping.jdbc`
- **Removed classes** — `BankFactory`, `BankFactoryImpl`, `AbstractAccountingConcept`, `Initializable`, `BankContextUtil`, Spring XML config are gone. Use `StorageProvider` instead.
- **`build().init()` no longer drops tables** — existing data for the same ledger name is preserved. Use `dropAll()` before `init()` for the old behavior.
- **Signed posting model** — `.debit()` decreases the stored balance and `.credit()` increases it. `AccountSide` is a reporting label and does not reverse that arithmetic.
- **JDBC rows are scoped by ledger name** — reopening that name reloads its accounts; a different name in the same database is isolated.
- **Exact decimal storage** — amounts are stored as decimal text, so values such as `5.123` are not rounded to two places.
- **Rejected transfers are atomic** — an overdraft leaves balances and transaction history unchanged.
- **Normal balance is stored with the account** — `listAccounts()` returns the `AccountSide` supplied in the chart.
- **Trial balance classifies a negative opening on the opposite side** from a positive opening.
- **JDBC transaction timestamps keep the time of day**, and account, transaction, and type references can be up to 255 characters.
- **Tag-triggered Maven Central publishing** — pushing a `v*` tag runs `.github/workflows/release.yml`.
- **JaCoCo enforces 95% instruction, line, and branch coverage** on `bookkeeping-core`, `bookkeeping-jdbc`, and `bookkeeping-spring-boot-starter`.
- **`AccountDao` no longer posts balances** — a transfer writes the new amount inside its own transaction
- **Client creation time is a timestamp**, and a transaction leg must reference its transaction and its account
- **`printHistoryLog()` uses SLF4J** instead of `System.out.println` — add an SLF4J binding to see output
- **MySQL driver class** changed from `com.mysql.jdbc.Driver` to `com.mysql.cj.jdbc.Driver`
- **Guava removed** from transitive dependencies
- **Money constructor** now throws `NullPointerException` for null arguments (uses `Objects.requireNonNull`)

### Added

- **Pure in-memory mode** — thread-safe, lock-based bookkeeping with no external dependencies
- **`StorageProvider` SPI** — pluggable persistence with `InMemoryStorageProvider` and `JdbcStorageProvider`
- **`StorageFactory` interface** — allows `ConnectionOptions` to be passed directly to `.storage()` without exposing `JdbcStorageProvider`
- **`AccountSide` enum** — explicit debit/credit classification (`AccountSide.DEBIT`, `AccountSide.CREDIT`)
- **Normal balance concept** — accounts support optional `AccountSide normalBalance` for T-account semantics
- **`TransferRequest.debit()` / `.credit()` builder methods** — explicitly label transaction legs
- **`TrialBalance`** — computes total debits/credits, verifies balance
- **`Ledger.computeTrialBalance()`**, **`printTrialBalance()`**, **`listAccounts()`**, **`dropAll()`**
- **SQLite support** — `DataSourceDriver.JDBC_SQLITE` for file-based persistence
- **Spring Boot Starter** — auto-wires a `Ledger` bean from `bookkeeping.*` application properties
- **`JdbcStorageProvider(DataSource, String)` constructor** — accepts external DataSource (e.g., HikariCP)
- **Cookbook examples** — e-commerce, expenses, FX ledgers, a multi-currency transfer, JDBC, Spring Boot, SQLite files, rejected transfers, reopening a ledger, a fixed clock, and embedded H2, HSQLDB, and Derby
- **Immutable value objects** — all model classes have proper `equals`/`hashCode`/`toString`
- **Thread safety documentation** in README
- **Accounting glossary** in README
- **GitHub Actions CI**, **JaCoCo/Codecov**, **Testcontainers** for MySQL/PostgreSQL
- **JPMS support** via `Automatic-Module-Name`
- **`LedgerBuilder.clock(Clock)`** — transaction time and the trial-balance stamp come from the supplied clock. Spring uses an application `Clock` bean when one is present
- **Text reports** — ledger history and trial-balance tables are rendered by `LedgerReport` and `TrialBalanceReport`
- **Trial balance reads each transaction once** — storage returns the full history in one pass
- **Stored openings** — each account keeps the amount it was opened with. A reopened ledger classifies that amount, so a different chart does not change the trial balance
- **`Transaction.getTransactionDate()` returns `Instant`**
- **Ledger history reads the transaction list once** and groups it in memory. History and the trial balance list the accounts that are stored and print their exact decimal text. History timestamps are UTC
- **`StorageProvider.open(String)`** returns the services for that client. `accountService()` and `transferService()` then return the most recently opened session on every thread. `open(String, Clock)` stamps that session only, so a clock set on a pooled thread does not stick to a later open
- **Derby `dropAll()`** can run before the schema exists
- **SQLite foreign keys** are enabled for an external `DataSource` as well as the built-in one
- **Branch coverage** is enforced alongside instruction and line coverage

### Changed

- **JDBC module is framework-free** — plain `java.sql` with explicit transaction management (SERIALIZABLE isolation)
- **`TransactionLeg`** carries optional `AccountSide`, exposes `getSide()` and `getAbsoluteAmount()`
- **Non-destructive schema initialization** — `CREATE TABLE IF NOT EXISTS`
- **Formatted output** — box-drawn tables with dynamic column widths
- **JUnit 4 → JUnit 5**, **H2 1.4 → 2.4**, all drivers updated
- **Assertions use RealityCheck 1.1.0** (`io.github.imetaxas:realitycheck-core`). Tests run on JUnit Jupiter 6.1.3
- **Spring Boot starter 3.4.4 → 4.1.1**. JaCoCo 0.8.15, Surefire and Failsafe 3.6.0
- **Libraries**: SLF4J 2.0.20, Lombok 1.18.48, H2 2.5.252, SQLite JDBC 3.53.4.0, PostgreSQL JDBC 42.7.14, MySQL Connector/J 26.7.0. Derby stays 10.16.1.1 because 10.17 requires Java 19
- **Testcontainers 1.21.4 → 2.0.5**. MySQL and PostgreSQL use the 2.0 modules (`testcontainers-mysql`, `testcontainers-postgresql`, `testcontainers-junit-jupiter`)
- **Travis CI → GitHub Actions**, **Cobertura/Coveralls → JaCoCo/Codecov**

### Removed

- `System.out.println` — all output via SLF4J
- Spring framework dependency from JDBC module
- Hibernate, Guava, and AssertJ. The old RealityCheck dependency is replaced by `io.github.imetaxas:realitycheck-core` 1.1.0
- JUnit 4. Testcontainers 2.0.5 no longer needs it
- `applicationContext.xml`, Travis CI config, `CoverageTool`

## [0.1.0] - 2018-09-06

Initial release.

- Double-entry bookkeeping with multi-currency, multi-leg transactions
- Embedded H2, HSQL, Derby support
- JDBC H2, MySQL, PostgreSQL support
- Step builder API for transfer requests
- Spring 5 + JDBC persistence
