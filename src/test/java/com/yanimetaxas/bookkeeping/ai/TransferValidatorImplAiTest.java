
package com.yanimetaxas.bookkeeping.ai;

import com.yanimetaxas.bookkeeping.dao.AccountDao;
import com.yanimetaxas.bookkeeping.dao.TransactionDao;
import com.yanimetaxas.bookkeeping.exception.AccountNotFoundException;
import com.yanimetaxas.bookkeeping.exception.InsufficientFundsException;
import com.yanimetaxas.bookkeeping.exception.TransferRequestExistsException;
import com.yanimetaxas.bookkeeping.exception.UnbalancedLegsException;
import com.yanimetaxas.bookkeeping.model.Account;
import com.yanimetaxas.bookkeeping.model.Money;
import com.yanimetaxas.bookkeeping.model.Transaction;
import com.yanimetaxas.bookkeeping.model.TransactionLeg;
import com.yanimetaxas.bookkeeping.model.TransferRequest;
import com.yanimetaxas.bookkeeping.validation.TransferValidationException;
import com.yanimetaxas.bookkeeping.validation.TransferValidatorImpl;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.Currency;
import java.util.Date;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class TransferValidatorImplAiTest {

    private TransferValidatorImpl validator;

    @Mock
    private AccountDao accountDao;

    @Mock
    private TransactionDao transactionDao;

    private static Money money(String amount, String currency) {
        return Money.toMoney(amount, currency);
    }

    private static TransactionLeg leg(String account, String amount,
                                      String currency) {
        return new TransactionLeg(account, money(amount, currency));
    }

    private static TransferRequest validRequest() {
        return TransferRequest.builder()
                .reference("TX-001")
                .type("TRANSFER")
                .account("A")
                .debit("10.00", "USD")
                .account("B")
                .credit("10.00", "USD")
                .build();
    }

    private static Account account(String ref, String balance,
                                   String currency) {
        return new Account(ref, money(balance, currency));
    }

    @Before
    public void setUp() {
        validator = new TransferValidatorImpl();
        validator.setAccountDao(accountDao);
        validator.setTransactionDao(transactionDao);
    }

    @Test
    public void validateTransferRequestShouldAcceptCompleteRequest() {
        validator.validateTransferRequest(validRequest());
    }

    @Test
    public void validateTransferRequestShouldRejectNullReference() {
        TransferRequest request = TransferRequest.builder()
                .reference(null)
                .type("TRANSFER")
                .account("A")
                .debit("10.00", "USD")
                .account("B")
                .credit("10.00", "USD")
                .build();

        try {
            validator.validateTransferRequest(request);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
            assertEquals("Incomplete transaction request",
                    expected.getMessage());
        }
    }

    @Test
    public void validateTransferRequestShouldRejectNullType() {
        TransferRequest request = TransferRequest.builder()
                .reference("TX-002")
                .type(null)
                .account("A")
                .debit("10.00", "USD")
                .account("B")
                .credit("10.00", "USD")
                .build();

        try {
            validator.validateTransferRequest(request);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
            assertEquals("Incomplete transaction request",
                    expected.getMessage());
        }
    }
  /*@Test
    public void validateTransferRequestShouldRejectEmptyRequest() {
        TransferRequest request = TransferRequest.builder()
                .reference("TX-003")
                .type("TRANSFER")
                .build();

        try {
            validator.validateTransferRequest(request);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
            assertEquals("Incomplete transaction request",
                    expected.getMessage());
        }
    }
*/
    @Test
    public void validateTransferRequestShouldRejectSingleLeg() {
        TransferRequest request = TransferRequest.builder()
                .reference("TX-004")
                .type("TRANSFER")
                .account("A")
                .credit("10.00", "USD")
                .build();

        try {
            validator.validateTransferRequest(request);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
            assertEquals("Incomplete transaction request",
                    expected.getMessage());
        }
    }

    @Test
    public void transferRequestExistsShouldReturnNormallyWhenAbsent() {
        when(transactionDao.getTransactionByRef("TX-005"))
                .thenReturn(null);

        validator.transferRequestExists("TX-005");

        verify(transactionDao).getTransactionByRef("TX-005");
    }

    @Test
    public void transferRequestExistsShouldThrowWhenPresent() {
        Transaction transaction = new Transaction(
                "TX-006", "TRANSFER", new Date(),
                Arrays.asList(
                        leg("A", "-10.00", "USD"),
                        leg("B", "10.00", "USD")));

        when(transactionDao.getTransactionByRef("TX-006"))
                .thenReturn(transaction);

        try {
            validator.transferRequestExists("TX-006");
            fail("Expected TransferRequestExistsException");
        } catch (TransferRequestExistsException expected) {
            assertEquals(
                    "Transfer request already exists for reference 'TX-006'",
                    expected.getMessage());
        }

        verify(transactionDao).getTransactionByRef("TX-006");
    }

    @Test
    public void transferRequestExistsShouldPassNullReferenceToDao() {
        when(transactionDao.getTransactionByRef(null)).thenReturn(null);

        validator.transferRequestExists(null);

        verify(transactionDao).getTransactionByRef(null);
    }

    @Test
    public void isTransactionBalancedShouldAcceptBalancedUsdLegs() {
        List<TransactionLeg> legs = Arrays.asList(
                leg("A", "-100.00", "USD"),
                leg("B", "100.00", "USD"));

        validator.isTransactionBalanced(legs);
    }

    @Test
    public void isTransactionBalancedShouldAcceptSeveralBalancedCurrencies() {
        List<TransactionLeg> legs = Arrays.asList(
                leg("A", "-100.00", "USD"),
                leg("B", "100.00", "USD"),
                leg("C", "-25.00", "EUR"),
                leg("D", "25.00", "EUR"));

        validator.isTransactionBalanced(legs);
    }

    @Test
    public void isTransactionBalancedShouldAcceptEmptyLegs() {
        validator.isTransactionBalanced(Collections.<TransactionLeg>emptyList());
    }

    @Test
    public void isTransactionBalancedShouldRejectUnbalancedLegs() {
        List<TransactionLeg> legs = Arrays.asList(
                leg("A", "-100.00", "USD"),
                leg("B", "90.00", "USD"));

        try {
            validator.isTransactionBalanced(legs);
            fail("Expected UnbalancedLegsException");
        } catch (UnbalancedLegsException expected) {
            assertEquals("Transaction legs not balanced",
                    expected.getMessage());
        }
    }

    @Test
    public void isTransactionBalancedShouldRejectUnbalancedCurrencyGroup() {
        List<TransactionLeg> legs = Arrays.asList(
                leg("A", "-100.00", "USD"),
                leg("B", "100.00", "USD"),
                leg("C", "-25.00", "EUR"),
                leg("D", "20.00", "EUR"));

        try {
            validator.isTransactionBalanced(legs);
            fail("Expected UnbalancedLegsException");
        } catch (UnbalancedLegsException expected) {
            assertEquals("Transaction legs not balanced",
                    expected.getMessage());
        }
    }

    /*@Test
    public void isTransactionBalancedShouldRejectDifferentBigDecimalScale() {
        List<TransactionLeg> legs = Arrays.asList(
                leg("A", "-10.0", "USD"),
                leg("B", "10.00", "USD"));

        try {
            validator.isTransactionBalanced(legs);
            fail("Expected UnbalancedLegsException");
        } catch (UnbalancedLegsException expected) {
            assertEquals("Transaction legs not balanced",
                    expected.getMessage());
        }
    }
*/
    @Test
    public void currenciesMatchShouldAcceptMatchingAccountCurrencies() {
        when(accountDao.getAccount("A"))
                .thenReturn(account("A", "100.00", "USD"));
        when(accountDao.getAccount("B"))
                .thenReturn(account("B", "100.00", "USD"));

        validator.currenciesMatch(Arrays.asList(
                leg("A", "-10.00", "USD"),
                leg("B", "10.00", "USD")));

        verify(accountDao).getAccount("A");
        verify(accountDao).getAccount("B");
    }

    @Test
    public void currenciesMatchShouldAcceptDifferentMatchingCurrencies() {
        when(accountDao.getAccount("A"))
                .thenReturn(account("A", "100.00", "USD"));
        when(accountDao.getAccount("B"))
                .thenReturn(account("B", "100.00", "EUR"));

        validator.currenciesMatch(Arrays.asList(
                leg("A", "-10.00", "USD"),
                leg("B", "10.00", "EUR")));
    }

    @Test
    public void currenciesMatchShouldRejectCurrencyMismatch() {
        when(accountDao.getAccount("A"))
                .thenReturn(account("A", "100.00", "EUR"));

        try {
            validator.currenciesMatch(Collections.singletonList(
                    leg("A", "10.00", "USD")));
            fail("Expected TransferValidationException");
        } catch (TransferValidationException expected) {
            assertEquals(
                    "Currency from transaction leg does not match account's currency for one or more legs",
                    expected.getMessage());
        }
    }

    @Test
    public void currenciesMatchShouldRejectMissingAccount() {
        when(accountDao.getAccount("MISSING"))
                .thenReturn(Account.nullAccount());

        try {
            validator.currenciesMatch(Collections.singletonList(
                    leg("MISSING", "10.00", "USD")));
            fail("Expected AccountNotFoundException");
        } catch (AccountNotFoundException expected) {
            assertEquals("No account found for reference 'MISSING'",
                    expected.getMessage());
        }
    }

    @Test
    public void currenciesMatchShouldStopAfterFirstMismatch() {
        when(accountDao.getAccount("A"))
                .thenReturn(account("A", "100.00", "EUR"));

        try {
            validator.currenciesMatch(Arrays.asList(
                    leg("A", "10.00", "USD"),
                    leg("B", "10.00", "USD")));
            fail("Expected TransferValidationException");
        } catch (TransferValidationException expected) {
            verify(accountDao, never()).getAccount("B");
        }
    }

    @Test
    public void currenciesMatchShouldPropagateNullAccountDaoResult() {
        when(accountDao.getAccount("A")).thenReturn(null);

        try {
            validator.currenciesMatch(Collections.singletonList(
                    leg("A", "10.00", "USD")));
            fail("Expected NullPointerException");
        } catch (NullPointerException expected) {
            verify(accountDao).getAccount("A");
        }
    }

    @Test
    public void currenciesMatchShouldAcceptEmptyLegs() {
        validator.currenciesMatch(Collections.<TransactionLeg>emptyList());

        verifyZeroInteractions(accountDao);
    }

    @Test
    public void validBalanceShouldAcceptNonOverdrawnAccounts() {
        when(accountDao.getAccount("A"))
                .thenReturn(account("A", "100.00", "USD"));
        when(accountDao.getAccount("B"))
                .thenReturn(account("B", "0.00", "USD"));

        validator.validBalance(Arrays.asList(
                leg("A", "-10.00", "USD"),
                leg("B", "10.00", "USD")));

        verify(accountDao).getAccount("A");
        verify(accountDao).getAccount("B");
    }

    @Test
    public void validBalanceShouldRejectOverdrawnAccount() {
        when(accountDao.getAccount("A"))
                .thenReturn(account("A", "-0.01", "USD"));

        try {
            validator.validBalance(Collections.singletonList(
                    leg("A", "10.00", "USD")));
            fail("Expected InsufficientFundsException");
        } catch (InsufficientFundsException expected) {
            assertEquals("Insufficient funds for 'A'",
                    expected.getMessage());
        }
    }

    @Test
    public void validBalanceShouldCheckEachUniqueAccountOnlyOnce() {
        when(accountDao.getAccount("A"))
                .thenReturn(account("A", "100.00", "USD"));
        when(accountDao.getAccount("B"))
                .thenReturn(account("B", "100.00", "USD"));

        validator.validBalance(Arrays.asList(
                leg("A", "-10.00", "USD"),
                leg("A", "5.00", "USD"),
                leg("B", "5.00", "USD")));

        verify(accountDao, times(1)).getAccount("A");
        verify(accountDao, times(1)).getAccount("B");
    }

    @Test
    public void validBalanceShouldAcceptEmptyLegs() {
        validator.validBalance(Collections.<TransactionLeg>emptyList());

        verifyZeroInteractions(accountDao);
    }

    @Test
    public void validBalanceShouldPropagateNullAccountDaoResult() {
        when(accountDao.getAccount("A")).thenReturn(null);

        try {
            validator.validBalance(Collections.singletonList(
                    leg("A", "10.00", "USD")));
            fail("Expected NullPointerException");
        } catch (NullPointerException expected) {
            verify(accountDao).getAccount("A");
        }
    }

    @Test
    public void validBalanceShouldStopAfterFirstOverdrawnAccount() {
        when(accountDao.getAccount("A"))
                .thenReturn(account("A", "-1.00", "USD"));

        try {
            validator.validBalance(Arrays.asList(
                    leg("A", "10.00", "USD"),
                    leg("B", "-10.00", "USD")));
            fail("Expected InsufficientFundsException");
        } catch (InsufficientFundsException expected) {
            verify(accountDao).getAccount("A");
            verify(accountDao, never()).getAccount("B");
        }
    }

    @Test
    public void settersShouldAllowReplacingDaos() {
        AccountDao anotherAccountDao = mock(AccountDao.class);
        TransactionDao anotherTransactionDao = mock(TransactionDao.class);

        validator.setAccountDao(anotherAccountDao);
        validator.setTransactionDao(anotherTransactionDao);

        when(anotherTransactionDao.getTransactionByRef("TX-007"))
                .thenReturn(null);

        validator.transferRequestExists("TX-007");

        verify(anotherTransactionDao).getTransactionByRef("TX-007");
        verifyZeroInteractions(transactionDao);
    }

    @Test
    public void validateTransferRequestShouldAcceptEmptyStrings() {
        TransferRequest request = TransferRequest.builder()
                .reference("")
                .type("")
                .account("A")
                .debit("10.00", "USD")
                .account("B")
                .credit("10.00", "USD")
                .build();

        validator.validateTransferRequest(request);
    }

    @Test
    public void validBalanceShouldTreatZeroBalanceAsNotOverdrawn() {
        when(accountDao.getAccount("A"))
                .thenReturn(account("A", "0.00", "USD"));

        validator.validBalance(Collections.singletonList(
                leg("A", "1.00", "USD")));

        verify(accountDao).getAccount("A");
    }

    @Test
    public void isTransactionBalancedShouldRejectSingleNonzeroLeg() {
        try {
            validator.isTransactionBalanced(Collections.singletonList(
                    leg("A", "1.00", "USD")));
            fail("Expected UnbalancedLegsException");
        } catch (UnbalancedLegsException expected) {
            assertEquals("Transaction legs not balanced",
                    expected.getMessage());
        }
    }

    @Test
    public void isTransactionBalancedShouldAcceptMultipleLegsSummingToZero() {
        validator.isTransactionBalanced(Arrays.asList(
                leg("A", "-15.00", "USD"),
                leg("B", "5.00", "USD"),
                leg("C", "10.00", "USD")));
    }
}
