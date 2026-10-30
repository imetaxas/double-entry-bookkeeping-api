package com.yanimetaxas.bookkeeping;

import static io.github.imetaxas.realitycheck.RealityAssertions.assertThat;
import static io.github.imetaxas.realitycheck.RealityAssertions.assertThatThrownBy;

import com.yanimetaxas.bookkeeping.model.Money;
import org.junit.jupiter.api.Test;

class MoneyTest {

  @Test
  void compareToWhenAmountIsSame() {
    Money money1 = Money.toMoney("1000", "SEK");
    Money money2 = Money.toMoney("1000", "SEK");

    assertThat(money1.compareTo(money2)).isZero();
    assertThat(money2.compareTo(money1)).isZero();
  }

  @Test
  void compareToWhenAmountIsDifferent() {
    assertThat(Money.toMoney("1", "SEK").compareTo(Money.toMoney("1000", "SEK"))).isNegative();
  }

  @Test
  void equalsWhenMoneyIsSame() {
    Money money1 = Money.toMoney("1000", "SEK");

    assertThat(money1).isEqualTo(money1);
  }

  @Test
  void equalsWhenAmountAndCurrencyAreSame() {
    Money money1 = Money.toMoney("1000", "SEK");
    Money money2 = Money.toMoney("1000", "SEK");
    assertThat(money1).isEqualTo(money2);
    assertThat(money1.hashCode()).isEqualTo(money2.hashCode());
  }

  @Test
  void equalsWhenAmountIsDifferent() {
    assertThat(Money.toMoney("1", "SEK")).isNotEqualTo(Money.toMoney("1000", "SEK"));
  }

  @Test
  void equalsWhenCurrencyIsDifferent() {
    assertThat(Money.toMoney("1000", "SEK")).isNotEqualTo(Money.toMoney("1000", "EUR"));
  }

  @Test
  void constructorRejectsNullAmount() {
    assertThatThrownBy(() -> new Money(null, java.util.Currency.getInstance("EUR")))
        .isInstanceOf(NullPointerException.class);
  }

  @Test
  void constructorRejectsNullCurrency() {
    assertThatThrownBy(() -> new Money(new java.math.BigDecimal("1000.00"), null))
        .isInstanceOf(NullPointerException.class);
  }

  @Test
  void isNullMoneyReturnsTrueForNullMoney() {
    assertThat(Money.NULL_MONEY.isNullMoney()).isTrue();
  }

  @Test
  void isNullMoneyReturnsFalseForRealMoney() {
    assertThat(Money.toMoney("100", "EUR").isNullMoney()).isFalse();
  }
}
