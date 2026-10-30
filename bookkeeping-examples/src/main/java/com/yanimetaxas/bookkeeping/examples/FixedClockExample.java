package com.yanimetaxas.bookkeeping.examples;

import com.yanimetaxas.bookkeeping.ChartOfAccounts;
import com.yanimetaxas.bookkeeping.ChartOfAccounts.ChartOfAccountsBuilder;
import com.yanimetaxas.bookkeeping.Ledger;
import com.yanimetaxas.bookkeeping.Ledger.LedgerBuilder;
import com.yanimetaxas.bookkeeping.model.AccountSide;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import lombok.extern.slf4j.Slf4j;

/**
 * Stamps a posting with a fixed {@link Clock}. History renders that instant in UTC.
 *
 * <p>Run: {@code mvn -pl bookkeeping-examples -am compile -Prun-example
 * -Dexec.mainClass=com.yanimetaxas.bookkeeping.examples.FixedClockExample}
 */
@Slf4j
public class FixedClockExample {

  public static void main(String... args) {
    Instant postedAt = Instant.parse("2024-05-06T07:08:00Z");
    ChartOfAccounts chart =
        new ChartOfAccountsBuilder()
            .create("Cash", "1000.00", "EUR", AccountSide.DEBIT)
            .create("Revenue", "0.00", "EUR", AccountSide.CREDIT)
            .create("Equity", "1000.00", "EUR", AccountSide.CREDIT)
            .build();
    Ledger ledger =
        new LedgerBuilder(chart)
            .name("Dated Ledger")
            .clock(Clock.fixed(postedAt, ZoneOffset.UTC))
            .build()
            .init();
    ledger.commit(
        ledger
            .createTransferRequest()
            .reference("T1")
            .type("sale")
            .account("Cash")
            .debit("50.00", "EUR")
            .account("Revenue")
            .credit("50.00", "EUR")
            .build());

    Instant stored = ledger.getTransactionByRef("T1").getTransactionDate();
    if (!stored.equals(postedAt)) {
      throw new IllegalStateException("Expected " + postedAt + " but was " + stored);
    }
    if (!ledger.toString().contains("2024-05-06 07:08")) {
      throw new IllegalStateException("History did not render the UTC timestamp");
    }
    log.info("=== Fixed clock {} ===", stored);
    ledger.printHistoryLog();
  }
}
