package com.yanimetaxas.bookkeeping.ai;
import com.yanimetaxas.bookkeeping.dao.AccountDao;
import com.yanimetaxas.bookkeeping.dao.TransactionDao;
import com.yanimetaxas.bookkeeping.exception.AccountNotFoundException;
import com.yanimetaxas.bookkeeping.exception.InsufficientFundsException;
import com.yanimetaxas.bookkeeping.model.Money;
import com.yanimetaxas.bookkeeping.model.Transaction;
import com.yanimetaxas.bookkeeping.model.TransactionLeg;
import com.yanimetaxas.bookkeeping.model.TransferRequest;
import com.yanimetaxas.bookkeeping.service.TransferServiceImpl;
import com.yanimetaxas.bookkeeping.validation.TransferValidator;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

public class TransferServiceImplAiTest {

    @Mock
    private TransferValidator validator;

    @Mock
    private AccountDao accountDao;

    @Mock
    private TransactionDao transactionDao;

    private TransferServiceImpl service;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        service = new TransferServiceImpl();
        service.setValidator(validator);
        service.setAccountDao(accountDao);
        service.setTransactionDao(transactionDao);
    }

    private TransferRequest createTransferRequest() {
        return TransferRequest.builder()
                .reference("TX-001")
                .type("TRANSFER")
                .account("ACC-001")
                .debit("100.00", "USD")
                .account("ACC-002")
                .credit("100.00", "USD")
                .build();
    }

    @Test
    public void transferFunds_validRequest_updatesBalancesValidatesAndStoresTransaction() {
        TransferRequest request = createTransferRequest();

        service.transferFunds(request);

        verify(validator).validateTransferRequest(request);
        verify(validator).isTransactionBalanced(request.getLegs());
        verify(validator).transferRequestExists("TX-001");
        verify(validator).currenciesMatch(request.getLegs());

        verify(accountDao, times(2)).updateBalance(any(TransactionLeg.class));
        verify(validator).validBalance(request.getLegs());

        ArgumentCaptor<Transaction> captor =
                ArgumentCaptor.forClass(Transaction.class);
        verify(transactionDao).storeTransaction(captor.capture());

        Transaction stored = captor.getValue();
        assertEquals("TX-001", stored.getTransactionRef());
        assertEquals("TRANSFER", stored.getTransactionType());
        assertNotNull(stored.getTransactionDate());
        assertEquals(2, stored.getLegs().size());
        assertEquals(request.getLegs(), stored.getLegs());
    }

    @Test
    public void transferFunds_updatesBalancesInRequestOrder() {
        TransferRequest request = createTransferRequest();

        service.transferFunds(request);

        ArgumentCaptor<TransactionLeg> captor =
                ArgumentCaptor.forClass(TransactionLeg.class);
        verify(accountDao, times(2)).updateBalance(captor.capture());

        List<TransactionLeg> updatedLegs = captor.getAllValues();
        assertSame(request.getLegs().get(0), updatedLegs.get(0));
        assertSame(request.getLegs().get(1), updatedLegs.get(1));
    }

    @Test
    public void transferFunds_singleValidationFailureDoesNotUpdateBalances() {
        TransferRequest request = createTransferRequest();
        RuntimeException failure = new IllegalArgumentException("Invalid request");

        doThrow(failure).when(validator).validateTransferRequest(request);

        try {
            service.transferFunds(request);
            fail("Expected validation exception");
        } catch (IllegalArgumentException expected) {
            assertSame(failure, expected);
        }

        verify(validator).validateTransferRequest(request);
        verify(validator, never()).isTransactionBalanced(any(Iterable.class));
        verifyZeroInteractions(accountDao);
        verifyZeroInteractions(transactionDao);
    }

    @Test
    public void transferFunds_unbalancedRequestFailureDoesNotUpdateBalances() {
        TransferRequest request = createTransferRequest();
        RuntimeException failure = new IllegalStateException("Unbalanced legs");

        doThrow(failure).when(validator).isTransactionBalanced(request.getLegs());

        try {
            service.transferFunds(request);
            fail("Expected validation exception");
        } catch (IllegalStateException expected) {
            assertSame(failure, expected);
        }

        verify(validator).validateTransferRequest(request);
        verify(validator).isTransactionBalanced(request.getLegs());
        verify(validator, never()).transferRequestExists(anyString());
        verifyZeroInteractions(accountDao);
        verifyZeroInteractions(transactionDao);
    }

    @Test
    public void transferFunds_existingTransactionFailureDoesNotUpdateBalances() {
        TransferRequest request = createTransferRequest();
        RuntimeException failure = new IllegalStateException("Duplicate transaction");

        doThrow(failure).when(validator).transferRequestExists("TX-001");

        try {
            service.transferFunds(request);
            fail("Expected validation exception");
        } catch (IllegalStateException expected) {
            assertSame(failure, expected);
        }

        verify(validator).transferRequestExists("TX-001");
        verify(validator, never()).currenciesMatch(any(Iterable.class));
        verifyZeroInteractions(accountDao);
        verifyZeroInteractions(transactionDao);
    }

    @Test
    public void transferFunds_currencyMismatchFailureDoesNotUpdateBalances() {
        TransferRequest request = createTransferRequest();
        RuntimeException failure = new IllegalArgumentException("Currency mismatch");

        doThrow(failure).when(validator).currenciesMatch(request.getLegs());

        try {
            service.transferFunds(request);
            fail("Expected validation exception");
        } catch (IllegalArgumentException expected) {
            assertSame(failure, expected);
        }

        verify(validator).currenciesMatch(request.getLegs());
        verifyZeroInteractions(accountDao);
        verifyZeroInteractions(transactionDao);
    }

    @Test
    public void transferFunds_insufficientFundsDoesNotStoreTransaction() {
        TransferRequest request = createTransferRequest();
        InsufficientFundsException failure =
                new InsufficientFundsException("ACC-001");

        doThrow(failure).when(validator).validBalance(request.getLegs());

        try {
            service.transferFunds(request);
            fail("Expected insufficient funds exception");
        } catch (InsufficientFundsException expected) {
            assertSame(failure, expected);
        }

        verify(accountDao, times(2)).updateBalance(any(TransactionLeg.class));
        verify(validator).validBalance(request.getLegs());
        verify(transactionDao, never()).storeTransaction(any(Transaction.class));
    }

    @Test
    public void transferFunds_balanceUpdateFailureStopsProcessing() {
        TransferRequest request = createTransferRequest();
        RuntimeException failure = new IllegalStateException("Database failure");

        doThrow(failure).when(accountDao)
                .updateBalance(request.getLegs().get(0));

        try {
            service.transferFunds(request);
            fail("Expected update failure");
        } catch (IllegalStateException expected) {
            assertSame(failure, expected);
        }

        verify(accountDao).updateBalance(request.getLegs().get(0));
        verify(accountDao, never()).updateBalance(request.getLegs().get(1));
        verify(validator, never()).validBalance(any(Iterable.class));
        verify(transactionDao, never()).storeTransaction(any(Transaction.class));
    }

    @Test
    public void transferFunds_transactionStorageFailurePropagates() {
        TransferRequest request = createTransferRequest();
        RuntimeException failure = new IllegalStateException("Storage failure");

        doThrow(failure).when(transactionDao).storeTransaction(any(Transaction.class));

        try {
            service.transferFunds(request);
            fail("Expected storage failure");
        } catch (IllegalStateException expected) {
            assertSame(failure, expected);
        }

        verify(validator).validBalance(request.getLegs());
        verify(transactionDao).storeTransaction(any(Transaction.class));
    }

    @Test
    public void findTransactionsByAccountRef_accountDoesNotExistThrowsException() {
        when(accountDao.accountExists("MISSING")).thenReturn(false);

        try {
            service.findTransactionsByAccountRef("MISSING");
            fail("Expected AccountNotFoundException");
        } catch (AccountNotFoundException expected) {
            assertTrue(expected.getMessage().contains("MISSING"));
        }

        verify(accountDao).accountExists("MISSING");
        verifyZeroInteractions(transactionDao);
    }

    @Test
    public void findTransactionsByAccountRef_noTransactionReferencesReturnsEmptyList()
            throws AccountNotFoundException {
        when(accountDao.accountExists("ACC-001")).thenReturn(true);
        when(transactionDao.getTransactionRefsForAccount("ACC-001"))
                .thenReturn(Collections.<String>emptySet());

        List<Transaction> result =
                service.findTransactionsByAccountRef("ACC-001");

        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(transactionDao).getTransactionRefsForAccount("ACC-001");
        verify(transactionDao, never()).getTransactions(anyList());
    }

    @Test
    public void findTransactionsByAccountRef_existingReferencesReturnsTransactions()
            throws AccountNotFoundException {
        Transaction first = new Transaction(
                "TX-001", "TRANSFER", new Date(),
                Collections.<TransactionLeg>emptyList());
        Transaction second = new Transaction(
                "TX-002", "PAYMENT", new Date(),
                Collections.<TransactionLeg>emptyList());

        Set<String> refs = new HashSet<String>(
                Arrays.asList("TX-001", "TX-002"));
        List<Transaction> expected = Arrays.asList(first, second);

        when(accountDao.accountExists("ACC-001")).thenReturn(true);
        when(transactionDao.getTransactionRefsForAccount("ACC-001"))
                .thenReturn(refs);
        when(transactionDao.getTransactions(anyList())).thenReturn(expected);

        List<Transaction> result =
                service.findTransactionsByAccountRef("ACC-001");

        assertSame(expected, result);
        verify(accountDao).accountExists("ACC-001");

        ArgumentCaptor<List> captor = ArgumentCaptor.forClass(List.class);
        verify(transactionDao).getTransactions(captor.capture());

        List<String> capturedRefs = captor.getValue();
        assertEquals(2, capturedRefs.size());
        assertTrue(capturedRefs.contains("TX-001"));
        assertTrue(capturedRefs.contains("TX-002"));
    }

    @Test
    public void findTransactionsByAccountRef_nullAccountReferencePassesToDao()
            throws AccountNotFoundException {
        when(accountDao.accountExists(null)).thenReturn(false);

        try {
            service.findTransactionsByAccountRef(null);
            fail("Expected AccountNotFoundException");
        } catch (AccountNotFoundException expected) {
            assertTrue(expected.getMessage().contains("null"));
        }

        verify(accountDao).accountExists(null);
        verifyZeroInteractions(transactionDao);
    }

    @Test
    public void findTransactionsByAccountRef_accountDaoFailurePropagates()
            throws AccountNotFoundException {
        RuntimeException failure = new IllegalStateException("DAO failure");
        when(accountDao.accountExists("ACC-001")).thenThrow(failure);

        try {
            service.findTransactionsByAccountRef("ACC-001");
            fail("Expected DAO failure");
        } catch (IllegalStateException expected) {
            assertSame(failure, expected);
        }

        verifyZeroInteractions(transactionDao);
    }

    @Test
    public void findTransactionsByAccountRef_referenceLookupFailurePropagates()
            throws AccountNotFoundException {
        RuntimeException failure = new IllegalStateException("Reference lookup failed");
        when(accountDao.accountExists("ACC-001")).thenReturn(true);
        when(transactionDao.getTransactionRefsForAccount("ACC-001"))
                .thenThrow(failure);

        try {
            service.findTransactionsByAccountRef("ACC-001");
            fail("Expected lookup failure");
        } catch (IllegalStateException expected) {
            assertSame(failure, expected);
        }

        verify(transactionDao).getTransactionRefsForAccount("ACC-001");
        verify(transactionDao, never()).getTransactions(anyList());
    }

    @Test
    public void findTransactionsByAccountRef_transactionLookupFailurePropagates()
            throws AccountNotFoundException {
        RuntimeException failure = new IllegalStateException("Transaction lookup failed");
        Set<String> refs = new HashSet<String>(Collections.singletonList("TX-001"));

        when(accountDao.accountExists("ACC-001")).thenReturn(true);
        when(transactionDao.getTransactionRefsForAccount("ACC-001")).thenReturn(refs);
        when(transactionDao.getTransactions(anyList())).thenThrow(failure);

        try {
            service.findTransactionsByAccountRef("ACC-001");
            fail("Expected transaction lookup failure");
        } catch (IllegalStateException expected) {
            assertSame(failure, expected);
        }

        verify(transactionDao).getTransactions(anyList());
    }

    @Test
    public void getTransactionByRef_returnsTransactionFromDao() {
        Transaction expected = new Transaction(
                "TX-001", "TRANSFER", new Date(),
                Collections.<TransactionLeg>emptyList());
        when(transactionDao.getTransactionByRef("TX-001")).thenReturn(expected);

        Transaction actual = service.getTransactionByRef("TX-001");

        assertSame(expected, actual);
        verify(transactionDao).getTransactionByRef("TX-001");
    }

    @Test
    public void getTransactionByRef_returnsNullWhenTransactionDoesNotExist() {
        when(transactionDao.getTransactionByRef("MISSING")).thenReturn(null);

        assertNull(service.getTransactionByRef("MISSING"));

        verify(transactionDao).getTransactionByRef("MISSING");
    }

    @Test
    public void getTransactionByRef_nullReferenceIsPassedToDao() {
        when(transactionDao.getTransactionByRef(null)).thenReturn(null);

        assertNull(service.getTransactionByRef(null));

        verify(transactionDao).getTransactionByRef(null);
    }

    @Test
    public void getTransactionByRef_daoFailurePropagates() {
        RuntimeException failure = new IllegalStateException("DAO failure");
        when(transactionDao.getTransactionByRef("TX-001")).thenThrow(failure);

        try {
            service.getTransactionByRef("TX-001");
            fail("Expected DAO failure");
        } catch (IllegalStateException expected) {
            assertSame(failure, expected);
        }
    }

    @Test
    public void transferFunds_nullRequestPropagatesValidatorFailure() {
        RuntimeException failure = new IllegalArgumentException("Request is null");
        doThrow(failure).when(validator).validateTransferRequest(null);

        try {
            service.transferFunds(null);
            fail("Expected validation failure");
        } catch (IllegalArgumentException expected) {
            assertSame(failure, expected);
        }

        verify(validator).validateTransferRequest(null);
        verify(validator, never()).isTransactionBalanced(any(Iterable.class));
        verifyZeroInteractions(accountDao);
        verifyZeroInteractions(transactionDao);
    }
}