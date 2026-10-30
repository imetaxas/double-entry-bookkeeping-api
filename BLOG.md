# Why We Built a Zero-Dependency Double-Entry Bookkeeping Library for the JVM

> A technical deep-dive into designing an embeddable accounting engine that works anywhere Java runs — from microservices to Android apps to serverless functions.

## The Problem: Financial Logic Deserves a Real Library

Every application that handles money eventually reinvents the same wheel: "debit this account, credit that account, make sure it balances." Sometimes it's a fintech startup building a payment platform. Sometimes it's an e-commerce backend tracking inventory costs. Sometimes it's a SaaS product managing subscription revenue.

In every case, the underlying model is the same: **double-entry bookkeeping** — a system perfected over 500 years, where every financial event touches at least two accounts and the books always balance.

Yet the JVM ecosystem lacked a lightweight, embeddable library for this. Most solutions fell into two camps:

1. **Full accounting systems** (ERPNext, GnuCash) — monolithic applications, not libraries. You can't embed them in your service.
2. **Home-grown implementations** — scattered across codebases, untested, and missing edge cases like multi-currency or overdraft protection.

We set out to build a third option: a library you can add with a single Maven dependency, use with three lines of code, and extend when your needs grow.

## Design Principles

### 1. Zero Dependencies for the Core

The biggest barrier to library adoption is transitive dependency hell. Add one library, and suddenly you're resolving version conflicts between three logging frameworks, two JSON parsers, and a Spring version you didn't ask for.

Our core module (`bookkeeping-core`) has exactly **one** runtime dependency: SLF4J's API jar (a 40 KB interface library). Everything else — the accounting engine, transaction validation, trial balance computation — is pure Java 17+. No Spring, no Hibernate, no Guava, no Jackson.

```xml
<dependency>
  <groupId>com.yanimetaxas</groupId>
  <artifactId>bookkeeping-core</artifactId>
  <version>1.0.0</version>
</dependency>
```

This design means the library works everywhere: Spring Boot services, Quarkus microservices, plain Java applications, Android apps, AWS Lambda functions, even Gradle build plugins.

### 2. Persistence is Pluggable, Not Mandatory

Not every use case needs a database. Unit tests, simulations, and stateless serverless functions benefit from a pure in-memory implementation. But production financial systems need durability.

We solved this with the **StorageProvider SPI** — a simple interface with four methods:

```java
public interface StorageProvider {
    AccountService accountService();
    TransferService transferService();
    void initialize();
    void dropAll();
}
```

The core module ships with `InMemoryStorageProvider`. Need a database? Add the `bookkeeping-jdbc` module:

```java
Ledger ledger = new LedgerBuilder(chart)
    .options(ConnectionOptions.EMBEDDED_H2_CONNECTION)
    .build()
    .init();
```

The JDBC module itself uses **zero frameworks** — pure `java.sql` with manual transaction management. No Spring JDBC, no JPA, no ORM. This keeps the dependency footprint tiny and gives you full control.

### 3. The Builder API Prevents Mistakes

Accounting has rules. Every transaction must have at least two legs. Debits must equal credits. You shouldn't mix currencies in a single transfer. Rather than catching these errors at runtime, we use a **step-builder pattern** that guides you through the process:

```java
ledger.commit(ledger.createTransferRequest()
    .reference("INV-001")       // Step 1: reference (required)
    .type("sale")               // Step 2: type (required)
    .account("Cash")            // Step 3: pick an account
    .debit("100.00", "EUR")     // Step 4: debit it
    .account("Revenue")         // Step 5: pick another account
    .credit("100.00", "EUR")    // Step 6: credit it
    .build());                  // Validates balance before returning
```

The builder won't let you create a transaction with a single leg or skip the reference. The engine validates that legs balance and currencies match at commit time, throwing specific exceptions (`UnbalancedLegsException`, `InsufficientFundsException`) rather than generic errors.

## Accounting Concepts, Mapped to Code

One of our goals was to make the API feel natural to both developers and accountants. Here's how standard accounting maps to the library:

| Accounting Concept | API | What It Does |
|---|---|---|
| General Ledger | `Ledger` | The top-level container for all accounts and transactions |
| Chart of Accounts | `ChartOfAccounts` | Defines the account structure before the ledger opens |
| Journal Entry | `TransferRequest` | A balanced set of debits and credits to be committed |
| Posting | `ledger.commit(...)` | Atomically applies the journal entry to all accounts |
| Trial Balance | `ledger.computeTrialBalance()` | Verifies that total debits equal total credits |
| Normal Balance | `AccountSide.DEBIT` / `CREDIT` | Declares which side increases the account's value |

Every account has an optional **normal balance** — `DEBIT` for assets and expenses, `CREDIT` for liabilities, equity, and revenue. This isn't just metadata: it powers the trial balance computation and gives transaction legs explicit meaning.

## Real-World Examples

### E-Commerce: Revenue, COGS, and Shipping

```java
// Customer pays 130 EUR for a product
ledger.commit(ledger.createTransferRequest()
    .reference("ORD-001-REVENUE").type("customer-payment")
    .account("Cash").debit("130.00", "EUR")
    .account("Revenue").credit("110.00", "EUR")
    .account("SalesTaxPayable").credit("20.00", "EUR")
    .build());

// Recognize cost of goods sold
ledger.commit(ledger.createTransferRequest()
    .reference("ORD-001-COGS").type("cost-of-goods")
    .account("CostOfGoodsSold").debit("80.00", "EUR")
    .account("Inventory").credit("80.00", "EUR")
    .build());
```

### Multi-Currency FX

Each currency gets its own ledger (since you can't balance EUR against USD in a single entry). An FX trade becomes two linked entries:

```java
// EUR side: sell 1,850 EUR
eurLedger.commit(eurLedger.createTransferRequest()
    .reference("FX-001-EUR").type("fx-sell")
    .account("EUR:FXSettlement").debit("1850.00", "EUR")
    .account("EUR:Cash").credit("1850.00", "EUR")
    .build());

// USD side: buy 2,000 USD
usdLedger.commit(usdLedger.createTransferRequest()
    .reference("FX-001-USD").type("fx-buy")
    .account("USD:Cash").debit("2000.00", "USD")
    .account("USD:FXSettlement").credit("2000.00", "USD")
    .build());
```

### Personal Expense Tracking

```java
// Salary received
ledger.commit(ledger.createTransferRequest()
    .reference("SAL-MAR").type("income")
    .account("BankAccount").debit("4500.00", "USD")
    .account("Salary").credit("4500.00", "USD")
    .build());

// Grocery shopping
ledger.commit(ledger.createTransferRequest()
    .reference("GRO-001").type("expense")
    .account("Groceries").debit("85.50", "USD")
    .account("BankAccount").credit("85.50", "USD")
    .build());
```

## Architecture: Modules and What They Contain

```
bookkeeping-parent/
├── bookkeeping-core/                  Pure in-memory engine (~50 KB, SLF4J only)
├── bookkeeping-jdbc/                  Plain JDBC persistence (~30 KB, no frameworks)
├── bookkeeping-spring-boot-starter/   Auto-configuration for Spring Boot apps
└── bookkeeping-examples/              Cookbook examples
```

| Module | Dependencies | Use Case |
|---|---|---|
| `bookkeeping-core` | SLF4J only | Anywhere: microservices, Android, Lambda, tests |
| `bookkeeping-jdbc` | `core` + JDBC driver | Production apps needing database persistence |
| `bookkeeping-spring-boot-starter` | `core` + Spring Boot | Spring Boot apps wanting zero-config ledger beans |

The JDBC module supports **H2, SQLite, HSQLDB, Apache Derby, MySQL, and PostgreSQL** — and since it uses plain `java.sql`, any JDBC-compliant database should work. You can even pass your own `DataSource` (e.g., HikariCP connection pool) for production use.

## Why Plain JDBC Instead of Spring/JPA/ORM?

We made the deliberate choice to use `java.sql.Connection` and `PreparedStatement` directly, instead of Spring JDBC, JPA, or any ORM. Here's why:

1. **Zero transitive dependencies.** Spring JDBC pulls in `spring-core`, `spring-beans`, `spring-tx` — over 4 MB of JARs that conflict with whatever Spring version your app already uses.

2. **Transparent transactions.** Our transaction boundary is explicit: `conn.setAutoCommit(false)`, execute, `conn.commit()`. No proxy magic, no annotation processing, no "oops, my @Transactional didn't fire because I called the method internally."

3. **Universal compatibility.** Plain JDBC works with Java 17+, any application server, any framework, any platform. No Spring context required.

4. **Library, not framework.** A library should work *inside* your framework, not *be* a framework. If you want Spring integration, we provide it as a separate optional starter module — not as a core requirement.

## Getting Started

### Maven

```xml
<!-- In-memory only (zero dependencies) -->
<dependency>
  <groupId>com.yanimetaxas</groupId>
  <artifactId>bookkeeping-core</artifactId>
  <version>1.0.0</version>
</dependency>

<!-- With JDBC persistence -->
<dependency>
  <groupId>com.yanimetaxas</groupId>
  <artifactId>bookkeeping-jdbc</artifactId>
  <version>1.0.0</version>
</dependency>

<!-- Spring Boot starter -->
<dependency>
  <groupId>com.yanimetaxas</groupId>
  <artifactId>bookkeeping-spring-boot-starter</artifactId>
  <version>1.0.0</version>
</dependency>
```

### Spring Boot (application.yml)

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

Then just inject the ledger:

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

## What's Next

We're actively working on:

- **Event sourcing adapter** — replay ledger state from an event log
- **Kotlin DSL** — idiomatic Kotlin builder for the transfer API
- **Reporting extensions** — income statement, balance sheet generation
- **Audit trail** — immutable history of all ledger mutations

The library is open-source under the MIT license. We welcome contributions, bug reports, and feature requests.

**GitHub**: [github.com/imetaxas/double-entry-bookkeeping-api](https://github.com/imetaxas/double-entry-bookkeeping-api)
**Maven Central**: [com.yanimetaxas:bookkeeping-core](https://search.maven.org/artifact/com.yanimetaxas/bookkeeping-core)
