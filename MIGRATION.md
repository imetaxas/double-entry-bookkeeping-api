# Migration Guide: v0.1.0 → v1.0.0

This guide covers every breaking change with side-by-side diffs so you can migrate mechanically.

## 1. Maven Dependency

The single artifact is now split into focused modules. You only pull in what you use.

```diff
 <!-- v0.1.0 -->
-<dependency>
-  <groupId>com.yanimetaxas</groupId>
-  <artifactId>bookkeeping</artifactId>
-  <version>0.1.0</version>
-</dependency>

 <!-- v1.0.0 — in-memory only (zero dependencies) -->
+<dependency>
+  <groupId>com.yanimetaxas</groupId>
+  <artifactId>bookkeeping-core</artifactId>
+  <version>1.0.0</version>
+</dependency>

 <!-- v1.0.0 — add JDBC persistence (optional) -->
+<dependency>
+  <groupId>com.yanimetaxas</groupId>
+  <artifactId>bookkeeping-jdbc</artifactId>
+  <version>1.0.0</version>
+</dependency>
```

## 2. Imports

JDBC classes moved from the root package to `com.yanimetaxas.bookkeeping.jdbc`:

```diff
-import com.yanimetaxas.bookkeeping.ConnectionOptions;
-import com.yanimetaxas.bookkeeping.DataSourceDriver;
+import com.yanimetaxas.bookkeeping.jdbc.ConnectionOptions;
+import com.yanimetaxas.bookkeeping.jdbc.DataSourceDriver;
```

`bookkeeping-core` keeps a deprecated `ConnectionOptions` wrapper, but it does not provide the v0.1.0 constants or constructors. Those types live in `bookkeeping-jdbc`, which keeps database drivers out of the zero-dependency core module. Code that used `ConnectionOptions.EMBEDDED_H2_CONNECTION` must import `com.yanimetaxas.bookkeeping.jdbc.ConnectionOptions`.

Core classes are unchanged:

```java
// These stay the same
import com.yanimetaxas.bookkeeping.Ledger;
import com.yanimetaxas.bookkeeping.Ledger.LedgerBuilder;
import com.yanimetaxas.bookkeeping.ChartOfAccounts;
```

## 3. Builder API

The `.options()` method is preserved — the call site is identical:

```diff
 // No change needed — same method, same signature
 Ledger ledger = new LedgerBuilder(chart)
     .options(ConnectionOptions.EMBEDDED_H2_CONNECTION)
     .build()
     .init();
```

## 4. In-Memory Mode (new default)

In v0.1.0, a database was always required. In v1.0.0, omitting `.options()` gives you a pure in-memory ledger:

```diff
 // v0.1.0 — this would fail without a ConnectionOptions
-Ledger ledger = new LedgerBuilder(chart).build().init();

 // v1.0.0 — works out of the box (in-memory, zero dependencies)
+Ledger ledger = new LedgerBuilder(chart).build().init();
```

## 5. Removed Classes

| v0.1.0 Class | v1.0.0 Replacement |
|---|---|
| `BankFactory` | `StorageProvider` (SPI interface) |
| `BankFactoryImpl` | `InMemoryStorageProvider` or `JdbcStorageProvider` |
| `AbstractAccountingConcept` | Removed — use `Ledger` directly |
| `Initializable` | Removed — call `ledger.init()` explicitly |
| `BankContextUtil` | Removed — `JdbcStorageProvider` manages connections internally |

```diff
-BankFactory bankFactory = new BankFactoryImpl(options, chartOfAccounts);
-bankFactory.createBank();
+Ledger ledger = new LedgerBuilder(chart)
+    .options(ConnectionOptions.EMBEDDED_H2_CONNECTION)
+    .build().init();
```

## 6. Schema Initialization

v0.1.0 dropped and recreated tables on every run. v1.0.0 preserves existing data:

```diff
 // v0.1.0 — always dropped tables
-new LedgerBuilder(chart).options(options).build().init();

 // v1.0.0 — init() is non-destructive (CREATE TABLE IF NOT EXISTS)
+new LedgerBuilder(chart).options(options).build().init();

 // v1.0.0 — to get the old drop-and-recreate behavior:
+new LedgerBuilder(chart).options(options).build().dropAll().init();
```

## 7. Account Sides (new, optional)

v1.0.0 adds explicit debit/credit sides. This is **fully backward-compatible** — the old API still works:

```diff
 // v0.1.0 style — still works in v1.0.0
 new ChartOfAccountsBuilder()
     .create("Cash", "1000.00", "USD")
     .build();

 // v1.0.0 — optional explicit normal balance
+new ChartOfAccountsBuilder()
+    .create("Cash", "1000.00", "USD", AccountSide.DEBIT)
+    .create("Revenue", "0.00", "USD", AccountSide.CREDIT)
+    .build();
```

## 8. Transfer Request Builder (new debit/credit methods)

v0.1.0 only had `.account().amount()`. v1.0.0 adds `.debit()` / `.credit()` for clarity:

```diff
 // v0.1.0 style — still works
 ledger.createTransferRequest()
     .reference("T1").type("sale")
     .account("Cash").amount("50.00", "USD")
     .account("Revenue").amount("-50.00", "USD")
     .build();

 // v1.0.0 — explicit debit/credit (recommended)
+ledger.createTransferRequest()
+    .reference("T1").type("sale")
+    .account("Cash").debit("50.00", "USD")
+    .account("Revenue").credit("50.00", "USD")
+    .build();
```

## 9. Logging

v0.1.0 used `System.out.println`. v1.0.0 uses SLF4J:

```diff
 // To see output, add an SLF4J binding to your classpath:
+<dependency>
+  <groupId>org.slf4j</groupId>
+  <artifactId>slf4j-simple</artifactId>
+  <version>2.0.20</version>
+</dependency>
```

## 10. MySQL Driver

```diff
-com.mysql.jdbc.Driver
+com.mysql.cj.jdbc.Driver
```

This is a MySQL Connector/J 8.x change — update your `ConnectionOptions` if you use a custom driver class.

## Checklist

- [ ] Update Maven dependencies (single artifact → `bookkeeping-core` + optional `bookkeeping-jdbc`)
- [ ] Update imports: `ConnectionOptions` and `DataSourceDriver` to `...jdbc.*`
- [ ] Replace `BankFactory` / `BankFactoryImpl` with `LedgerBuilder`
- [ ] Add SLF4J binding if you relied on console output
- [ ] (Optional) Add `AccountSide` to chart of accounts
- [ ] (Optional) Switch `.amount()` to `.debit()` / `.credit()` in transfer requests
- [ ] Run tests
