package com.project.springjta.doubleentry;

import static com.project.springjta.doubleentry.Money.toMoney;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

/**
 * @author yanimetaxas
 * @since 14-Nov-14
 */
public class BankUnitTest {

  private static final String CASH_ACCOUNT = "cash";

  private static final String REVENUE_ACCOUNT = "revenue";

  private AccountService accountServiceMock;

  private TransferService transferServiceMock;

  @Before
  public void setupMockAccounts() throws Exception {
    accountServiceMock = Mockito.mock(AccountService.class);

    Mockito.when(accountServiceMock.getAccountBalance(CASH_ACCOUNT))
        .thenReturn(toMoney("1000.00", "EUR"))
        .thenReturn(toMoney("984.50", "EUR"));
    Mockito.when(accountServiceMock.getAccountBalance(REVENUE_ACCOUNT))
        .thenReturn(toMoney("0.00", "EUR"))
        .thenReturn(toMoney("15.50", "EUR"));

    transferServiceMock = Mockito.mock(TransferService.class);

    Mockito.doNothing().when(transferServiceMock).transferFunds(Mockito.any(TransferRequest.class));

    List<TransactionLeg> t1Legs = new ArrayList<>();
    t1Legs.add(new TransactionLeg(CASH_ACCOUNT, toMoney("-5.00", "EUR")));
    t1Legs.add(new TransactionLeg(REVENUE_ACCOUNT, toMoney("5.00", "EUR")));

    List<TransactionLeg> t2Legs = new ArrayList<>();
    t2Legs.add(new TransactionLeg(REVENUE_ACCOUNT, toMoney("10.50", "EUR")));
    t2Legs.add(new TransactionLeg(CASH_ACCOUNT, toMoney("-10.50", "EUR")));

    List<Transaction> transactions = new ArrayList<>();
    transactions.add(new Transaction("T1", "testing", new Date(), t1Legs));
    transactions.add(new Transaction("T2", "testing", new Date(), t2Legs));

    Mockito.when(transferServiceMock.findTransactionsByAccountRef(CASH_ACCOUNT))
        .thenReturn(transactions);

    Mockito.when(transferServiceMock.findTransactionsByAccountRef(REVENUE_ACCOUNT))
        .thenReturn(transactions);
  }

  @Test
  public void accountBalancesUpdatedAfterTransfer() {
    Assert.assertEquals(toMoney("1000.00", "EUR"),
        accountServiceMock.getAccountBalance(CASH_ACCOUNT));
    Assert.assertEquals(toMoney("0.00", "EUR"),
        accountServiceMock.getAccountBalance(REVENUE_ACCOUNT));

    // Create two distinct transactions with two legs each

    transferServiceMock.transferFunds(TransferRequest.builder()
        .reference("T1").type("testing")
        .account(CASH_ACCOUNT).amount(toMoney("-5.00", "EUR"))
        .account(REVENUE_ACCOUNT).amount(toMoney("5.00", "EUR"))
        .build());

    transferServiceMock.transferFunds(TransferRequest.builder()
        .reference("T2").type("testing")
        .account(REVENUE_ACCOUNT).amount(toMoney("10.50", "EUR"))
        .account(CASH_ACCOUNT).amount(toMoney("-10.50", "EUR"))
        .build());

    Assert.assertEquals(toMoney("984.50", "EUR"),
        accountServiceMock.getAccountBalance(CASH_ACCOUNT));
    Assert.assertEquals(toMoney("15.50", "EUR"),
        accountServiceMock.getAccountBalance(REVENUE_ACCOUNT));

    List<Transaction> t1 = transferServiceMock.findTransactionsByAccountRef(CASH_ACCOUNT);
    Assert.assertEquals(2, t1.size());

    List<Transaction> t2 = transferServiceMock.findTransactionsByAccountRef(REVENUE_ACCOUNT);
    Assert.assertEquals(2, t2.size());
  }
}
