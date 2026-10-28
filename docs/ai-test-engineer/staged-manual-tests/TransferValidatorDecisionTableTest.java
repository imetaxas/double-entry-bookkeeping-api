package com.yanimetaxas.bookkeeping.manual;

import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.common.collect.Lists;
import com.yanimetaxas.bookkeeping.dao.AccountDao;
import com.yanimetaxas.bookkeeping.exception.InsufficientFundsException;
import com.yanimetaxas.bookkeeping.exception.UnbalancedLegsException;
import com.yanimetaxas.bookkeeping.model.Account;
import com.yanimetaxas.bookkeeping.model.Money;
import com.yanimetaxas.bookkeeping.model.TransactionLeg;
import com.yanimetaxas.bookkeeping.model.TransferRequest;
import com.yanimetaxas.bookkeeping.validation.TransferValidatorImpl;
import java.util.List;
import org.junit.Before;
import org.junit.Test;

/**
 * Decision table for validateTransferRequest (conditions: ref present?, type present?, legs >= 2?)
 *
 * <pre>
 * Rule | ref  | type | legs>=2 | Expected
 *  R1  | yes  | yes  | yes     | accepted            (tested with exactly 2 legs = boundary)
 *  R2  | no   | yes  | yes     | IllegalArgument
 *  R3  | yes  | no   | yes     | IllegalArgument
 *  R4  | yes  | yes  | no (1)  | IllegalArgument     (boundary: 1 leg)
 * </pre>
 * Kills mutants S2 (size()<2 -> <=2), S3 (type null check). Also covers S7 and S8.
 */
public class TransferValidatorDecisionTableTest {

  private TransferValidatorImpl validator;
  private AccountDao accountDao;

  @Before
  public void setUp() {
    validator = new TransferValidatorImpl();
    accountDao = mock(AccountDao.class);
    validator.setAccountDao(accountDao);
  }

  private static TransferRequest twoLegs(String ref, String type) {
    return TransferRequest.builder().reference(ref).type(type)
        .account("A").debit("5.00", "EUR")
        .account("B").credit("5.00", "EUR").build();
  }

  @Test
  public void r1_completeRequestWithExactlyTwoLegsIsAccepted() {
    validator.validateTransferRequest(twoLegs("T1", "type")); // must not throw
  }

  @Test(expected = IllegalArgumentException.class)
  public void r2_missingReferenceRejected() {
    validator.validateTransferRequest(twoLegs(null, "type"));
  }

  @Test(expected = IllegalArgumentException.class)
  public void r3_missingTypeRejected() {
    validator.validateTransferRequest(twoLegs("T1", null));
  }

  @Test(expected = IllegalArgumentException.class)
  public void r4_singleLegRejected() {
    TransferRequest r = TransferRequest.builder().reference("T1").type("type")
        .account("A").credit("5.00", "EUR").build();
    validator.validateTransferRequest(r);
  }

  // ---- isTransactionBalanced: per-currency balancing (S8) ----

  private static TransactionLeg leg(String acc, String amt, String cur) {
    return new TransactionLeg(acc, Money.toMoney(amt, cur));
  }

  @Test
  public void eachCurrencyBalancedIsAccepted() {
    validator.isTransactionBalanced(Lists.newArrayList(
        leg("A", "-5.00", "EUR"), leg("B", "5.00", "EUR"),
        leg("C", "-7.00", "USD"), leg("D", "7.00", "USD")));
  }

  @Test(expected = UnbalancedLegsException.class)
  public void oneCurrencyUnbalancedIsRejectedEvenIfOtherBalanced() {
    validator.isTransactionBalanced(Lists.newArrayList(
        leg("A", "-5.00", "EUR"), leg("B", "5.00", "EUR"),
        leg("C", "-7.00", "USD"), leg("D", "6.99", "USD")));
  }

  // ---- validBalance: overdraft check on EVERY account touched (S7) ----

  @Test
  public void overdrawnSecondAccountIsDetected() {
    when(accountDao.getAccount("A")).thenReturn(new Account("A", Money.toMoney("10.00", "EUR")));
    when(accountDao.getAccount("B")).thenReturn(new Account("B", Money.toMoney("-0.01", "EUR")));
    List<TransactionLeg> legs = Lists.newArrayList(leg("A", "1.00", "EUR"), leg("B", "-1.00", "EUR"));
    try {
      validator.validBalance(legs);
      fail("expected InsufficientFundsException");
    } catch (InsufficientFundsException expected) {
      // ok
    }
  }

  @Test
  public void accountsEndingExactlyAtZeroAreAccepted() {
    when(accountDao.getAccount("A")).thenReturn(new Account("A", Money.toMoney("0.00", "EUR")));
    validator.validBalance(Lists.newArrayList(leg("A", "-1.00", "EUR")));
  }
}
