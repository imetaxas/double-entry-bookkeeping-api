package com.yanimetaxas.bookkeeping;

import static com.yanimetaxas.realitycheck.Reality.checkThat;

import com.yanimetaxas.bookkeeping.exception.AccountNotFoundException;
import com.yanimetaxas.bookkeeping.exception.InfrastructureException;
import com.yanimetaxas.bookkeeping.exception.InsufficientFundsException;
import com.yanimetaxas.bookkeeping.exception.TransferRequestExistsException;
import com.yanimetaxas.bookkeeping.exception.UnbalancedLegsException;
import com.yanimetaxas.bookkeeping.model.Money;
import com.yanimetaxas.bookkeeping.model.Transaction;
import com.yanimetaxas.bookkeeping.model.TransferRequest;
import com.yanimetaxas.bookkeeping.service.AccountService;
import com.yanimetaxas.bookkeeping.service.TransferService;
import com.yanimetaxas.bookkeeping.tools.CoverageTool;
import com.yanimetaxas.bookkeeping.util.BankContextUtil;
import com.yanimetaxas.bookkeeping.validation.TransferValidationException;
import java.math.BigDecimal;
import java.util.Currency;
import java.util.List;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * @author yanimetaxas
 * @since 14-Nov-14
 */
public class BankFunctionalTest {

  private static final String FACTORY_CLASS_NAME = "com.yanimetaxas.bookkeeping.BankFactoryImpl";

  private static final String CASH_ACCOUNT_1 = "cash_1_EUR";
  private static final String REVENUE_ACCOUNT_1 = "revenue_1_EUR";

  private AccountService accountService;
  private TransferService transferService;

  @Before
  public void setupSystemStateBeforeEachTest() throws Exception {
    BankFactory bankFactory = (BankFactory) Class.forName(FACTORY_CLASS_NAME).newInstance();

    accountService = bankFactory.getAccountService();
    transferService = bankFactory.getTransferService();

    bankFactory.setupInitialData(ConnectionOptions.EMBEDDED_H2_CONNECTION);
  }

  @Test
  public void assertThatFindTransactionsByAccountRefWhenTransactionForAccountNotExistsAreZero() {
    accountService.createAccount(CASH_ACCOUNT_1, Money.toMoney("10.00", "EUR"));
    List<Transaction> transactions = transferService.findTransactionsByAccountRef(CASH_ACCOUNT_1);

    checkThat(transactions.size()).isEqualTo(0);
  }

  @Test
  public void assertThatGetTransactionByRefWhenRefNotExistsReturnsNull() {
    accountService.createAccount(CASH_ACCOUNT_1, Money.toMoney("1000.00", "EUR"));
    accountService.createAccount(REVENUE_ACCOUNT_1, Money.toMoney("0.00", "EUR"));

    transferService.transferFunds(TransferRequest.builder()
        .reference("T2")
        .type("testing")
        .account(CASH_ACCOUNT_1).amount(Money.toMoney("-10.50", "EUR"))
        .account(REVENUE_ACCOUNT_1).amount(Money.toMoney("10.50", "EUR"))
        .build());

    Transaction transaction = transferService.getTransactionByRef("");

    checkThat(transaction).isNull();
  }

  @Test(expected = IllegalArgumentException.class)
  public void assertThatTransferFundsWhenTransferHasOneLegThrowsException() {
    accountService.createAccount(CASH_ACCOUNT_1, Money.toMoney("1000.00", "EUR"));
    accountService.createAccount(REVENUE_ACCOUNT_1, Money.toMoney("0.00", "EUR"));

    transferService.transferFunds(TransferRequest.builder()
        .reference("T1")
        .type("testing")
        .account(CASH_ACCOUNT_1).amount(Money.toMoney("-5.00", "EUR"))
        .build());
  }

  @Test(expected = IllegalArgumentException.class)
  public void assertThatTransferFundsWhenTransferReferenceIsNullThrowsException() {
    accountService.createAccount(CASH_ACCOUNT_1, Money.toMoney("1000.00", "EUR"));
    accountService.createAccount(REVENUE_ACCOUNT_1, Money.toMoney("0.00", "EUR"));

    transferService.transferFunds(TransferRequest.builder()
        .reference(null)
        .type("testing")
        .account(CASH_ACCOUNT_1).amount(Money.toMoney("-5.00", "EUR"))
        .account(REVENUE_ACCOUNT_1).amount(Money.toMoney("5.00", "EUR"))
        .build());
  }

  @Test(expected = IllegalArgumentException.class)
  public void assertThatTransferFundsWhenTransferTypeIsNullThrowsException() {
    accountService.createAccount(CASH_ACCOUNT_1, Money.toMoney("1000.00", "EUR"));
    accountService.createAccount(REVENUE_ACCOUNT_1, Money.toMoney("0.00", "EUR"));

    transferService.transferFunds(TransferRequest.builder()
        .reference("T1")
        .type(null)
        .account(CASH_ACCOUNT_1).amount(Money.toMoney("-5.00", "EUR"))
        .account(REVENUE_ACCOUNT_1).amount(Money.toMoney("5.00", "EUR"))
        .build());
  }

  @Test(expected = TransferValidationException.class)
  public void assertThatTransferFundsWhenTransferLegAccountRefIsNullThrowsException() {
    accountService.createAccount(CASH_ACCOUNT_1, Money.toMoney("1000.00", "EUR"));
    accountService.createAccount(REVENUE_ACCOUNT_1, Money.toMoney("0.00", "EUR"));

    transferService.transferFunds(TransferRequest.builder()
        .reference("T1")
        .type("testing")
        .account(null).amount(Money.toMoney("-5.00", "EUR"))
        .account(REVENUE_ACCOUNT_1).amount(Money.toMoney("5.00", "EUR"))
        .build());
  }

  @Test(expected = TransferValidationException.class)
  public void assertThatTransferFundsWhenTransferLegAmountIsNullThrowsException() {
    accountService.createAccount(CASH_ACCOUNT_1, Money.toMoney("1000.00", "EUR"));
    accountService.createAccount(REVENUE_ACCOUNT_1, Money.toMoney("0.00", "EUR"));

    transferService.transferFunds(TransferRequest.builder()
        .reference("T1")
        .type("testing")
        .account(CASH_ACCOUNT_1).amount(Money.toMoney("-5.00", "EUR"))
        .account(REVENUE_ACCOUNT_1).amount(null)
        .build());
  }

  @Test(expected = UnbalancedLegsException.class)
  public void assertThatTransferFundsWhenTransactionLegsAreUnbalancedThrowsException() {
    accountService.createAccount(CASH_ACCOUNT_1, Money.toMoney("1000.00", "EUR"));
    accountService.createAccount(REVENUE_ACCOUNT_1, Money.toMoney("0.00", "EUR"));

    transferService.transferFunds(TransferRequest.builder()
        .reference("T1")
        .type("testing")
        .account(CASH_ACCOUNT_1).amount(Money.toMoney("-10.00", "EUR"))
        .account(REVENUE_ACCOUNT_1).amount(Money.toMoney("5.00", "EUR"))
        .build());
  }

  @Test(expected = TransferValidationException.class)
  public void assertThatTransferFundsWhenAccountCurrencyNotMatchTransferCurrencyThrowsException() {
    accountService.createAccount(CASH_ACCOUNT_1, Money.toMoney("1000.00", "EUR"));
    accountService.createAccount(REVENUE_ACCOUNT_1, Money.toMoney("0.00", "SEK"));

    transferService.transferFunds(TransferRequest.builder()
        .reference("T1")
        .type("testing")
        .account(CASH_ACCOUNT_1).amount(Money.toMoney("-5.00", "EUR"))
        .account(REVENUE_ACCOUNT_1).amount(Money.toMoney("5.00", "EUR"))
        .build());
  }

  @Test(expected = AccountNotFoundException.class)
  public void assertThatTransferFundsWhenAccountNotFoundThrowsException() {
    accountService.createAccount(CASH_ACCOUNT_1, Money.toMoney("1000.00", "EUR"));
    accountService.createAccount(REVENUE_ACCOUNT_1, Money.toMoney("0.00", "EUR"));

    transferService.transferFunds(TransferRequest.builder()
        .reference("T1")
        .type("testing")
        .account("wrong_account").amount(Money.toMoney("-5.00", "EUR"))
        .account(REVENUE_ACCOUNT_1).amount(Money.toMoney("5.00", "EUR"))
        .build());
  }

  @Test(expected = TransferRequestExistsException.class)
  public void assertThatTransferFundsWhenTransferRequestExistsThrowsException() {
    accountService.createAccount(CASH_ACCOUNT_1, Money.toMoney("1000.00", "EUR"));
    accountService.createAccount(REVENUE_ACCOUNT_1, Money.toMoney("0.00", "EUR"));

    transferService.transferFunds(TransferRequest.builder()
        .reference("T1")
        .type("testing")
        .account(CASH_ACCOUNT_1).amount(Money.toMoney("-5.00", "EUR"))
        .account(REVENUE_ACCOUNT_1).amount(Money.toMoney("5.00", "EUR"))
        .build());

    transferService.transferFunds(TransferRequest.builder()
        .reference("T1")
        .type("testing")
        .account(CASH_ACCOUNT_1).amount(Money.toMoney("-5.00", "EUR"))
        .account(REVENUE_ACCOUNT_1).amount(Money.toMoney("5.00", "EUR"))
        .build());
  }

  @Test(expected = InsufficientFundsException.class)
  public void assertThatTransferFundsWhenAccountIsOverdrawnThrowsException() {
    accountService.createAccount(CASH_ACCOUNT_1, Money.toMoney("10.00", "EUR"));
    accountService.createAccount(REVENUE_ACCOUNT_1, Money.toMoney("0.00", "EUR"));

    transferService.transferFunds(TransferRequest.builder()
        .reference("T1").type("testing")
        .account(CASH_ACCOUNT_1).amount(Money.toMoney("-20.00", "EUR"))
        .account(REVENUE_ACCOUNT_1).amount(Money.toMoney("20.00", "EUR"))
        .build());
  }

  @Test(expected = InfrastructureException.class)
  public void accountAlreadyExistsThrowsException() {
    accountService.createAccount(CASH_ACCOUNT_1, Money.toMoney("1000.00", "EUR"));
    accountService.createAccount(CASH_ACCOUNT_1, Money.toMoney("1000.00", "EUR"));
  }

  @Test(expected = IllegalArgumentException.class)
  public void assertThatCreateAccountWhenMoneyAmountIsNullThrowsException() {
    accountService.createAccount(CASH_ACCOUNT_1, new Money(null, Currency.getInstance("EUR")));
  }

  @Test(expected = IllegalArgumentException.class)
  public void assertThatCreateAccountWhenMoneyCurrencyIsNullThrowsException() {
    accountService.createAccount(CASH_ACCOUNT_1, new Money(new BigDecimal("1000.00"), null));
  }

  @Test(expected = AccountNotFoundException.class)
  public void assertThatGetAccountBalanceWhenNotExistsThrowsException() {
    accountService.getAccountBalance(CASH_ACCOUNT_1);
  }

  @Test(expected = AccountNotFoundException.class)
  public void assertThatGetAccountBalanceWhenAccountRefIsNullThrowsException() {
    accountService.getAccountBalance(null);
  }

  @Test(expected = AccountNotFoundException.class)
  public void assertThatFindTransactionsByAccountRefWhenAccountNotExistsThrowsException() {
    transferService.findTransactionsByAccountRef("");
  }

  @After
  public void tearDown() {
    CoverageTool.testPrivateConstructor(BankContextUtil.class);
  }
}
