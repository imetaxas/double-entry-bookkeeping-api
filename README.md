[![Build Status](https://travis-ci.org/imetaxas/double-entry-bookkeeping-spring-jta.svg?branch=master)](https://travis-ci.org/imetaxas/double-entry-bookkeeping-spring-jta)
[![Coveralls Coverage](https://coveralls.io/repos/github/imetaxas/double-entry-bookkeeping-spring-jta/badge.svg?branch=master)](https://coveralls.io/github/imetaxas/double-entry-bookkeeping-spring-jta?branch=master)
[![CodeCov Coverage](https://codecov.io/gh/imetaxas/double-entry-bookkeeping-spring-jta/graph/badge.svg?branch=master)](https://codecov.io/gh/imetaxas/double-entry-bookkeeping-spring-jta?branch=master)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

# Double Entry Bookkeeping API
Implementation of a Double-entry bookkeeping service using Spring 5, Java Transaction API and the H2 database in embedded mode.

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
```
ChartOfAccounts chartOfAccounts = ChartOfAccountsBuilder.create()
              .account(CASH_ACCOUNT_1, "1000.00", "EUR")
              .account(REVENUE_ACCOUNT_1, "0.00", "EUR")
              .build();
      
Ledger ledger = new Ledger(chartOfAccounts);

  
TransferRequest transferRequest1 = ledger.createTransferRequest()
    .reference("T1")
    .type("testing1")
    .account(CASH_ACCOUNT_1).debit("5.00", "EUR")
    .account(REVENUE_ACCOUNT_1).credit("5.00", "EUR")
    .build();
    
ledger.commit(transferRequest1);
  
TransferRequest transferRequest2 = ledger.createTransferRequest()
    .reference("T2")
    .type("testing2")
    .account(CASH_ACCOUNT_1).debit("10.50", "EUR")
    .account(REVENUE_ACCOUNT_1).credit("10.50", "EUR")
    .build();
  
ledger.commit(transferRequest2);
  
List<Transaction> cashAccountTransactionList = ledger.findTransactions(CASH_ACCOUNT_1);
List<Transaction> revenueAccountTransactionList = ledger.findTransactions(REVENUE_ACCOUNT_1);
  
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