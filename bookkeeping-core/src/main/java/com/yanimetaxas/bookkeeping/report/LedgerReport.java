package com.yanimetaxas.bookkeeping.report;

import com.yanimetaxas.bookkeeping.model.Account;
import com.yanimetaxas.bookkeeping.model.Money;
import com.yanimetaxas.bookkeeping.model.Transaction;
import com.yanimetaxas.bookkeeping.model.TransactionLeg;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Renders a ledger's balances and history as text tables. */
public final class LedgerReport {

  private static final DateTimeFormatter DATE_FORMATTER =
      DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

  private LedgerReport() {}

  public static String format(String name, List<Account> accounts, List<Transaction> transactions) {
    return accounts(name, accounts) + "\n\n" + history(accounts, transactions) + "\n";
  }

  private static String accounts(String name, List<Account> stored) {
    List<String[]> rows = new ArrayList<>();
    for (Account account : stored) {
      Money balance = account.getBalance();
      rows.add(
          new String[] {
            account.getAccountRef(),
            balance.getAmount().toPlainString(),
            balance.getCurrency().getCurrencyCode()
          });
    }

    int accountWidth = width("Account", rows, 0);
    int amountWidth = width("Amount", rows, 1);
    int currencyWidth = width("Currency", rows, 2);
    String separator = TextTable.separator(accountWidth, amountWidth, currencyWidth);
    String format =
        "| %-" + accountWidth + "s | %" + amountWidth + "s | %-" + currencyWidth + "s |";

    StringBuilder report = new StringBuilder();
    report.append("Ledger: ").append(name).append("\n\n");
    report.append(separator).append("\n");
    report.append(TextTable.row(format, "Account", "Amount", "Currency")).append("\n");
    report.append(separator).append("\n");
    for (String[] row : rows) {
      report.append(TextTable.row(format, row)).append("\n");
    }
    report.append(separator);
    return report.toString();
  }

  private static String history(List<Account> stored, List<Transaction> transactions) {
    Map<String, List<Transaction>> byAccount = new LinkedHashMap<>();
    for (Transaction transaction : transactions) {
      Set<String> touched = new LinkedHashSet<>();
      for (TransactionLeg leg : transaction.getLegs()) {
        if (touched.add(leg.getAccountRef())) {
          byAccount.computeIfAbsent(leg.getAccountRef(), ref -> new ArrayList<>()).add(transaction);
        }
      }
    }

    List<String[]> rows = new ArrayList<>();
    List<String[]> legs = new ArrayList<>();
    for (Account account : stored) {
      List<Transaction> posted =
          byAccount.getOrDefault(account.getAccountRef(), List.of());
      if (posted.isEmpty()) {
        rows.add(new String[] {account.getAccountRef(), "-", "-", "-"});
        continue;
      }
      for (Transaction transaction : posted) {
        rows.add(
            new String[] {
              account.getAccountRef(),
              transaction.getTransactionRef(),
              transaction.getTransactionType(),
              formatDate(transaction.getTransactionDate())
            });
        for (TransactionLeg leg : transaction.getLegs()) {
          legs.add(
              new String[] {
                account.getAccountRef(),
                leg.getAccountRef(),
                leg.getAmount().getAmount().toPlainString(),
                leg.getAmount().getCurrency().getCurrencyCode()
              });
        }
      }
    }

    StringBuilder report = new StringBuilder();
    report.append(transactionTable(rows));
    if (!legs.isEmpty()) {
      report.append("\n\n").append(legTable(legs));
    }
    return report.toString();
  }

  private static String transactionTable(List<String[]> rows) {
    int accountWidth = width("Account", rows, 0);
    int transactionWidth = width("Transaction", rows, 1);
    int typeWidth = width("Type", rows, 2);
    int dateWidth = width("Date", rows, 3);
    String separator = TextTable.separator(accountWidth, transactionWidth, typeWidth, dateWidth);
    String format =
        "| %-"
            + accountWidth
            + "s | %-"
            + transactionWidth
            + "s | %-"
            + typeWidth
            + "s | %-"
            + dateWidth
            + "s |";

    StringBuilder report = new StringBuilder();
    report.append(separator).append("\n");
    report.append(TextTable.row(format, "Account", "Transaction", "Type", "Date")).append("\n");
    report.append(separator).append("\n");
    for (String[] row : rows) {
      report.append(TextTable.row(format, row)).append("\n");
    }
    report.append(separator);
    return report.toString();
  }

  private static String legTable(List<String[]> rows) {
    int accountWidth = width("Account", rows, 0);
    int legWidth = width("Leg Account", rows, 1);
    int amountWidth = width("Amount", rows, 2);
    int currencyWidth = width("Currency", rows, 3);
    String separator = TextTable.separator(accountWidth, legWidth, amountWidth, currencyWidth);
    String header =
        "| %-" + accountWidth + "s | %-" + legWidth + "s | %" + amountWidth + "s | %-"
            + currencyWidth + "s |";
    String format = header;

    StringBuilder report = new StringBuilder();
    report.append(separator).append("\n");
    report.append(TextTable.row(header, "Account", "Leg Account", "Amount", "Currency"))
        .append("\n");
    report.append(separator).append("\n");
    for (String[] row : rows) {
      report.append(TextTable.row(format, row)).append("\n");
    }
    report.append(separator);
    return report.toString();
  }

  private static int width(String header, List<String[]> rows, int column) {
    return Math.max(header.length(), rows.stream().mapToInt(row -> row[column].length()).max().orElse(0));
  }

  private static String formatDate(Instant date) {
    return date.atZone(ZoneOffset.UTC).format(DATE_FORMATTER);
  }
}
