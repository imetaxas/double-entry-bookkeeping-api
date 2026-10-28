package com.yanimetaxas.bookkeeping.manual;

import com.google.common.collect.Lists;
import com.yanimetaxas.bookkeeping.exception.UnbalancedLegsException;
import com.yanimetaxas.bookkeeping.model.Money;
import com.yanimetaxas.bookkeeping.model.TransactionLeg;
import com.yanimetaxas.bookkeeping.validation.TransferValidatorImpl;
import org.junit.Test;

/**
 * Mutant 1: TransferValidatorImpl line 95 - NonVoidMethodCall, removed call to Money::getCurrency.
 * Legs are then grouped under ONE key, so different currencies could "net off" against each other.
 *
 * Technique: decision table. Rule from TransferRequest Javadoc: the total for legs with the SAME
 * currency must be zero.
 *
 * <pre>
 * Rule | USD balanced | EUR balanced | overall sum zero | Expected
 *  R1  | yes          | yes          | yes              | accepted
 *  R2  | no           | no           | yes (cross-net)  | rejected   <-- kills the mutant
 *  R3  | yes          | no           | no               | rejected
 *  R4  | no           | yes          | no               | rejected
 * </pre>
 */
public class TransferValidatorCurrencyTest {

  private final TransferValidatorImpl validator = new TransferValidatorImpl();

  private static TransactionLeg leg(String account, String amount, String currency) {
    return new TransactionLeg(account, Money.toMoney(amount, currency));
  }

  @Test
  public void r1_eachCurrencyBalancedIsAccepted() {
    validator.isTransactionBalanced(Lists.newArrayList(
        leg("A", "-10.00", "USD"), leg("B", "10.00", "USD"),
        leg("C", "-5.00", "EUR"), leg("D", "5.00", "EUR")));
  }

  @Test(expected = UnbalancedLegsException.class)
  public void r2_currenciesMustNotNetAgainstEachOther() {
    validator.isTransactionBalanced(Lists.newArrayList(
        leg("A", "-10.00", "USD"), leg("B", "10.00", "EUR")));
  }

  @Test(expected = UnbalancedLegsException.class)
  public void r3_usdBalancedEurNotIsRejected() {
    validator.isTransactionBalanced(Lists.newArrayList(
        leg("A", "-10.00", "USD"), leg("B", "10.00", "USD"),
        leg("C", "-5.00", "EUR"), leg("D", "4.00", "EUR")));
  }

  @Test(expected = UnbalancedLegsException.class)
  public void r4_eurBalancedUsdNotIsRejected() {
    validator.isTransactionBalanced(Lists.newArrayList(
        leg("A", "-10.00", "USD"), leg("B", "9.00", "USD"),
        leg("C", "-5.00", "EUR"), leg("D", "5.00", "EUR")));
  }
}
