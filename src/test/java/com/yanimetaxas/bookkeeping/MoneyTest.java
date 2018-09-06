package com.yanimetaxas.bookkeeping;

import static com.yanimetaxas.realitycheck.Reality.checkThat;

import com.yanimetaxas.bookkeeping.model.Money;
import org.junit.Test;

/**
 * @author yanimetaxas
 * @since 11-Feb-18
 */
public class MoneyTest {

  @Test
  public void compareToWhenAmountIsSame() {
    Money money1 = Money.toMoney("1000", "SEK");
    Money money2 = Money.toMoney("1000", "SEK");

    checkThat(money1.compareTo(money2)).isEqualTo(0);
    checkThat(money2.compareTo(money1)).isEqualTo(0);
  }

  @Test
  public void compareToWhenAmountIsDifferent() {
    checkThat(Money.toMoney("1", "SEK").compareTo(Money.toMoney("1000", "SEK"))).isEqualTo(-1);
  }

  @Test
  public void equalsWhenMoneyIsSame() {
    Money money1 = Money.toMoney("1000", "SEK");

    checkThat(money1.equals(money1) && money1.equals(money1)).isTrue();
    checkThat(money1.hashCode() == money1.hashCode()).isTrue();
  }

  @Test
  public void equalsWhenAmountAndCurrencyAreSame() {
    Money money1 = Money.toMoney("1000", "SEK");
    Money money2 = Money.toMoney("1000", "SEK");

    checkThat(money1.equals(money2) && money2.equals(money1)).isTrue();
    checkThat(money1.hashCode() == money2.hashCode()).isTrue();
  }

  @Test
  public void equalsWhenAmountIsDifferent() {
    checkThat(Money.toMoney("1", "SEK").equals(Money.toMoney("1000", "SEK"))).isFalse();
  }

  @Test
  public void equalsWhenCurrencyIsDifferent() {
    checkThat(Money.toMoney("1000", "SEK").equals(Money.toMoney("1000", "EUR"))).isFalse();
  }

  @Test
  public void equalsWhenAmountAndCurrencyAreDifferent() {
    checkThat(Money.toMoney("1", "SEK").equals(Money.toMoney("1000", "EUR"))).isFalse();
  }

  @Test
  public void equalsWhenMoneyIsNull() {
    checkThat(Money.toMoney("1", "SEK").equals(null)).isFalse();
  }
}