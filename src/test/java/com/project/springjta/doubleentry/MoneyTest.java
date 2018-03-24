package com.project.springjta.doubleentry;

import static com.project.springjta.doubleentry.Money.toMoney;
import static com.yanimetaxas.realitycheck.Reality.checkThat;

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

    checkThat(money1.compareTo(money2)).isEqualTo(0);
    checkThat(money2.compareTo(money1)).isEqualTo(0);
  }

  @Test
  public void compareTo_WhenAmountIsDifferent() throws Exception {
    checkThat(toMoney("1", "SEK").compareTo(toMoney("1000", "SEK"))).isEqualTo(-1);
  }

  @Test
  public void equals_WhenMoneyIsSame() throws Exception {
    Money money1 = toMoney("1000", "SEK");

    checkThat(money1.equals(money1) && money1.equals(money1)).isTrue();
    checkThat(money1.hashCode() == money1.hashCode()).isTrue();
  }

  @Test
  public void equals_WhenAmountAndCurrencyAreSame() throws Exception {
    Money money1 = toMoney("1000", "SEK");
    Money money2 = toMoney("1000", "SEK");

    checkThat(money1.equals(money2) && money2.equals(money1)).isTrue();
    checkThat(money1.hashCode() == money2.hashCode()).isTrue();
  }

  @Test
  public void equals_WhenAmountIsDifferent() throws Exception {
    checkThat(toMoney("1", "SEK").equals(toMoney("1000", "SEK"))).isFalse();
  }

  @Test
  public void equals_WhenCurrencyIsDifferent() throws Exception {
    checkThat(toMoney("1000", "SEK").equals(toMoney("1000", "EUR"))).isFalse();
  }

  @Test
  public void equals_WhenAmountAndCurrencyAreDifferent() throws Exception {
    checkThat(toMoney("1", "SEK").equals(toMoney("1000", "EUR"))).isFalse();
  }

  @Test
  public void equals_WhenMoneyIsNull() throws Exception {
    checkThat(toMoney("1", "SEK").equals(null)).isFalse();
  }
}