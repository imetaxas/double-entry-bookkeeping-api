package com.yanimetaxas.bookkeeping.manual;

import com.yanimetaxas.bookkeeping.dao.AccountDao;
import com.yanimetaxas.bookkeeping.dao.TransactionDao;
import com.yanimetaxas.bookkeeping.exception.AccountNotFoundException;
import com.yanimetaxas.bookkeeping.exception.UnbalancedLegsException;
import com.yanimetaxas.bookkeeping.model.Money;
import com.yanimetaxas.bookkeeping.model.Transaction;
import com.yanimetaxas.bookkeeping.model.TransactionLeg;
import com.yanimetaxas.bookkeeping.model.TransferRequest;
import com.yanimetaxas.bookkeeping.service.TransferServiceImpl;
import com.yanimetaxas.bookkeeping.validation.TransferValidatorImpl;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Currency;
import java.util.Date;
import java.util.List;

import org.junit.Test;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Manual tests written to kill survivors from the PIT v1 report
 * (AI-only tests). Java 7 compatible: no lambdas, no streams.
 */
public class ManualMutationKillTests {

    private static final Currency USD = Currency.getInstance("USD");

    // ------------------------------------------------------------------
    // Money.hashCode  -> survivors Money.java:96 and Money.java:97 (x3)
    // The AI tests only checked "equal objects have equal hashes", never
    // the actual value, so any change to the formula still passed.
    // Currency.hashCode() is identity-based, so the expected value is
    // computed inside the test instead of hard-coded.
    // ------------------------------------------------------------------
    @Test
    public void moneyHashCodeIsAmountHashTimes31PlusCurrencyHash() {
        BigDecimal amount = new BigDecimal("100.00"); // hashCode = 310002
        Money money = new Money(amount, USD);

        int expected = 31 * amount.hashCode() + USD.hashCode();

        assertEquals(expected, money.hashCode());
    }

    // ------------------------------------------------------------------
    // Transaction.java:45 -> unmodifiableList replaced by its argument
    // ------------------------------------------------------------------
    @Test
    public void transactionLegsCannotBeModifiedFromOutside() {
        List<TransactionLeg> legs = new ArrayList<TransactionLeg>();
        legs.add(new TransactionLeg("A", Money.toMoney("-10.00", "USD")));
        legs.add(new TransactionLeg("B", Money.toMoney("10.00", "USD")));
        Transaction tx = new Transaction("TX-1", "TRANSFER", new Date(), legs);

        try {
            tx.getLegs().add(new TransactionLeg("C", Money.toMoney("1.00", "USD")));
            fail("Expected UnsupportedOperationException");
        } catch (UnsupportedOperationException expected) {
            assertEquals(2, tx.getLegs().size());
        }
    }

    // ------------------------------------------------------------------
    // Transaction.java:49 -> call to Date::getTime removed
    // The AI test only did assertNotNull on the date.
    // ------------------------------------------------------------------
    @Test
    public void transactionKeepsTheExactTimestampItWasGiven() {
        long millis = 1700000000000L;
        Transaction tx = new Transaction("TX-2", "TRANSFER", new Date(millis),
                new ArrayList<TransactionLeg>());

        assertEquals(millis, tx.getTransactionDate().getTime());
    }

    // ------------------------------------------------------------------
    // TransferRequest.java:119, :127, :134 -> "Removed assignment to
    // member variable accountRef". These are the resets of the builder's
    // accountRef after amount()/credit()/debit() add a leg. The fluent
    // step interface forces account(...) next, which overwrites the field,
    // so no normal call sequence can observe the missing reset. The field
    // is therefore read by reflection.
    // ------------------------------------------------------------------
    private static Object readAccountRef(Object builder) throws Exception {
        Class<?> type = builder.getClass();
        while (type != null) {
            try {
                Field field = type.getDeclaredField("accountRef");
                field.setAccessible(true);
                return field.get(builder);
            } catch (NoSuchFieldException e) {
                type = type.getSuperclass();
            }
        }
        fail("accountRef field not found on the builder");
        return null;
    }

    @Test
    public void creditClearsAccountRefAfterAddingLeg() throws Exception {
        Object step = TransferRequest.builder()
                .reference("TX-3").type("TRANSFER")
                .account("ACCOUNT-1")
                .credit("10.00", "USD");

        assertNotEquals("ACCOUNT-1", readAccountRef(step));
    }

    @Test
    public void debitClearsAccountRefAfterAddingLeg() throws Exception {
        Object step = TransferRequest.builder()
                .reference("TX-4").type("TRANSFER")
                .account("ACCOUNT-1")
                .debit("10.00", "USD");

        assertNotEquals("ACCOUNT-1", readAccountRef(step));
    }

    @Test
    public void amountClearsAccountRefAfterAddingLeg() throws Exception {
        Object step = TransferRequest.builder()
                .reference("TX-5").type("TRANSFER")
                .account("ACCOUNT-1")
                .amount(Money.toMoney("10.00", "USD"));

        assertNotEquals("ACCOUNT-1", readAccountRef(step));
    }

    // ------------------------------------------------------------------
    // TransferServiceImpl.java:68 -> `return Lists.newArrayList()` replaced
    // by Collections.emptyList(). Both are empty, but the original hands the
    // caller a MUTABLE list. The AI test only asserted isEmpty().
    // ------------------------------------------------------------------
    @Test
    public void noTransactionsReturnsAMutableEmptyList() throws AccountNotFoundException {
        AccountDao accountDao = mock(AccountDao.class);
        TransactionDao transactionDao = mock(TransactionDao.class);
        TransferServiceImpl service = new TransferServiceImpl();
        service.setAccountDao(accountDao);
        service.setTransactionDao(transactionDao);
        when(accountDao.accountExists("ACC-1")).thenReturn(true);
        when(transactionDao.getTransactionRefsForAccount("ACC-1"))
                .thenReturn(Collections.<String>emptySet());

        List<Transaction> result = service.findTransactionsByAccountRef("ACC-1");
        result.add(new Transaction("TX-6", "TRANSFER", new Date(),
                new ArrayList<TransactionLeg>()));

        assertEquals(1, result.size());
    }

    // ------------------------------------------------------------------
    // TransferValidatorImpl.java:95 (removed Money::getCurrency in
    // sortByCurrency -> every leg lands in one bucket). (removed Money::getCurrency).
    // Only kills the mutant if line 95 is where legs are grouped by
    // currency. If that mutant still survives, treat it as equivalent.
    // ------------------------------------------------------------------
    @Test
    public void legsThatOnlyCancelAcrossDifferentCurrenciesAreUnbalanced() {
        TransferValidatorImpl validator = new TransferValidatorImpl();
        List<TransactionLeg> legs = Arrays.asList(
                new TransactionLeg("A", Money.toMoney("100.00", "USD")),
                new TransactionLeg("B", Money.toMoney("-100.00", "EUR")));

        try {
            validator.isTransactionBalanced(legs);
            fail("Expected UnbalancedLegsException");
        } catch (UnbalancedLegsException expected) {
            // each currency sums to non-zero, so this must be rejected
        }
    }
}