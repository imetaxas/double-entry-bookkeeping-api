package com.yanimetaxas.bookkeeping.manual;

import com.google.common.collect.Lists;
import com.yanimetaxas.bookkeeping.exception.UnbalancedLegsException;
import com.yanimetaxas.bookkeeping.model.Money;
import com.yanimetaxas.bookkeeping.model.TransactionLeg;
import com.yanimetaxas.bookkeeping.validation.TransferValidatorImpl;
import org.junit.Test;

import static org.junit.Assert.fail;

public class DefectCandidateTest {

    @Test
    public void balancedLegsWithoutDecimalPlacesShouldBeAccepted() {
        new TransferValidatorImpl().isTransactionBalanced(Lists.newArrayList(
                new TransactionLeg("A", Money.toMoney("-5", "EUR")),
                new TransactionLeg("B", Money.toMoney("5", "EUR"))));
    }

    @Test
    public void balancedLegsWithDifferentDecimalScalesShouldBeAccepted() {
        new TransferValidatorImpl().isTransactionBalanced(Lists.newArrayList(
                new TransactionLeg("A", Money.toMoney("-5.0", "EUR")),
                new TransactionLeg("B", Money.toMoney("5.00", "EUR"))));
    }

    @Test
    public void genuinelyUnbalancedLegsShouldBeRejected() {
        try {
            new TransferValidatorImpl().isTransactionBalanced(Lists.newArrayList(
                    new TransactionLeg("A", Money.toMoney("-5.00", "EUR")),
                    new TransactionLeg("B", Money.toMoney("4.00", "EUR"))));
            fail("Expected UnbalancedLegsException");
        } catch (UnbalancedLegsException expected) {
            // Expected: the sum is -1.00, not zero.
        }
    }
}
