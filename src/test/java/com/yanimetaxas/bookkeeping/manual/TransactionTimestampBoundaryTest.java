package com.yanimetaxas.bookkeeping.manual;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;

import com.yanimetaxas.bookkeeping.model.Transaction;
import com.yanimetaxas.bookkeeping.model.TransactionLeg;
import java.util.ArrayList;
import java.util.Date;
import org.junit.Test;

/**
 * Mutant 7: Transaction line 49 - removed call to Date::getTime (the copy gets time 0).
 *
 * Technique: boundary value analysis on the timestamp (milliseconds since 1970-01-01):
 * just before the epoch (-1), the epoch itself (0), just after it (1), and a realistic value.
 * Testing only 0 would NOT detect this mutant, because the mutant returns 0; the values 1, -1
 * and 1700000000000 do. The AI suite only asserted assertNotNull on the date.
 */
public class TransactionTimestampBoundaryTest {

  private static final long[] BOUNDARIES = {-1L, 0L, 1L, 1700000000000L};

  @Test
  public void getTransactionDateReturnsAnEqualSeparateCopyAtEveryBoundary() {
    for (long millis : BOUNDARIES) {
      Date original = new Date(millis);
      Transaction t = new Transaction("T", "type", original, new ArrayList<TransactionLeg>());
      Date copy = t.getTransactionDate();
      assertEquals("millis=" + millis, millis, copy.getTime());
      assertNotSame("millis=" + millis, original, copy);
    }
  }
}
