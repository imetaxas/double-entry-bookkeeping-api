package com.yanimetaxas.bookkeeping.manual;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.yanimetaxas.bookkeeping.model.Account;
import com.yanimetaxas.bookkeeping.model.Money;
import org.junit.Test;

/** M1 - Boundary Value Analysis on the overdraft rule (balance < 0). Kills mutant S1. */
public class AccountBoundaryTest {

  private static Account acc(String amount) {
    return new Account("A", Money.toMoney(amount, "EUR"));
  }

  @Test
  public void balanceJustBelowZeroIsOverdrawn() {
    assertTrue(acc("-0.01").isOverdrawn());
  }

  @Test
  public void balanceExactlyZeroIsNotOverdrawn() { // the boundary itself
    assertFalse(acc("0.00").isOverdrawn());
  }

  @Test
  public void balanceJustAboveZeroIsNotOverdrawn() {
    assertFalse(acc("0.01").isOverdrawn());
  }
}
