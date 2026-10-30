package com.yanimetaxas.bookkeeping.examples;

import com.yanimetaxas.bookkeeping.ChartOfAccounts;
import com.yanimetaxas.bookkeeping.ChartOfAccounts.ChartOfAccountsBuilder;
import com.yanimetaxas.bookkeeping.Ledger;
import com.yanimetaxas.bookkeeping.Ledger.LedgerBuilder;
import com.yanimetaxas.bookkeeping.model.AccountSide;
import lombok.extern.slf4j.Slf4j;

/**
 * E-commerce ledger demonstrating an online shop's accounting flow.
 *
 * <p>Transactions move value from funded accounts (assets) to reporting accounts
 * (revenue, expenses). Each transfer is balanced: total debits equal total credits.
 *
 * <p>Run: {@code mvn -pl bookkeeping-examples -am compile -Prun-example
 * -Dexec.mainClass=com.yanimetaxas.bookkeeping.examples.ECommerceLedgerExample}
 */
@Slf4j
public class ECommerceLedgerExample {

  public static void main(String... args) {
    ChartOfAccounts chart =
        new ChartOfAccountsBuilder()
            .create("Cash", "15000.00", "EUR", AccountSide.DEBIT)
            .create("Inventory", "5000.00", "EUR", AccountSide.DEBIT)
            .create("Revenue", "0.00", "EUR", AccountSide.CREDIT)
            .create("CostOfGoodsSold", "0.00", "EUR", AccountSide.DEBIT)
            .create("ShippingExpense", "0.00", "EUR", AccountSide.DEBIT)
            .create("SalesTaxPayable", "0.00", "EUR", AccountSide.CREDIT)
            .create("OwnersEquity", "20000.00", "EUR", AccountSide.CREDIT)
            .build();

    Ledger ledger = new LedgerBuilder(chart).name("E-Commerce Ledger").build().init();

    // === Order #1: Sale for 130 EUR (110 revenue + 20 tax) ===

    ledger.commit(
        ledger
            .createTransferRequest()
            .reference("ORD-001-REVENUE")
            .type("customer-payment")
            .account("Cash")
            .debit("130.00", "EUR")
            .account("Revenue")
            .credit("110.00", "EUR")
            .account("SalesTaxPayable")
            .credit("20.00", "EUR")
            .build());

    ledger.commit(
        ledger
            .createTransferRequest()
            .reference("ORD-001-COGS")
            .type("cost-of-goods")
            .account("Inventory")
            .debit("80.00", "EUR")
            .account("CostOfGoodsSold")
            .credit("80.00", "EUR")
            .build());

    ledger.commit(
        ledger
            .createTransferRequest()
            .reference("ORD-001-SHIP")
            .type("shipping")
            .account("Cash")
            .debit("12.00", "EUR")
            .account("ShippingExpense")
            .credit("12.00", "EUR")
            .build());

    // === Order #2: Another sale (200 EUR, 140 EUR COGS) ===

    ledger.commit(
        ledger
            .createTransferRequest()
            .reference("ORD-002-REVENUE")
            .type("customer-payment")
            .account("Cash")
            .debit("200.00", "EUR")
            .account("Revenue")
            .credit("200.00", "EUR")
            .build());

    ledger.commit(
        ledger
            .createTransferRequest()
            .reference("ORD-002-COGS")
            .type("cost-of-goods")
            .account("Inventory")
            .debit("140.00", "EUR")
            .account("CostOfGoodsSold")
            .credit("140.00", "EUR")
            .build());

    log.info("=== E-Commerce Ledger ===");
    ledger.printHistoryLog();
    ledger.printTrialBalance();
  }
}
