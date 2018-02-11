package com.project.springjta.doubleentry;

import static com.project.springjta.doubleentry.Money.toMoney;

import com.project.springjta.doubleentry.tools.CoverageTool;
import com.project.springjta.doubleentry.util.BankContextUtil;
import com.project.springjta.doubleentry.validation.TransferValidationException;
import java.math.BigDecimal;
import java.util.Currency;
import java.util.List;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * @author yanimetaxas
 * @since 14-Nov-14
 */
public class BankFunctionalTest {
    // There are no semantics in these constants, its just names

    private static final String CASH_ACCOUNT_1 = "cash_1_EUR";

    private static final String CASH_ACCOUNT_2 = "cash_2_SEK";

    private static final String REVENUE_ACCOUNT_1 = "revenue_1_EUR";

    private static final String REVENUE_ACCOUNT_2 = "revenue_2_SEK";

    private AccountService accountService;

    private TransferService transferService;

    @Before
    public void setupSystemStateBeforeEachTest() throws Exception {
        BankFactory bankFactory = (BankFactory) Class.forName(
                "com.project.springjta.doubleentry.BankFactoryImpl").newInstance();

        accountService = bankFactory.getAccountService();
        transferService = bankFactory.getTransferService();

        bankFactory.setupInitialData();
    }

    @Test
    public void accountBalancesUpdatedAfterTransfer() {
        accountService.createAccount(CASH_ACCOUNT_1, toMoney("1000.00", "EUR"));
        accountService.createAccount(REVENUE_ACCOUNT_1, toMoney("0.00", "EUR"));

        Assert.assertEquals(toMoney("1000.00", "EUR"), accountService.getAccountBalance(CASH_ACCOUNT_1));
        Assert.assertEquals(toMoney("0.00", "EUR"), accountService.getAccountBalance(REVENUE_ACCOUNT_1));

        transferService.transferFunds(TransferRequest.builder()
                .reference("T1").type("testing")
                .account(CASH_ACCOUNT_1).amount(toMoney("-5.00", "EUR"))
                .account(REVENUE_ACCOUNT_1).amount(toMoney("5.00", "EUR"))
                .build());

        transferService.transferFunds(TransferRequest.builder()
                .reference("T2").type("testing")
                .account(CASH_ACCOUNT_1).amount(toMoney("-10.50", "EUR"))
                .account(REVENUE_ACCOUNT_1).amount(toMoney("10.50", "EUR"))
                .build());

        Assert.assertEquals(toMoney("984.50", "EUR"), accountService.getAccountBalance(CASH_ACCOUNT_1));
        Assert.assertEquals(toMoney("15.50", "EUR"), accountService.getAccountBalance(REVENUE_ACCOUNT_1));

        List<Transaction> t1 = transferService.findTransactionsByAccountRef(CASH_ACCOUNT_1);
        Assert.assertEquals(2, t1.size());

        Transaction transaction = transferService.getTransactionByRef("T1");
        Assert.assertEquals("T1", transaction.getTransactionRef());

        transaction = transferService.getTransactionByRef("T2");
        Assert.assertEquals("T2", transaction.getTransactionRef());

        List<Transaction> t2 = transferService.findTransactionsByAccountRef(REVENUE_ACCOUNT_1);
        Assert.assertEquals(2, t2.size());
    }

    @Test
    public void accountBalancesUpdatedAfterMultiLeggedTransfer() {
        accountService.createAccount(CASH_ACCOUNT_1, toMoney("1000.00", "EUR"));
        accountService.createAccount(REVENUE_ACCOUNT_1, toMoney("0.00", "EUR"));

        Assert.assertEquals(toMoney("1000.00", "EUR"), accountService.getAccountBalance(CASH_ACCOUNT_1));
        Assert.assertEquals(toMoney("0.00", "EUR"), accountService.getAccountBalance(REVENUE_ACCOUNT_1));

        transferService.transferFunds(TransferRequest.builder()
                .reference("T1").type("testing")
                .account(CASH_ACCOUNT_1).amount(toMoney("-5.00", "EUR"))
                .account(REVENUE_ACCOUNT_1).amount(toMoney("5.00", "EUR"))
                .account(CASH_ACCOUNT_1).amount(toMoney("-10.50", "EUR"))
                .account(REVENUE_ACCOUNT_1).amount(toMoney("10.50", "EUR"))
                .account(CASH_ACCOUNT_1).amount(toMoney("-2.00", "EUR"))
                .account(REVENUE_ACCOUNT_1).amount(toMoney("1.00", "EUR"))
                .account(REVENUE_ACCOUNT_1).amount(toMoney("1.00", "EUR"))
                .build());

        Assert.assertEquals(toMoney("982.50", "EUR"), accountService.getAccountBalance(CASH_ACCOUNT_1));
        Assert.assertEquals(toMoney("17.50", "EUR"), accountService.getAccountBalance(REVENUE_ACCOUNT_1));
    }

    @Test
    public void accountBalancesUpdatedAfterMultiLeggedMultiCurrencyTransfer() {
        accountService.createAccount(CASH_ACCOUNT_1, toMoney("1000.00", "EUR"));
        accountService.createAccount(REVENUE_ACCOUNT_1, toMoney("0.00", "EUR"));
        accountService.createAccount(CASH_ACCOUNT_2, toMoney("1000.00", "SEK"));
        accountService.createAccount(REVENUE_ACCOUNT_2, toMoney("0.00", "SEK"));

        Assert.assertEquals(toMoney("1000.00", "SEK"), accountService.getAccountBalance(CASH_ACCOUNT_2));
        Assert.assertEquals(toMoney("0.00", "SEK"), accountService.getAccountBalance(REVENUE_ACCOUNT_2));

        transferService.transferFunds(TransferRequest.builder()
                .reference("T1").type("testing")
                .account(CASH_ACCOUNT_1).amount(toMoney("-5.00", "EUR"))
                .account(REVENUE_ACCOUNT_1).amount(toMoney("5.00", "EUR"))
                .account(CASH_ACCOUNT_2).amount(toMoney("-10.50", "SEK"))
                .account(REVENUE_ACCOUNT_2).amount(toMoney("10.50", "SEK"))
                .build());

        Assert.assertEquals(toMoney("995.00", "EUR"), accountService.getAccountBalance(CASH_ACCOUNT_1));
        Assert.assertEquals(toMoney("5.00", "EUR"), accountService.getAccountBalance(REVENUE_ACCOUNT_1));
        Assert.assertEquals(toMoney("989.50", "SEK"), accountService.getAccountBalance(CASH_ACCOUNT_2));
        Assert.assertEquals(toMoney("10.50", "SEK"), accountService.getAccountBalance(REVENUE_ACCOUNT_2));
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

    @After
    public void tearDown() throws Exception {
        CoverageTool.testPrivateConstructor(BankContextUtil.class);
    }
}
