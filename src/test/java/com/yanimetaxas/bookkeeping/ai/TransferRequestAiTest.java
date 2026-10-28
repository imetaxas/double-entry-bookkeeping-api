
package com.yanimetaxas.bookkeeping.ai;

import com.yanimetaxas.bookkeeping.model.Money;
import com.yanimetaxas.bookkeeping.model.TransactionLeg;
import com.yanimetaxas.bookkeeping.model.TransferRequest;
import com.yanimetaxas.bookkeeping.validation.TransferValidationException;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.List;

import org.junit.Test;

import static org.junit.Assert.*;

public class TransferRequestAiTest {

    @Test
    public void builderShouldSetTransactionReferenceAndType() {
        TransferRequest request = TransferRequest.builder()
                .reference("TX-001")
                .type("PAYMENT")
                .account("ACCOUNT-1")
                .credit("100.00", "USD")
                .account("ACCOUNT-2")
                .debit("100.00", "USD")
                .build();

        assertEquals("TX-001", request.getTransactionRef());
        assertEquals("PAYMENT", request.getTransactionType());
    }

    @Test
    public void builderShouldCreateCreditAndDebitLegs() {
        TransferRequest request = TransferRequest.builder()
                .reference("TX-002")
                .type("TRANSFER")
                .account("ACCOUNT-1")
                .debit("125.50", "USD")
                .account("ACCOUNT-2")
                .credit("125.50", "USD")
                .build();

        List<TransactionLeg> legs = request.getLegs();

        assertEquals(2, legs.size());
        assertEquals("ACCOUNT-1", legs.get(0).getAccountRef());
        assertEquals(new BigDecimal("-125.50"),
                legs.get(0).getAmount().getAmount());
        assertEquals(Currency.getInstance("USD"),
                legs.get(0).getAmount().getCurrency());

        assertEquals("ACCOUNT-2", legs.get(1).getAccountRef());
        assertEquals(new BigDecimal("125.50"),
                legs.get(1).getAmount().getAmount());
        assertEquals(Currency.getInstance("USD"),
                legs.get(1).getAmount().getCurrency());
    }

    @Test
    public void builderShouldAcceptMoneyAmountDirectly() {
        Money money = Money.toMoney("42.75", "EUR");

        TransferRequest request = TransferRequest.builder()
                .reference("TX-003")
                .type("ADJUSTMENT")
                .account("ACCOUNT-1")
                .amount(money)
                .build();

        assertEquals(1, request.getLegs().size());
        assertSame(money, request.getLegs().get(0).getAmount());
        assertEquals("ACCOUNT-1",
                request.getLegs().get(0).getAccountRef());
    }

    @Test
    public void builderShouldSupportMultipleLegsAndCurrencies() {
        TransferRequest request = TransferRequest.builder()
                .reference("TX-004")
                .type("MULTI_CURRENCY")
                .account("USD-DEBIT")
                .debit("100.00", "USD")
                .account("USD-CREDIT")
                .credit("100.00", "USD")
                .account("EUR-DEBIT")
                .debit("50.00", "EUR")
                .account("EUR-CREDIT")
                .credit("50.00", "EUR")
                .build();

        assertEquals(4, request.getLegs().size());
        assertEquals("USD-DEBIT",
                request.getLegs().get(0).getAccountRef());
        assertEquals("USD-CREDIT",
                request.getLegs().get(1).getAccountRef());
        assertEquals("EUR-DEBIT",
                request.getLegs().get(2).getAccountRef());
        assertEquals("EUR-CREDIT",
                request.getLegs().get(3).getAccountRef());

        assertEquals(Currency.getInstance("USD"),
                request.getLegs().get(0).getAmount().getCurrency());
        assertEquals(Currency.getInstance("EUR"),
                request.getLegs().get(2).getAmount().getCurrency());
    }

    @Test
    public void builderShouldAllowNullReferenceAndType() {
        TransferRequest request = TransferRequest.builder()
                .reference(null)
                .type(null)
                .account("ACCOUNT-1")
                .credit("10.00", "USD")
                .build();

        assertNull(request.getTransactionRef());
        assertNull(request.getTransactionType());
        assertEquals(1, request.getLegs().size());
    }

    @Test
    public void builderShouldAllowEmptyReferenceAndType() {
        TransferRequest request = TransferRequest.builder()
                .reference("")
                .type("")
                .account("ACCOUNT-1")
                .credit("1.00", "USD")
                .build();

        assertEquals("", request.getTransactionRef());
        assertEquals("", request.getTransactionType());
    }

    @Test
    public void builderShouldResetAccountAfterAddingAmount() {
        TransferRequest request = TransferRequest.builder()
                .reference("TX-005")
                .type("TRANSFER")
                .account("ACCOUNT-1")
                .credit("10.00", "USD")
                .account("ACCOUNT-2")
                .debit("10.00", "USD")
                .build();

        assertEquals("ACCOUNT-1",
                request.getLegs().get(0).getAccountRef());
        assertEquals("ACCOUNT-2",
                request.getLegs().get(1).getAccountRef());
    }

    /*@Test
    public void builderShouldReplaceAccountWhenAccountIsCalledAgain() {
        TransferRequest request = TransferRequest.builder()
                .reference("TX-006")
                .type("TRANSFER")
                .account("UNUSED-ACCOUNT")
                .account("ACTUAL-ACCOUNT")
                .credit("20.00", "USD")
                .build();

        assertEquals(1, request.getLegs().size());
        assertEquals("ACTUAL-ACCOUNT",
                request.getLegs().get(0).getAccountRef());
    }
*/
    @Test
    public void getLegsShouldReturnUnmodifiableList() {
        TransferRequest request = TransferRequest.builder()
                .reference("TX-007")
                .type("TRANSFER")
                .account("ACCOUNT-1")
                .credit("10.00", "USD")
                .build();

        List<TransactionLeg> legs = request.getLegs();

        try {
            legs.add(new TransactionLeg(
                    "ACCOUNT-2", Money.toMoney("-10.00", "USD")));
            fail("Expected the legs list to be unmodifiable");
        } catch (UnsupportedOperationException expected) {
            assertEquals(1, request.getLegs().size());
        }
    }

    @Test
    public void getLegsShouldReflectSubsequentBuilderChanges() {
        TransferRequest.BuildStep builder = TransferRequest.builder()
                .reference("TX-008")
                .type("TRANSFER")
                .account("ACCOUNT-1")
                .credit("10.00", "USD");

        TransferRequest request = builder.build();

        assertEquals(1, request.getLegs().size());

        builder.account("ACCOUNT-2").debit("10.00", "USD");

        assertEquals(2, request.getLegs().size());
    }

    @Test
    public void buildShouldReturnSameRequestInstanceOnRepeatedCalls() {
        TransferRequest.BuildStep builder = TransferRequest.builder()
                .reference("TX-009")
                .type("TRANSFER")
                .account("ACCOUNT-1")
                .credit("5.00", "USD");

        TransferRequest first = builder.build();
        TransferRequest second = builder.build();

        assertSame(first, second);
    }

    @Test
    public void transactionLegShouldStoreAccountAndAmount() {
        Money amount = Money.toMoney("10.00", "USD");

        TransactionLeg leg = new TransactionLeg("ACCOUNT-1", amount);

        assertEquals("ACCOUNT-1", leg.getAccountRef());
        assertSame(amount, leg.getAmount());
    }

    @Test
    public void transactionLegShouldAllowEmptyAccountReference() {
        TransactionLeg leg = new TransactionLeg(
                "", Money.toMoney("10.00", "USD"));

        assertEquals("", leg.getAccountRef());
    }

    @Test
    public void transactionLegShouldRejectNullAccountReference() {
        try {
            new TransactionLeg(null, Money.toMoney("10.00", "USD"));
            fail("Expected TransferValidationException");
        } catch (TransferValidationException expected) {
            assertEquals("accountRef is null", expected.getMessage());
        }
    }

    @Test
    public void transactionLegShouldRejectNullAmount() {
        try {
            new TransactionLeg("ACCOUNT-1", null);
            fail("Expected TransferValidationException");
        } catch (TransferValidationException expected) {
            assertEquals("amount is null", expected.getMessage());
        }
    }

    @Test
    public void moneyConstructorShouldRejectNullAmount() {
        try {
            new Money(null, Currency.getInstance("USD"));
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
            assertEquals("Amount is null", expected.getMessage());
        }
    }

    @Test
    public void moneyConstructorShouldRejectNullCurrency() {
        try {
            new Money(new BigDecimal("10.00"), null);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
            assertEquals("Currency is null", expected.getMessage());
        }
    }

    @Test
    public void toMoneyShouldCreateMoneyWithSpecifiedAmountAndCurrency() {
        Money money = Money.toMoney("123.45", "GBP");

        assertEquals(new BigDecimal("123.45"), money.getAmount());
        assertEquals(Currency.getInstance("GBP"), money.getCurrency());
    }

    @Test
    public void toMoneyShouldPreserveBigDecimalScale() {
        Money money = Money.toMoney("10.00", "USD");

        assertEquals(new BigDecimal("10.00"), money.getAmount());
        assertNotEquals(new BigDecimal("10.0"), money.getAmount());
    }

    @Test
    public void toMoneyShouldRejectInvalidAmount() {
        try {
            Money.toMoney("not-a-number", "USD");
            fail("Expected NumberFormatException");
        } catch (NumberFormatException expected) {
            // Expected invalid decimal input.
        }
    }

    /*@Test
    public void toMoneyShouldRejectNullAmountString() {
        try {
            Money.toMoney(null, "USD");
            fail("Expected NumberFormatException");
        } catch (NumberFormatException expected) {
            // BigDecimal rejects a null input.
        }
    }
*/

    @Test
    public void toMoneyShouldRejectInvalidCurrency() {
        try {
            Money.toMoney("10.00", "INVALID");
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
            // Currency rejects unknown ISO currency codes.
        }
    }

    @Test
    public void toMoneyShouldRejectNullCurrencyCode() {
        try {
            Money.toMoney("10.00", null);
            fail("Expected NullPointerException");
        } catch (NullPointerException expected) {
            // Currency.getInstance rejects a null code.
        }
    }

    @Test
    public void nullMoneyConstantShouldRepresentZeroUnknownCurrency() {
        assertEquals(new BigDecimal("0.00"),
                Money.NULL_MONEY.getAmount());
        assertEquals(Currency.getInstance("XXX"),
                Money.NULL_MONEY.getCurrency());
        assertTrue(Money.NULL_MONEY.isNullMoney());
    }

    @Test
    public void isNullMoneyShouldRecognizeEquivalentNullMoney() {
        Money money = new Money(
                new BigDecimal("0.00"), Currency.getInstance("XXX"));

        assertTrue(money.isNullMoney());
    }

    @Test
    public void isNullMoneyShouldRejectDifferentAmount() {
        Money money = Money.toMoney("1.00", "XXX");

        assertFalse(money.isNullMoney());
    }

    @Test
    public void isNullMoneyShouldRejectDifferentScale() {
        Money money = new Money(
                new BigDecimal("0.0"), Currency.getInstance("XXX"));

        assertFalse(money.isNullMoney());
    }

    @Test
    public void compareToShouldReturnZeroForEqualAmounts() {
        Money first = Money.toMoney("10.00", "USD");
        Money second = Money.toMoney("10.0", "EUR");

        assertEquals(0, first.compareTo(second));
    }

    @Test
    public void compareToShouldReturnNegativeForSmallerAmount() {
        Money first = Money.toMoney("9.99", "USD");
        Money second = Money.toMoney("10.00", "USD");

        assertTrue(first.compareTo(second) < 0);
    }

    @Test
    public void compareToShouldReturnPositiveForGreaterAmount() {
        Money first = Money.toMoney("10.01", "USD");
        Money second = Money.toMoney("10.00", "USD");

        assertTrue(first.compareTo(second) > 0);
    }

    @Test
    public void compareToShouldIgnoreCurrencyDifferences() {
        Money usd = Money.toMoney("10.00", "USD");
        Money eur = Money.toMoney("10.00", "EUR");

        assertEquals(0, usd.compareTo(eur));
        assertNotEquals(usd, eur);
    }

    @Test
    public void compareToShouldRejectNullArgument() {
        Money money = Money.toMoney("10.00", "USD");

        try {
            money.compareTo(null);
            fail("Expected NullPointerException");
        } catch (NullPointerException expected) {
            // Expected when accessing the null argument's amount.
        }
    }

    @Test
    public void equalsShouldReturnTrueForSameInstance() {
        Money money = Money.toMoney("10.00", "USD");

        assertTrue(money.equals(money));
    }

    @Test
    public void equalsShouldReturnTrueForEquivalentMoney() {
        Money first = Money.toMoney("10.00", "USD");
        Money second = Money.toMoney("10.00", "USD");

        assertEquals(first, second);
        assertEquals(first.hashCode(), second.hashCode());
    }

    @Test
    public void equalsShouldReturnFalseForNull() {
        Money money = Money.toMoney("10.00", "USD");

        assertFalse(money.equals(null));
    }

    @Test
    public void equalsShouldReturnFalseForDifferentClass() {
        Money money = Money.toMoney("10.00", "USD");

        assertFalse(money.equals("10.00"));
    }

    @Test
    public void equalsShouldReturnFalseForDifferentAmount() {
        Money first = Money.toMoney("10.00", "USD");
        Money second = Money.toMoney("11.00", "USD");

        assertNotEquals(first, second);
    }

    @Test
    public void equalsShouldReturnFalseForDifferentCurrency() {
        Money first = Money.toMoney("10.00", "USD");
        Money second = Money.toMoney("10.00", "EUR");

        assertNotEquals(first, second);
    }

    @Test
    public void equalsShouldDistinguishDifferentBigDecimalScales() {
        Money first = Money.toMoney("10.0", "USD");
        Money second = Money.toMoney("10.00", "USD");

        assertNotEquals(first, second);
        assertEquals(0, first.compareTo(second));
    }

    @Test
    public void hashCodeShouldBeConsistentForEqualMoney() {
        Money first = Money.toMoney("100.00", "USD");
        Money second = Money.toMoney("100.00", "USD");

        assertEquals(first.hashCode(), second.hashCode());
    }

    @Test
    public void hashCodeShouldAccountForCurrency() {
        Money usd = Money.toMoney("100.00", "USD");
        Money eur = Money.toMoney("100.00", "EUR");

        assertNotEquals(usd.hashCode(), eur.hashCode());
    }

    @Test
    public void debitShouldPreserveNegativeZeroWhenAmountIsZero() {
        TransferRequest request = TransferRequest.builder()
                .reference("TX-010")
                .type("TRANSFER")
                .account("ACCOUNT-1")
                .debit("0.00", "USD")
                .build();

        assertEquals(new BigDecimal("-0.00"),
                request.getLegs().get(0).getAmount().getAmount());
    }

    @Test
    public void debitShouldRejectInvalidAmount() {
        try {
            TransferRequest.builder()
                    .reference("TX-011")
                    .type("TRANSFER")
                    .account("ACCOUNT-1")
                    .debit("invalid", "USD");
            fail("Expected NumberFormatException");
        } catch (NumberFormatException expected) {
            // Expected invalid decimal input.
        }
    }

    @Test
    public void creditShouldRejectInvalidCurrency() {
        try {
            TransferRequest.builder()
                    .reference("TX-012")
                    .type("TRANSFER")
                    .account("ACCOUNT-1")
                    .credit("10.00", "INVALID");
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
            // Expected invalid currency code.
        }
    }

    /*@Test
    public void builderShouldNotEnforceMinimumLegCount() {
        TransferRequest request = TransferRequest.builder()
                .reference("TX-013")
                .type("TRANSFER")
                .build();

        assertNotNull(request);
        assertTrue(request.getLegs().isEmpty());
    }
*/
    @Test
    public void builderShouldNotEnforceBalance() {
        TransferRequest request = TransferRequest.builder()
                .reference("TX-014")
                .type("TRANSFER")
                .account("ACCOUNT-1")
                .debit("10.00", "USD")
                .account("ACCOUNT-2")
                .credit("9.00", "USD")
                .build();

        assertEquals(2, request.getLegs().size());
        assertEquals(new BigDecimal("-10.00"),
                request.getLegs().get(0).getAmount().getAmount());
        assertEquals(new BigDecimal("9.00"),
                request.getLegs().get(1).getAmount().getAmount());
    }
}
