package com.yanimetaxas.bookkeeping.report;

import com.yanimetaxas.bookkeeping.model.TrialBalance;
import com.yanimetaxas.bookkeeping.model.TrialBalanceEntry;

/** Renders a trial balance as a text table. */
public final class TrialBalanceReport {

  private TrialBalanceReport() {}

  public static String format(TrialBalance trialBalance) {
    int accountWidth =
        Math.max(
            "Account".length(),
            trialBalance.getEntries().stream()
                .mapToInt(entry -> entry.getAccountRef().length())
                .max()
                .orElse(0));
    int debitWidth =
        Math.max(
            "Debit".length(),
            trialBalance.getEntries().stream()
                .mapToInt(entry -> money(entry.getDebitTotal()))
                .max()
                .orElse(0));
    int creditWidth =
        Math.max(
            "Credit".length(),
            trialBalance.getEntries().stream()
                .mapToInt(entry -> money(entry.getCreditTotal()))
                .max()
                .orElse(0));

    String separator = TextTable.separator(accountWidth, debitWidth, creditWidth);
    String format = "| %-" + accountWidth + "s | %" + debitWidth + "s | %" + creditWidth + "s |";

    StringBuilder report = new StringBuilder();
    report.append("Trial Balance (").append(trialBalance.getTimestamp()).append(")\n\n");
    report.append(separator).append("\n");
    report.append(TextTable.row(format, "Account", "Debit", "Credit")).append("\n");
    report.append(separator).append("\n");
    for (TrialBalanceEntry entry : trialBalance.getEntries()) {
      report
          .append(
              TextTable.row(
                  format,
                  entry.getAccountRef(),
                  entry.getDebitTotal().toPlainString(),
                  entry.getCreditTotal().toPlainString()))
          .append("\n");
    }
    report.append(separator).append("\n");
    report
        .append(
            TextTable.row(
                format,
                "TOTAL",
                trialBalance.getTotalDebits().toPlainString(),
                trialBalance.getTotalCredits().toPlainString()))
        .append("\n");
    report.append(separator).append("\n");
    report.append("Status: ").append(trialBalance.isBalanced() ? "BALANCED" : "UNBALANCED");
    return report.toString();
  }

  private static int money(java.math.BigDecimal amount) {
    return amount.toPlainString().length();
  }
}
