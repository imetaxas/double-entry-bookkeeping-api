package com.project.springjta.doubleentry;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.List;
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

        List<Transaction> t2 = transferService.findTransactionsByAccountRef(REVENUE_ACCOUNT_1);
        Assert.assertEquals(2, t2.size());
    }

    @Test
    public void accountBalancesUpdatedAfterMultileggedTransfer() {
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
    public void accountBalancesUpdatedAfterMultileggedMulticurrencyTransfer() {
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

    private static Money toMoney(String amount, String currency) {
        return new Money(new BigDecimal(amount), Currency.getInstance(currency));
    }
}
