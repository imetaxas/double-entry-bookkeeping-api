package com.yanimetaxas.bookkeeping.examples;

import com.yanimetaxas.bookkeeping.ChartOfAccounts;
import com.yanimetaxas.bookkeeping.ChartOfAccounts.ChartOfAccountsBuilder;
import com.yanimetaxas.bookkeeping.Ledger;
import com.yanimetaxas.bookkeeping.Ledger.LedgerBuilder;
import com.yanimetaxas.bookkeeping.model.AccountSide;
import lombok.extern.slf4j.Slf4j;

/**
 * Personal expense tracker demonstrating how to categorize spending against a bank account.
 *
 * <p>Each expense debits the source account (BankAccount or CashWallet) and credits the
 * corresponding expense category. An ATM withdrawal is a transfer between asset accounts.
 *
 * <p>Run: {@code mvn -pl bookkeeping-examples -am compile -Prun-example
 * -Dexec.mainClass=com.yanimetaxas.bookkeeping.examples.ExpenseTrackerExample}
 */
@Slf4j
public class ExpenseTrackerExample {

  public static void main(String... args) {
    ChartOfAccounts chart =
        new ChartOfAccountsBuilder()
            .create("BankAccount", "8000.00", "USD", AccountSide.DEBIT)
            .create("CashWallet", "200.00", "USD", AccountSide.DEBIT)
            .create("Groceries", "0.00", "USD", AccountSide.DEBIT)
            .create("Rent", "0.00", "USD", AccountSide.DEBIT)
            .create("Utilities", "0.00", "USD", AccountSide.DEBIT)
            .create("Transport", "0.00", "USD", AccountSide.DEBIT)
            .create("Entertainment", "0.00", "USD", AccountSide.DEBIT)
            .create("Dining", "0.00", "USD", AccountSide.DEBIT)
            .create("OpeningBalance", "8200.00", "USD", AccountSide.CREDIT)
            .build();

    Ledger ledger = new LedgerBuilder(chart).name("Personal Expense Tracker").build().init();

    ledger.commit(
        ledger
            .createTransferRequest()
            .reference("RENT-MAR")
            .type("expense")
            .account("BankAccount")
            .debit("1200.00", "USD")
            .account("Rent")
            .credit("1200.00", "USD")
            .build());

    ledger.commit(
        ledger
            .createTransferRequest()
            .reference("UTIL-MAR")
            .type("expense")
            .account("BankAccount")
            .debit("150.00", "USD")
            .account("Utilities")
            .credit("150.00", "USD")
            .build());

    ledger.commit(
        ledger
            .createTransferRequest()
            .reference("GRO-001")
            .type("expense")
            .account("BankAccount")
            .debit("85.50", "USD")
            .account("Groceries")
            .credit("85.50", "USD")
            .build());

    ledger.commit(
        ledger
            .createTransferRequest()
            .reference("GRO-002")
            .type("expense")
            .account("BankAccount")
            .debit("62.30", "USD")
            .account("Groceries")
            .credit("62.30", "USD")
            .build());

    ledger.commit(
        ledger
            .createTransferRequest()
            .reference("TRANS-001")
            .type("expense")
            .account("BankAccount")
            .debit("45.00", "USD")
            .account("Transport")
            .credit("45.00", "USD")
            .build());

    ledger.commit(
        ledger
            .createTransferRequest()
            .reference("DIN-001")
            .type("expense")
            .account("CashWallet")
            .debit("32.00", "USD")
            .account("Dining")
            .credit("32.00", "USD")
            .build());

    ledger.commit(
        ledger
            .createTransferRequest()
            .reference("ENT-001")
            .type("expense")
            .account("CashWallet")
            .debit("25.00", "USD")
            .account("Entertainment")
            .credit("25.00", "USD")
            .build());

    ledger.commit(
        ledger
            .createTransferRequest()
            .reference("ATM-001")
            .type("transfer")
            .account("BankAccount")
            .debit("100.00", "USD")
            .account("CashWallet")
            .credit("100.00", "USD")
            .build());

    log.info("=== Personal Expense Tracker ===");
    ledger.printHistoryLog();

    log.info("--- Monthly Summary ---");
    log.info("Bank Account: {}", ledger.getAccountBalance("BankAccount"));
    log.info("Cash Wallet:  {}", ledger.getAccountBalance("CashWallet"));
    log.info("Rent:         {}", ledger.getAccountBalance("Rent"));
    log.info("Groceries:    {}", ledger.getAccountBalance("Groceries"));
    log.info("Transport:    {}", ledger.getAccountBalance("Transport"));
    log.info("Dining:       {}", ledger.getAccountBalance("Dining"));

    ledger.printTrialBalance();
  }
}
