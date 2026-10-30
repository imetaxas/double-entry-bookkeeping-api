package com.yanimetaxas.bookkeeping.examples;

import com.yanimetaxas.bookkeeping.ChartOfAccounts;
import com.yanimetaxas.bookkeeping.ChartOfAccounts.ChartOfAccountsBuilder;
import com.yanimetaxas.bookkeeping.Ledger;
import com.yanimetaxas.bookkeeping.Ledger.LedgerBuilder;
import com.yanimetaxas.bookkeeping.model.AccountSide;
import lombok.extern.slf4j.Slf4j;

/**
 * Multi-currency foreign exchange example showing how to maintain separate ledgers per currency
 * and record FX trades as paired entries.
 *
 * <p>Each currency gets its own ledger since you cannot mix currencies in a single balanced
 * transaction. An FX trade is recorded as two separate journal entries — one in each currency —
 * linked by a common reference prefix.
 *
 * <p>Run: {@code mvn -pl bookkeeping-examples -am compile -Prun-example
 * -Dexec.mainClass=com.yanimetaxas.bookkeeping.examples.MultiCurrencyFXExample}
 */
@Slf4j
public class MultiCurrencyFXExample {

  public static void main(String... args) {
    Ledger eurLedger =
        new LedgerBuilder(
                new ChartOfAccountsBuilder()
                    .create("EUR:Cash", "10000.00", "EUR", AccountSide.DEBIT)
                    .create("EUR:FXOutflow", "0.00", "EUR", AccountSide.CREDIT)
                    .create("EUR:Revenue", "0.00", "EUR", AccountSide.CREDIT)
                    .create("EUR:Equity", "10000.00", "EUR", AccountSide.CREDIT)
                    .build())
            .name("EUR Ledger")
            .build()
            .init();

    Ledger usdLedger =
        new LedgerBuilder(
                new ChartOfAccountsBuilder()
                    .create("USD:Cash", "5000.00", "USD", AccountSide.DEBIT)
                    .create("USD:FXOutflow", "0.00", "USD", AccountSide.CREDIT)
                    .create("USD:Revenue", "0.00", "USD", AccountSide.CREDIT)
                    .create("USD:Equity", "5000.00", "USD", AccountSide.CREDIT)
                    .build())
            .name("USD Ledger")
            .build()
            .init();

    // FX Trade: Sell 1,850 EUR, Buy 2,000 USD (rate: 1.0811)
    eurLedger.commit(
        eurLedger
            .createTransferRequest()
            .reference("FX-001-EUR")
            .type("fx-sell")
            .account("EUR:Cash")
            .debit("1850.00", "EUR")
            .account("EUR:FXOutflow")
            .credit("1850.00", "EUR")
            .build());

    usdLedger.commit(
        usdLedger
            .createTransferRequest()
            .reference("FX-001-USD")
            .type("fx-buy")
            .account("USD:Cash")
            .debit("2000.00", "USD")
            .account("USD:Revenue")
            .credit("2000.00", "USD")
            .build());

    // Revenue in each currency
    eurLedger.commit(
        eurLedger
            .createTransferRequest()
            .reference("REV-EUR-001")
            .type("sale")
            .account("EUR:Cash")
            .debit("500.00", "EUR")
            .account("EUR:Revenue")
            .credit("500.00", "EUR")
            .build());

    usdLedger.commit(
        usdLedger
            .createTransferRequest()
            .reference("REV-USD-001")
            .type("sale")
            .account("USD:Cash")
            .debit("750.00", "USD")
            .account("USD:Revenue")
            .credit("750.00", "USD")
            .build());

    log.info("=== Multi-Currency FX Example ===");

    log.info("--- EUR Ledger ---");
    eurLedger.printHistoryLog();
    eurLedger.printTrialBalance();

    log.info("--- USD Ledger ---");
    usdLedger.printHistoryLog();
    usdLedger.printTrialBalance();

    log.info("--- Position Summary ---");
    log.info("EUR Cash: {}", eurLedger.getAccountBalance("EUR:Cash"));
    log.info("USD Cash: {}", usdLedger.getAccountBalance("USD:Cash"));
  }
}
