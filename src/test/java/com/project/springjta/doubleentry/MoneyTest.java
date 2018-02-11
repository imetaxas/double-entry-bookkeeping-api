package com.project.springjta.doubleentry;

import static com.project.springjta.doubleentry.Money.toMoney;
import static org.junit.Assert.*;

import org.junit.Test;

/**
 * @author yanimetaxas
 * @since 11-Feb-18
 */
public class MoneyTest {

  @Test
  public void compareTo_WhenAmountIsSame() throws Exception {
    Money money1 = toMoney("1000", "SEK");
    Money money2 = toMoney("1000", "SEK");

    assertEquals(0, money1.compareTo(money2));
    assertEquals(0, money2.compareTo(money1));
  }

  @Test
  public void compareTo_WhenAmountIsDifferent() throws Exception {
    assertEquals(-1, toMoney("1", "SEK").compareTo(toMoney("1000", "SEK")));
  }

  @Test
  public void equals_WhenMoneyIsSame() throws Exception {
    Money money1 = toMoney("1000", "SEK");

    assertTrue(money1.equals(money1) && money1.equals(money1));
    assertTrue(money1.hashCode() == money1.hashCode());
  }

  @Test
  public void equals_WhenAmountAndCurrencyAreSame() throws Exception {
    Money money1 = toMoney("1000", "SEK");
    Money money2 = toMoney("1000", "SEK");

    assertTrue(money1.equals(money2) && money2.equals(money1));
    assertTrue(money1.hashCode() == money2.hashCode());
  }

  @Test
  public void equals_WhenAmountIsDifferent() throws Exception {
    assertFalse(toMoney("1", "SEK").equals(toMoney("1000", "SEK")));
  }

  @Test
  public void equals_WhenCurrencyIsDifferent() throws Exception {
    assertFalse(toMoney("1000", "SEK").equals(toMoney("1000", "EUR")));
  }

  @Test
  public void equals_WhenAmountAndCurrencyAreDifferent() throws Exception {
    assertFalse(toMoney("1", "SEK").equals(toMoney("1000", "EUR")));
  }

  @Test
  public void equals_WhenMoneyIsNull() throws Exception {
    assertFalse(toMoney("1", "SEK").equals(null));
  }
}