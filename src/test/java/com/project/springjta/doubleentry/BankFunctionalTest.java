package com.project.springjta.doubleentry;

import static com.project.springjta.doubleentry.Money.toMoney;
import static com.yanimetaxas.realitycheck.Reality.checkThat;

import com.project.springjta.doubleentry.tools.CoverageTool;
import com.project.springjta.doubleentry.util.BankContextUtil;
import com.project.springjta.doubleentry.validation.TransferValidationException;
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

  private static final String FACTORY_CLASS_NAME = "com.project.springjta.doubleentry.BankFactoryImpl";

  private static final String CASH_ACCOUNT_1 = "cash_1_EUR";
  private static final String REVENUE_ACCOUNT_1 = "revenue_1_EUR";

  private AccountService accountService;
  private TransferService transferService;

  @Before
  public void setupSystemStateBeforeEachTest() throws Exception {
    BankFactory bankFactory = (BankFactory) Class.forName(FACTORY_CLASS_NAME).newInstance();

    accountService = bankFactory.getAccountService();
    transferService = bankFactory.getTransferService();

    bankFactory.setupInitialData();
  }

  @Test
  public void findTransactionsByAccountRef_WhenTransactionForAccountNotExists() {
    accountService.createAccount(CASH_ACCOUNT_1, toMoney("10.00", "EUR"));
    List<Transaction> transactions = transferService.findTransactionsByAccountRef(CASH_ACCOUNT_1);

    checkThat(transactions.size()).isEqualTo(0);
  }

  @Test
  public void getTransactionByRef_WhenRefNotExists() {
    accountService.createAccount(CASH_ACCOUNT_1, toMoney("1000.00", "EUR"));
    accountService.createAccount(REVENUE_ACCOUNT_1, toMoney("0.00", "EUR"));

    transferService.transferFunds(TransferRequest.builder()
        .reference("T2").type("testing")
        .account(CASH_ACCOUNT_1).amount(toMoney("-10.50", "EUR"))
        .account(REVENUE_ACCOUNT_1).amount(toMoney("10.50", "EUR"))
        .build());

    Transaction transaction = transferService.getTransactionByRef("");

    checkThat(transaction).isNull();
  }

  @Test(expected = IllegalStateException.class)
  public void transferFunds_WhenTransferHasOneLeg() {
    accountService.createAccount(CASH_ACCOUNT_1, toMoney("1000.00", "EUR"));
    accountService.createAccount(REVENUE_ACCOUNT_1, toMoney("0.00", "EUR"));

    transferService.transferFunds(TransferRequest.builder()
        .reference("T1")
        .type("testing")
        .account(CASH_ACCOUNT_1).amount(toMoney("-5.00", "EUR"))
        .build());
  }

  @Test(expected = IllegalArgumentException.class)
  public void transferFunds_WhenTransferReferenceIsNull() {
    accountService.createAccount(CASH_ACCOUNT_1, toMoney("1000.00", "EUR"));
    accountService.createAccount(REVENUE_ACCOUNT_1, toMoney("0.00", "EUR"));

    transferService.transferFunds(TransferRequest.builder()
        .reference(null)
        .type("testing")
        .account(CASH_ACCOUNT_1).amount(toMoney("-5.00", "EUR"))
        .account(REVENUE_ACCOUNT_1).amount(toMoney("5.00", "EUR"))
        .build());
  }

  @Test(expected = IllegalArgumentException.class)
  public void transferFunds_WhenTransferTypeIsNull() {
    accountService.createAccount(CASH_ACCOUNT_1, toMoney("1000.00", "EUR"));
    accountService.createAccount(REVENUE_ACCOUNT_1, toMoney("0.00", "EUR"));

    transferService.transferFunds(TransferRequest.builder()
        .reference("T1")
        .type(null)
        .account(CASH_ACCOUNT_1).amount(toMoney("-5.00", "EUR"))
        .account(REVENUE_ACCOUNT_1).amount(toMoney("5.00", "EUR"))
        .build());
  }

  @Test(expected = TransferValidationException.class)
  public void transferFunds_WhenTransferLegAccountRefIsNull() {
    accountService.createAccount(CASH_ACCOUNT_1, toMoney("1000.00", "EUR"));
    accountService.createAccount(REVENUE_ACCOUNT_1, toMoney("0.00", "EUR"));

    transferService.transferFunds(TransferRequest.builder()
        .reference("T1")
        .type("testing")
        .account(null).amount(toMoney("-5.00", "EUR"))
        .account(REVENUE_ACCOUNT_1).amount(toMoney("5.00", "EUR"))
        .build());
  }

  @Test(expected = TransferValidationException.class)
  public void transferFunds_WhenTransferLegAmountIsNull() {
    accountService.createAccount(CASH_ACCOUNT_1, toMoney("1000.00", "EUR"));
    accountService.createAccount(REVENUE_ACCOUNT_1, toMoney("0.00", "EUR"));

    transferService.transferFunds(TransferRequest.builder()
        .reference("T1")
        .type("testing")
        .account(CASH_ACCOUNT_1).amount(toMoney("-5.00", "EUR"))
        .account(REVENUE_ACCOUNT_1).amount(null)
        .build());
  }

  @Test(expected = UnbalancedLegsException.class)
  public void transferFunds_WhenTransactionLegsAreUnbalanced() {
    accountService.createAccount(CASH_ACCOUNT_1, toMoney("1000.00", "EUR"));
    accountService.createAccount(REVENUE_ACCOUNT_1, toMoney("0.00", "EUR"));

    transferService.transferFunds(TransferRequest.builder()
        .reference("T1")
        .type("testing")
        .account(CASH_ACCOUNT_1).amount(toMoney("-10.00", "EUR"))
        .account(REVENUE_ACCOUNT_1).amount(toMoney("5.00", "EUR"))
        .build());
  }

  @Test(expected = TransferValidationException.class)
  public void transferFunds_WhenAccountCurrencyNotMatchTransferCurrency() {
    accountService.createAccount(CASH_ACCOUNT_1, toMoney("1000.00", "EUR"));
    accountService.createAccount(REVENUE_ACCOUNT_1, toMoney("0.00", "SEK"));

    transferService.transferFunds(TransferRequest.builder()
        .reference("T1")
        .type("testing")
        .account(CASH_ACCOUNT_1).amount(toMoney("-5.00", "EUR"))
        .account(REVENUE_ACCOUNT_1).amount(toMoney("5.00", "EUR"))
        .build());
  }

  @Test(expected = AccountNotFoundException.class)
  public void transferFunds_WhenAccountNotFound() {
    accountService.createAccount(CASH_ACCOUNT_1, toMoney("1000.00", "EUR"));
    accountService.createAccount(REVENUE_ACCOUNT_1, toMoney("0.00", "SEK"));

    transferService.transferFunds(TransferRequest.builder()
        .reference("T1")
        .type("testing")
        .account("wrong_account").amount(toMoney("-5.00", "EUR"))
        .account(REVENUE_ACCOUNT_1).amount(toMoney("5.00", "EUR"))
        .build());
  }

  @Test(expected = InsufficientFundsException.class)
  public void transferFunds_WhenAccountIsOverdrawn() {
    accountService.createAccount(CASH_ACCOUNT_1, toMoney("10.00", "EUR"));
    accountService.createAccount(REVENUE_ACCOUNT_1, toMoney("0.00", "EUR"));

    transferService.transferFunds(TransferRequest.builder()
        .reference("T1").type("testing")
        .account(CASH_ACCOUNT_1).amount(toMoney("-20.00", "EUR"))
        .account(REVENUE_ACCOUNT_1).amount(toMoney("20.00", "EUR"))
        .build());
  }

  @Test(expected = InfrastructureException.class)
  public void account_AlreadyExists() {
    accountService.createAccount(CASH_ACCOUNT_1, toMoney("1000.00", "EUR"));
    accountService.createAccount(CASH_ACCOUNT_1, toMoney("1000.00", "EUR"));
  }

  @Test(expected = NullPointerException.class)
  public void createAccount_WhenMoneyAmountIsNull() {
    accountService.createAccount(CASH_ACCOUNT_1, new Money(null, Currency.getInstance("EUR")));
  }

  @Test(expected = NullPointerException.class)
  public void createAccount_WhenMoneyCurrencyIsNull() {
    accountService.createAccount(CASH_ACCOUNT_1, new Money(new BigDecimal("1000.00"), null));
  }

  @Test(expected = AccountNotFoundException.class)
  public void accountBalance_NotExists() {
    accountService.getAccountBalance(CASH_ACCOUNT_1);
  }

  @Test(expected = AccountNotFoundException.class)
  public void accountBalance_isNull() {
    accountService.getAccountBalance(null);
  }

  @Test(expected = AccountNotFoundException.class)
  public void findTransactionsByAccountRef_WhenAccountNotExists() {
    transferService.findTransactionsByAccountRef("");
  }

  @After
  public void tearDown() throws Exception {
    CoverageTool.testPrivateConstructor(BankContextUtil.class);
  }
}
