package com.yanimetaxas.bookkeeping.ai;

import com.yanimetaxas.bookkeeping.model.Account;
import com.yanimetaxas.bookkeeping.model.Money;
import com.yanimetaxas.bookkeeping.model.TransactionLeg;
import com.yanimetaxas.bookkeeping.validation.TransferValidationException;
import java.math.BigDecimal;
import java.util.Currency;
import org.junit.Test;

import static org.junit.Assert.*;


public class MoneyAiTest {

    private static final Currency USD = Currency.getInstance("USD");
    private static final Currency EUR = Currency.getInstance("EUR");

    @Test
    public void constructorStoresAmountAndCurrency() {
        BigDecimal amount = new BigDecimal("123.45");
        Money money = new Money(amount, USD);

        assertSame(amount, money.getAmount());
        assertSame(USD, money.getCurrency());
    }

    @Test
    public void constructorRejectsNullAmount() {
        try {
            new Money(null, USD);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException ex) {
            assertEquals("Amount is null", ex.getMessage());
        }
    }

    @Test
    public void constructorRejectsNullCurrency() {
        try {
            new Money(BigDecimal.ONE, null);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException ex) {
            assertEquals("Currency is null", ex.getMessage());
        }
    }

    @Test
    public void toMoneyCreatesMoneyFromStringValues() {
        Money money = Money.toMoney("123.45", "USD");

        assertEquals(new BigDecimal("123.45"), money.getAmount());
        assertEquals(USD, money.getCurrency());
    }

    @Test
    public void toMoneyRejectsInvalidAmount() {
        try {
            Money.toMoney("not-a-number", "USD");
            fail("Expected NumberFormatException");
        } catch (NumberFormatException expected) {
            // Expected.
        }
    }

    @Test
    public void toMoneyRejectsInvalidCurrency() {
        try {
            Money.toMoney("10.00", "INVALID");
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
            // Expected.
        }
    }

    @Test
    public void equalsReturnsTrueForSameInstance() {
        Money money = Money.toMoney("10.00", "USD");

        assertTrue(money.equals(money));
    }

    @Test
    public void equalsReturnsTrueForEqualMoney() {
        Money first = Money.toMoney("10.00", "USD");
        Money second = Money.toMoney("10.00", "USD");

        assertEquals(first, second);
        assertEquals(first.hashCode(), second.hashCode());
    }

    @Test
    public void equalsReturnsFalseForDifferentAmounts() {
        assertNotEquals(
                Money.toMoney("10.00", "USD"),
                Money.toMoney("11.00", "USD"));
    }

    @Test
    public void equalsReturnsFalseForDifferentCurrencies() {
        assertNotEquals(
                Money.toMoney("10.00", "USD"),
                Money.toMoney("10.00", "EUR"));
    }

    @Test
    public void equalsReturnsFalseForNullAndDifferentTypes() {
        Money money = Money.toMoney("10.00", "USD");

        assertFalse(money.equals(null));
        assertFalse(money.equals("10.00"));
    }

    @Test
    public void equalsDistinguishesDifferentBigDecimalScales() {
        Money first = Money.toMoney("10.0", "USD");
        Money second = Money.toMoney("10.00", "USD");

        assertNotEquals(first, second);
        assertEquals(0, first.compareTo(second));
    }

    @Test
    public void compareToReturnsNegativeZeroAndPositiveResults() {
        Money ten = Money.toMoney("10.00", "USD");
        Money five = Money.toMoney("5.00", "EUR");
        Money fifteen = Money.toMoney("15.00", "USD");

        assertTrue(ten.compareTo(fifteen) < 0);
        assertEquals(0, ten.compareTo(Money.toMoney("10.0", "EUR")));
        assertTrue(ten.compareTo(five) > 0);
    }

    @Test
    public void compareToRejectsNull() {
        Money money = Money.toMoney("10.00", "USD");

        try {
            money.compareTo(null);
            fail("Expected NullPointerException");
        } catch (NullPointerException expected) {
            // Expected.
        }
    }

    @Test
    public void nullMoneyConstantHasExpectedValues() {
        Money nullMoney = Money.NULL_MONEY;

        assertEquals(new BigDecimal("0.00"), nullMoney.getAmount());
        assertEquals(Currency.getInstance("XXX"), nullMoney.getCurrency());
        assertTrue(nullMoney.isNullMoney());
    }

    @Test
    public void isNullMoneyReturnsFalseForNonNullMoney() {
        assertFalse(Money.toMoney("0.00", "USD").isNullMoney());
        assertFalse(Money.toMoney("1.00", "XXX").isNullMoney());
    }

    @Test
    public void nullMoneyIsRecognizedByValueEquality() {
        Money equivalent = new Money(
                new BigDecimal("0.00"),
                Currency.getInstance("XXX"));

        assertTrue(equivalent.isNullMoney());
    }

    @Test
    public void accountStoresReferenceAndBalance() {
        Money balance = Money.toMoney("100.00", "USD");
        Account account = new Account("ACC-1", balance);

        assertEquals("ACC-1", account.getAccountRef());
        assertSame(balance, account.getBalance());
        assertEquals(USD, account.getCurrency());
    }

    @Test
    public void accountRecognizesPositiveAndZeroBalancesAsNotOverdrawn() {
        assertFalse(new Account(
                "POSITIVE",
                Money.toMoney("10.00", "USD")).isOverdrawn());

        assertFalse(new Account(
                "ZERO",
                Money.toMoney("0.00", "USD")).isOverdrawn());
    }

    @Test
    public void accountRecognizesNegativeBalanceAsOverdrawn() {
        Account account = new Account(
                "OVERDRAWN",
                Money.toMoney("-0.01", "USD"));

        assertTrue(account.isOverdrawn());
    }

    @Test
    public void accountAllowsNullReferenceButBalanceMustBePresentForBalanceOperations() {
        Account account = new Account(null, Money.toMoney("5.00", "USD"));

        assertNull(account.getAccountRef());
        assertEquals(USD, account.getCurrency());
        assertFalse(account.isOverdrawn());
    }

    @Test
    public void accountWithNullBalanceThrowsWhenBalanceIsAccessed() {
        Account account = new Account("ACC-2", null);

        assertNull(account.getBalance());

        try {
            account.getCurrency();
            fail("Expected NullPointerException");
        } catch (NullPointerException expected) {
            // Expected.
        }

        try {
            account.isOverdrawn();
            fail("Expected NullPointerException");
        } catch (NullPointerException expected) {
            // Expected.
        }
    }

    @Test
    public void nullAccountIsSingletonAndHasExpectedValues() {
        Account first = Account.nullAccount();
        Account second = Account.nullAccount();

        assertSame(first, second);
        assertEquals("", first.getAccountRef());
        assertEquals(new BigDecimal("0.00"), first.getBalance().getAmount());
        assertEquals(Currency.getInstance("XXX"), first.getCurrency());
        assertFalse(first.isOverdrawn());
        assertTrue(first.isNullAccount());
    }

    @Test
    public void isNullAccountUsesObjectEquality() {
        Account account = new Account(
                "",
                new Money(
                        new BigDecimal("0.00"),
                        Currency.getInstance("XXX")));

        assertFalse(account.isNullAccount());
    }

    @Test
    public void transactionLegStoresAccountReferenceAndAmount() {
        Money amount = Money.toMoney("25.50", "USD");
        TransactionLeg leg = new TransactionLeg("ACC-3", amount);

        assertEquals("ACC-3", leg.getAccountRef());
        assertSame(amount, leg.getAmount());
    }

    @Test
    public void transactionLegAllowsEmptyAccountReference() {
        TransactionLeg leg = new TransactionLeg(
                "",
                Money.toMoney("1.00", "USD"));

        assertEquals("", leg.getAccountRef());
    }

    @Test
    public void transactionLegRejectsNullAccountReference() {
        try {
            new TransactionLeg(null, Money.toMoney("1.00", "USD"));
            fail("Expected TransferValidationException");
        } catch (TransferValidationException ex) {
            assertEquals("accountRef is null", ex.getMessage());
        }
    }

    @Test
    public void transactionLegRejectsNullAmount() {
        try {
            new TransactionLeg("ACC-4", null);
            fail("Expected TransferValidationException");
        } catch (TransferValidationException ex) {
            assertEquals("amount is null", ex.getMessage());
        }
    }
}