package com.yanimetaxas.bookkeeping.jdbc;

import static io.github.imetaxas.realitycheck.RealityAssertions.assertThat;
import static io.github.imetaxas.realitycheck.RealityAssertions.assertThatThrownBy;

import com.yanimetaxas.bookkeeping.exception.AccountNotFoundException;
import com.yanimetaxas.bookkeeping.exception.InfrastructureException;
import com.yanimetaxas.bookkeeping.model.Money;
import com.yanimetaxas.bookkeeping.model.Transaction;
import com.yanimetaxas.bookkeeping.model.TransferRequest;
import com.yanimetaxas.bookkeeping.service.AccountService;
import com.yanimetaxas.bookkeeping.service.TransferService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class BankFunctionalTest {

  private static final String CASH_ACCOUNT_1 = "cash_1_EUR";
  private static final String REVENUE_ACCOUNT_1 = "revenue_1_EUR";

  private AccountService accountService;
  private TransferService transferService;

  @BeforeEach
  void setUp() {
    JdbcStorageProvider storage = new JdbcStorageProvider(ConnectionOptions.EMBEDDED_H2_CONNECTION);
    storage.dropAll();
    storage.initialize("functional-test");
    accountService = storage.accountService();
    transferService = storage.transferService();
  }

  @Test
  void findTransactionsByAccountRefWhenNoTransactionsExistReturnsEmpty() {
    accountService.createAccount(CASH_ACCOUNT_1, Money.toMoney("10.00", "EUR"));
    List<Transaction> transactions = transferService.findTransactionsByAccountRef(CASH_ACCOUNT_1);
    assertThat(transactions).isEmpty();
  }

  @Test
  void getTransactionByRefWhenRefNotExistsReturnsNull() {
    assertThat(transferService.getTransactionByRef("")).isNull();
  }

  @Test
  void transferFundsWhenTransferHasOneLegThrowsException() {
    accountService.createAccount(CASH_ACCOUNT_1, Money.toMoney("1000.00", "EUR"));

    assertThatThrownBy(
            () ->
                transferService.transferFunds(
                    TransferRequest.builder()
                        .reference("T1")
                        .type("testing")
                        .account(CASH_ACCOUNT_1)
                        .amount(Money.toMoney("-5.00", "EUR"))
                        .build()))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void transferFundsWhenAccountNotFoundThrowsException() {
    accountService.createAccount(CASH_ACCOUNT_1, Money.toMoney("1000.00", "EUR"));
    accountService.createAccount(REVENUE_ACCOUNT_1, Money.toMoney("0.00", "EUR"));

    assertThatThrownBy(
            () ->
                transferService.transferFunds(
                    TransferRequest.builder()
                        .reference("T1")
                        .type("testing")
                        .account("wrong_account")
                        .amount(Money.toMoney("-5.00", "EUR"))
                        .account(REVENUE_ACCOUNT_1)
                        .amount(Money.toMoney("5.00", "EUR"))
                        .build()))
        .isInstanceOf(AccountNotFoundException.class);
  }

  @Test
  void accountAlreadyExistsThrowsException() {
    accountService.createAccount(CASH_ACCOUNT_1, Money.toMoney("1000.00", "EUR"));

    assertThatThrownBy(
            () -> accountService.createAccount(CASH_ACCOUNT_1, Money.toMoney("1000.00", "EUR")))
        .isInstanceOf(InfrastructureException.class);
  }

  @Test
  void getAccountBalanceWhenNotExistsThrowsException() {
    assertThatThrownBy(() -> accountService.getAccountBalance(CASH_ACCOUNT_1))
        .isInstanceOf(AccountNotFoundException.class);
  }
}
