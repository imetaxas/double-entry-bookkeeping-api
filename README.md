[![Build Status](https://travis-ci.org/imetaxas/double-entry-bookkeeping-api.svg?branch=master)](https://travis-ci.org/imetaxas/double-entry-bookkeeping-api)
[![codecov](https://codecov.io/gh/imetaxas/double-entry-bookkeeping-api/branch/master/graph/badge.svg)](https://codecov.io/gh/imetaxas/double-entry-bookkeeping-api)
[![Codacy Badge](https://api.codacy.com/project/badge/Grade/970cf46c3cf84ed281f62a60ca465d5b)](https://www.codacy.com/app/imetaxas/double-entry-bookkeeping-api?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=imetaxas/double-entry-bookkeeping-api&amp;utm_campaign=Badge_Grade)[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.yanimetaxas/bookkeeping/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.yanimetaxas/bookkeeping/)
[![PRs Welcome](https://img.shields.io/badge/PRs-welcome-green.svg)](http://makeapullrequest.com)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

# Double Entry Bookkeeping API
A library of the Double-entry bookkeeping concept which is downloadable from the [Central Repository](https://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.yanimetaxas%22). 

It uses Spring 5 and the Java Transaction API internally.

It supports H2, HSQL and Derby databases in embedded mode.

And H2, MySQL and Postgres using JDBC.

Description
--------------------

**Double-entry bookkeeping** involves making at least two entries or legs for every transaction.
A debit in one account and a corresponding credit in another account.
The sum of all debits should always equal the sum of all credits, providing a simple way to check for errors.
The following rules **MUST** apply:

  * An account **MUST NOT** be overdrawn, i.e. have a negative balance.
  * A monetary transaction **MAY** support multiple currencies as long as the total balance for the transaction legs with the same currency is zero.
  * The concepts of debit and credit are simplified by specifying that monetary transactions towards an account can have either a positive or negative value.

API
----
**Using embedded H2**
```
ChartOfAccounts chartOfAccounts = ChartOfAccountsBuilder.create()
              .account("CASH_ACCOUNT_1", "1000.00", "EUR")
              .account("REVENUE_ACCOUNT_1", "0.00", "EUR")
              .build();

Ledger ledger = new Ledger("My embedded H2 ledger", chartOfAccounts);
```

**Using Embedded HSQL**
```
ChartOfAccounts chartOfAccounts = ChartOfAccountsBuilder.create()
        .account("CASH_ACCOUNT_1", "1000.00", "EUR")
        .account("REVENUE_ACCOUNT_1", "0.00", "EUR")
        .build();

Ledger ledger = new Ledger("My embedded HSQL ledger", chartOfAccounts, ConnectionOptions.EMBEDDED_HSQL_CONNECTION);
```

**Using Embedded Derby**
```
ChartOfAccounts chartOfAccounts = ChartOfAccountsBuilder.create()
        .account("CASH_ACCOUNT_1", "1000.00", "EUR")
        .account("REVENUE_ACCOUNT_1", "0.00", "EUR")
        .build();

Ledger ledger = new Ledger("My embedded Derby ledger", chartOfAccounts, ConnectionOptions.EMBEDDED_DERBY_CONNECTION);
```

**Using JDBC H2**
```                        
ConnectionOptions options = new ConnectionOptions(
        DataSourceDriver.JDBC_H2,
        "URL",
        "USERNAME",
        "PASSWORD");

Ledger ledger = new Ledger("My JDBC H2 ledger", chartOfAccounts, options);
``` 

**Using JDBC MySQL**
```                        
ConnectionOptions options = new ConnectionOptions(
        DataSourceDriver.JDBC_MYSQL,
        "URL",
        "USERNAME",
        "PASSWORD");

Ledger ledger = new Ledger("My JDBC MySQL ledger", chartOfAccounts, options);
``` 

**Using JDBC Postgres**
```                        
ConnectionOptions options = new ConnectionOptions(
        DataSourceDriver.JDBC_POSTGRES,
        "URL",
        "USERNAME",
        "PASSWORD");

Ledger ledger = new Ledger("My JDBC Postgres ledger", chartOfAccounts, options);
``` 

**Commit Transfer Requests**
``` 
TransferRequest transferRequest1 = ledger.createTransferRequest()
    .reference("T1")
    .type("testing1")
    .account("CASH_ACCOUNT_1").debit("5.00", "EUR")
    .account("REVENUE_ACCOUNT_1").credit("5.00", "EUR")
    .build();
    
ledger.commit(transferRequest1);
  
TransferRequest transferRequest2 = ledger.createTransferRequest()
    .reference("T2")
    .type("testing2")
    .account("CASH_ACCOUNT_1").debit("10.50", "EUR")
    .account("REVENUE_ACCOUNT_1").credit("10.50", "EUR")
    .build();
  
ledger.commit(transferRequest2);
```
**Search the Ledger for committed Transactions**
```
List<Transaction> cashAccountTransactionList = ledger.findTransactions("CASH_ACCOUNT_1");
List<Transaction> revenueAccountTransactionList = ledger.findTransactions("REVENUE_ACCOUNT_1");

Transaction transaction1 = ledger.getTransactionByRef("T1");
Transaction transaction2 = ledger.getTransactionByRef("T2");
```
**Print the Ledger's history of operations**
```
ledger.printHistoryLog();
```

Build
-------
mvn package


## Contributing
If you would like to help making this project better, see the [CONTRIBUTING.md](CONTRIBUTING.md).  

## Maintainers
Send any other comments and suggestions to [Yani Metaxas](https://github.com/imetaxas).

## License
This project is distributed under the [MIT License](LICENSE).