package com.yanimetaxas.bookkeeping;

import static io.github.imetaxas.realitycheck.RealityAssertions.assertThat;
import static io.github.imetaxas.realitycheck.RealityAssertions.assertThatThrownBy;

import com.yanimetaxas.bookkeeping.ChartOfAccounts.ChartOfAccountsBuilder;
import com.yanimetaxas.bookkeeping.model.Account;
import com.yanimetaxas.bookkeeping.model.AccountSide;
import org.junit.jupiter.api.Test;

class ChartOfAccountsTest {

  @Test
  void emptyChartIsRejected() {
    assertThatThrownBy(() -> new ChartOfAccountsBuilder().build())
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("at least one account");
  }

  @Test
  void duplicateAccountReferenceIsRejected() {
    assertThatThrownBy(
            () ->
                new ChartOfAccountsBuilder()
                    .create("CASH", "1.00", "EUR")
                    .create("CASH", "2.00", "EUR")
                    .build())
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("already in the chart");
  }

  @Test
  void accountsStayInInsertionOrder() {
    ChartOfAccounts chart =
        new ChartOfAccountsBuilder()
            .create("REVENUE", "0.00", "EUR", AccountSide.CREDIT)
            .create("CASH", "10.00", "EUR", AccountSide.DEBIT)
            .build();

    assertThat(chart.get().stream().map(Account::getAccountRef).toList())
        .containsExactly("REVENUE", "CASH");
  }
}
