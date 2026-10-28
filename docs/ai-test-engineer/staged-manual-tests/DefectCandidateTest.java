package com.yanimetaxas.bookkeeping.manual;

import com.google.common.collect.Lists;
import com.yanimetaxas.bookkeeping.model.Money;
import com.yanimetaxas.bookkeeping.model.TransactionLeg;
import com.yanimetaxas.bookkeeping.validation.TransferValidatorImpl;
import org.junit.Ignore;
import org.junit.Test;

/**
 * DEFECT CANDIDATE: BigDecimal.equals() is scale-sensitive. checkTransactionLegsBalanced compares
 * the sum with new BigDecimal("0.00"), so legs written as "5" / "-5" (scale 0) sum to 0 (scale 0)
 * and are wrongly reported as unbalanced. Remove @Ignore to reproduce; log it in the defect count.
 */
public class DefectCandidateTest {

  @Ignore("Known defect candidate - reproduces scale-sensitivity bug (log in defect density)")
  @Test
  public void balancedLegsWithoutDecimalPlacesShouldBeAccepted() {
    new TransferValidatorImpl().isTransactionBalanced(Lists.newArrayList(
        new TransactionLeg("A", Money.toMoney("-5", "EUR")),
        new TransactionLeg("B", Money.toMoney("5", "EUR"))));
  }
}
