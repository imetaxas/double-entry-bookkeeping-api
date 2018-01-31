[![Build Status](https://travis-ci.org/imetaxas/double-entry-bookkeeping-spring-jta.svg?branch=master)](https://travis-ci.org/imetaxas/double-entry-bookkeeping-spring-jta)
[![Coverage Status](https://coveralls.io/repos/github/imetaxas/double-entry-bookkeeping-spring-jta/badge.svg?branch=master)](https://coveralls.io/github/imetaxas/double-entry-bookkeeping-spring-jta?branch=master)
# Double Entry Bookkeeping
Implementation of a Double-entry bookkeeping service using Spring 4, Java Transaction API and the H2 database in embedded mode.

Concept description
--------------------

**Double-entry bookkeeping** involves making at least two entries or legs for every transaction.
A debit in one account and a corresponding credit in another account.
The sum of all debits should always equal the sum of all credits, providing a simple way to check for errors.
The following rules **MUST** apply:

  * An account **MUST NOT** be overdrawn, i.e. have a negative balance.
  * A monetary transaction **MAY** support multiple currencies as long as the total balance for the transaction legs with the same currency is zero.
  * The concepts of debit and credit are simplified by specifying that monetary transactions towards an account can have either a positive or negative value.


Build
-------
mvn package


## Contributing
If you would like to help making this project better, see the [CONTRIBUTING.md](CONTRIBUTING.md).  

## Maintainers
Send any other comments and suggestions to [Yani Metaxas](https://github.com/imetaxas).

## License
This project is distributed under the [MIT License](LICENSE).