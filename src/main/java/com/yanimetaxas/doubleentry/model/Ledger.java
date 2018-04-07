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

  private ChartOfAccounts chartOfAccounts;

  public Ledger(ChartOfAccounts chartOfAccounts) throws InfrastructureException {
    super("com.yanimetaxas.doubleentry.BankFactoryImpl", ConnectionOptions.NO_CONNECTION);
    this.chartOfAccounts = chartOfAccounts;
    createAccounts();
  }

  public Ledger(ChartOfAccounts chartOfAccounts, ConnectionOptions options)
      throws InfrastructureException {
    super("com.yanimetaxas.doubleentry.BankFactoryImpl", options);
    this.chartOfAccounts = chartOfAccounts;
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
    sb.append(String
        .format("%20s %20s %15s %10s %10s %10s %10s", "Account", "|", "Transaction", "|", "Type",
            "|", "Date"));
    sb.append(String.format("%s",
        "\n-------------------------------------------------------------------------------------------------------------------------"));

    chartOfAccounts.getAccountRefToAccountsMap().forEach(
        (accountRef, account) -> {
          List<Transaction> transactions = getTransferService()
              .findTransactionsByAccountRef(accountRef);
          transactions.forEach(transaction -> sb.append("\n" + String
              .format("%20s %20s %10s %15s %10s %10s %10s %1s", accountRef, "|",
                  transaction.getTransactionRef(), "|", transaction.getTransactionType(), "|",
                  transaction.getTransactionDate(), "|")));
        });
    chartOfAccounts.getAccountRefToAccountsMap().forEach(
        (accountRef, account) -> {
          List<Transaction> transactions = getTransferService()
              .findTransactionsByAccountRef(accountRef);
          sb.append("\n\n" + String.format("%20s %20s %15s %4s %10s %10s %15s %5s", "Account", "|",
              "Transaction Leg Ref", "|", "Amount", "|", "Currency", "|"));
          sb.append(String.format("%s",
              "\n-------------------------------------------------------------------------------------------------------------------------"));
          transactions.forEach(transaction ->
              transaction.getLegs().forEach(leg -> sb.append("\n" + String
                  .format("%20s %20s %10s %10s %10s %10s %10s %10s", accountRef, "|",
                      leg.getAccountRef(), "|", leg.getAmount().getAmount(), "|",
                      leg.getAmount().getCurrency(), "|"))));
        });

    return sb.toString();
  }

  @Override
  public String toString() {
    return formatAccounts() + "\n\n" + formatTransactionLog();
  }
}
