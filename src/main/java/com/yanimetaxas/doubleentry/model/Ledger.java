package com.yanimetaxas.doubleentry.model;

import com.yanimetaxas.doubleentry.AbstractAccountingConcept;
import com.yanimetaxas.doubleentry.InfrastructureException;
import com.yanimetaxas.doubleentry.Money;
import com.yanimetaxas.doubleentry.Transaction;
import com.yanimetaxas.doubleentry.TransferRequest;
import com.yanimetaxas.doubleentry.TransferRequest.ReferenceStep;
import java.util.List;

/**
 * @author yanimetaxas
 * @since 02-Feb-18
 */
public class Ledger extends AbstractAccountingConcept {

  private String name;
  private ChartOfAccounts chartOfAccounts;

  public Ledger(String name, ChartOfAccounts chartOfAccounts) throws InfrastructureException {
    super("com.yanimetaxas.doubleentry.BankFactoryImpl", ConnectionOptions.EMBEDDED_H2_CONNECTION);
    this.chartOfAccounts = chartOfAccounts;
    this.name = name;
    createAccounts();
  }

  public Ledger(String name, ChartOfAccounts chartOfAccounts, ConnectionOptions options)
      throws InfrastructureException {
    super("com.yanimetaxas.doubleentry.BankFactoryImpl", options);
    this.chartOfAccounts = chartOfAccounts;
    this.name = name;
    createAccounts();
  }

  public void commit(TransferRequest transferRequest) {
    getTransferService().transferFunds(transferRequest);
  }

  public ReferenceStep createTransferRequest() {
    return TransferRequest.builder();
  }

  public List<Transaction> findTransactions(String accountRef) {
    return getTransferService().findTransactionsByAccountRef(accountRef);
  }

  public Transaction getTransactionByRef(String accountRef) {
    return getTransferService().getTransactionByRef(accountRef);
  }

  public Money getAccountBalance(String accountRef) {
    return getAccountService().getAccountBalance(accountRef);
  }

  private void createAccounts() {
    chartOfAccounts.getAccountRefToAccountsMap().forEach(
        (accountRef, account) -> getAccountService()
            .createAccount(accountRef, account.getBalance()));
  }

  public void printHistoryLog() {
    System.out.println(toString());
  }

  private String formatAccounts() {
    StringBuilder sb = new StringBuilder();
    sb.append("Ledger: " + name + "\n\n");
    sb.append(String
        .format("%20s %20s %10s %15s %10s %10s", "Account", "|", "Amount", "|", "Currency", "|"));
    sb.append(String.format("%s",
        "\n------------------------------------------------------------------------------------------"));

    chartOfAccounts.getAccountRefToAccountsMap().forEach(
        (accountRef, account) -> {
          Money money = getAccountService().getAccountBalance(accountRef);
          sb.append("\n" + String
              .format("%20s %20s %10.2f %15s %10s %10s", accountRef, "|", money.getAmount(), "|",
                  money.getCurrency(), "|"));
        });

    return sb.toString();
  }

  private String formatTransactionLog() {
    StringBuilder sb = new StringBuilder();

    if (!chartOfAccounts.getAccountRefToAccountsMap().isEmpty()) {
      sb.append(String
          .format("%20s %20s %15s %10s %10s %10s %10s", "Account", "|", "Transaction", "|", "Type",
              "|", "Date"));
      sb.append(String.format("%s",
          "\n-------------------------------------------------------------------------------------------------------------------------"));

      chartOfAccounts.getAccountRefToAccountsMap().forEach(
          (accountRef, account) -> {
            List<Transaction> transactions = getTransferService()
                .findTransactionsByAccountRef(accountRef);
            if (!transactions.isEmpty()) {
              transactions.forEach(transaction -> sb.append("\n" + String
                  .format("%20s %20s %10s %15s %10s %10s %10s %1s", accountRef, "|",
                      transaction.getTransactionRef(), "|", transaction.getTransactionType(), "|",
                      transaction.getTransactionDate(), "|")));
            } else {
              sb.append("\n" + String
                  .format("%20s %20s %10s %15s %10s %10s %10s %1s", accountRef, "|",
                      "N/A", "|", "N/A", "|",
                      "N/A", "|"));
            }
          });
      chartOfAccounts.getAccountRefToAccountsMap().forEach(
          (accountRef, account) -> {
            List<Transaction> transactions = getTransferService()
                .findTransactionsByAccountRef(accountRef);
            if (!transactions.isEmpty()) {
              sb.append(
                  "\n\n" + String.format("%20s %20s %15s %4s %10s %10s %15s %5s", "Account", "|",
                      "Transaction Leg Ref", "|", "Amount", "|", "Currency", "|"));
              sb.append(String.format("%s",
                  "\n-------------------------------------------------------------------------------------------------------------------------"));
              transactions.forEach(transaction ->
                  transaction.getLegs().forEach(leg -> sb.append("\n" + String
                      .format("%20s %20s %10s %10s %10s %10s %10s %10s", accountRef, "|",
                          leg.getAccountRef(), "|", leg.getAmount().getAmount(), "|",
                          leg.getAmount().getCurrency(), "|"))));
            }
          });
    }
    return sb.toString();
  }

  @Override
  public String toString() {
    return formatAccounts() + "\n\n" + formatTransactionLog() + "\n\n";
  }
}
