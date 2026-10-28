package com.yanimetaxas.bookkeeping.manual;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.yanimetaxas.bookkeeping.dao.AccountDao;
import com.yanimetaxas.bookkeeping.dao.TransactionDao;
import com.yanimetaxas.bookkeeping.exception.AccountNotFoundException;
import com.yanimetaxas.bookkeeping.exception.InsufficientFundsException;
import com.yanimetaxas.bookkeeping.model.Transaction;
import com.yanimetaxas.bookkeeping.model.TransactionLeg;
import com.yanimetaxas.bookkeeping.model.TransferRequest;
import com.yanimetaxas.bookkeeping.service.TransferServiceImpl;
import com.yanimetaxas.bookkeeping.validation.TransferValidator;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;

/** Interaction (state/protocol) tests. Kills mutants S4, S5, S6 (void-call removals) and S9. */
public class TransferServiceInteractionTest {

  private TransferServiceImpl service;
  private AccountDao accountDao;
  private TransactionDao transactionDao;
  private TransferValidator validator;

  @Before
  public void setUp() {
    accountDao = mock(AccountDao.class);
    transactionDao = mock(TransactionDao.class);
    validator = mock(TransferValidator.class);
    service = new TransferServiceImpl();
    service.setAccountDao(accountDao);
    service.setTransactionDao(transactionDao);
    service.setValidator(validator);
  }

  private static TransferRequest request() {
    return TransferRequest.builder().reference("T1").type("t")
        .account("A").debit("5.00", "EUR")
        .account("B").credit("5.00", "EUR").build();
  }

  @Test
  public void transferUpdatesEveryLegThenChecksBalanceThenStores() {
    TransferRequest req = request();
    service.transferFunds(req);

    InOrder order = inOrder(accountDao, validator, transactionDao);
    for (TransactionLeg leg : req.getLegs()) {
      order.verify(accountDao).updateBalance(leg);          // S5
    }
    order.verify(validator).validBalance(req.getLegs());     // S4
    order.verify(transactionDao).storeTransaction(any(Transaction.class)); // S6
  }

  @Test
  public void insufficientFundsPreventsStoringTheTransaction() {
    TransferRequest req = request();
    doThrow(new InsufficientFundsException("A")).when(validator).validBalance(req.getLegs());
    try {
      service.transferFunds(req);
      org.junit.Assert.fail("expected InsufficientFundsException");
    } catch (InsufficientFundsException expected) {
      verify(transactionDao, never()).storeTransaction(any(Transaction.class));
    }
  }

  @Test(expected = AccountNotFoundException.class)
  public void findTransactionsForUnknownAccountFails() {
    when(accountDao.accountExists("X")).thenReturn(false);
    service.findTransactionsByAccountRef("X");
  }

  @Test
  public void accountWithoutTransactionsReturnsEmptyListWithoutQueryingDao() {
    when(accountDao.accountExists("A")).thenReturn(true);
    when(transactionDao.getTransactionRefsForAccount("A")).thenReturn(Sets.<String>newHashSet());
    List<Transaction> result = service.findTransactionsByAccountRef("A");
    assertTrue(result.isEmpty());
    verify(transactionDao, never()).getTransactions(anyListOf(String.class)); // S9
  }

  @Test
  public void accountWithTransactionsReturnsDaoResult() {
    Transaction t = new Transaction("T1", "t", new java.util.Date(), request().getLegs());
    when(accountDao.accountExists("A")).thenReturn(true);
    when(transactionDao.getTransactionRefsForAccount("A")).thenReturn(Sets.newHashSet("T1"));
    when(transactionDao.getTransactions(Lists.newArrayList("T1"))).thenReturn(Lists.newArrayList(t));
    assertEquals(1, service.findTransactionsByAccountRef("A").size());
  }
}
