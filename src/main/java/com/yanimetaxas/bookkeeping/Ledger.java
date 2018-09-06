package com.yanimetaxas.bookkeeping;

import com.yanimetaxas.bookkeeping.exception.InfrastructureException;
import com.yanimetaxas.bookkeeping.exception.LedgerAccountException;
import com.yanimetaxas.bookkeeping.model.Account;
import com.yanimetaxas.bookkeeping.model.Money;
import com.yanimetaxas.bookkeeping.model.Transaction;
import com.yanimetaxas.bookkeeping.model.TransactionLeg;
import com.yanimetaxas.bookkeeping.model.TransferRequest;
import com.yanimetaxas.bookkeeping.model.TransferRequest.ReferenceStep;
import java.util.List;
import java.util.stream.Collectors;

/**
 * A general ledger contains all the accounts for recording transactions relating to assets,
 * liabilities, owners' equity, revenue, and expenses.
 *
 * @author yanimetaxas
 * @since 02-Feb-18
 */
public class Ledger extends AbstractAccountingConcept {

  private static final String FACTORY_CLASS_NAME = "com.yanimetaxas.bookkeeping.BankFactoryImpl";

  private String name;
  private ChartOfAccounts chartOfAccounts;

  private Ledger(LedgerBuilder builder) throws InfrastructureException {
    super(FACTORY_CLASS_NAME, builder.options);
    this.chartOfAccounts = builder.chartOfAccounts;
    this.name = builder.name;
  }

  public Ledger init() {
    chartOfAccounts.get().forEach(account -> {
      if (!account.getBalance().isNullMoney()) {
        getAccountService().createAccount(account.getAccountRef(), account.getBalance());
      }
    });
    return this;
  }

  public void commit(TransferRequest transferRequest) {
    validateAccountRefs(
        transferRequest.getLegs()
            .stream()
            .map(TransactionLeg::getAccountRef)
            .toArray(String[]::new)
    );
    getTransferService().transferFunds(transferRequest);
  }

  private void validateAccountRefs(String... accountRefs) {
    List<String> chartOfAccountsRefs = this.chartOfAccounts.get()
        .stream()
        .map(Account::getAccountRef)
        .collect(Collectors.toList());

    for (String ref : accountRefs) {
      if (!chartOfAccountsRefs.contains(ref)) {
        throw new LedgerAccountException(ref);
      }
    }
  }

  public ReferenceStep createTransferRequest() {
    return TransferRequest.builder();
  }

  public List<Transaction> findTransactions(String accountRef) {
    validateAccountRefs(accountRef);
    return getTransferService().findTransactionsByAccountRef(accountRef);
  }

  public Transaction getTransactionByRef(String transactionRef) {
    return getTransferService().getTransactionByRef(transactionRef);
  }

  public Money getAccountBalance(String accountRef) {
    validateAccountRefs(accountRef);
    return getAccountService().getAccountBalance(accountRef);
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

    chartOfAccounts.get().forEach(account -> {
      Money money = getAccountService().getAccountBalance(account.getAccountRef());
      sb.append("\n" + String
          .format("%20s %20s %10.2f %15s %10s %10s", account.getAccountRef(), "|",
              money.getAmount(), "|",
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

    chartOfAccounts.get().forEach(account -> {
      List<Transaction> transactions = getTransferService()
          .findTransactionsByAccountRef(account.getAccountRef());
      if (!transactions.isEmpty()) {
        transactions.forEach(transaction -> sb.append("\n" + String
            .format("%20s %20s %10s %15s %10s %10s %10s %1s", account.getAccountRef(), "|",
                transaction.getTransactionRef(), "|", transaction.getTransactionType(), "|",
                transaction.getTransactionDate(), "|")));
      } else {
        sb.append("\n" + String
            .format("%20s %20s %10s %15s %10s %10s %10s %1s", account.getAccountRef(), "|",
                "N/A", "|", "N/A", "|",
                "N/A", "|"));
      }
    });
    chartOfAccounts.get().forEach(account -> {
      List<Transaction> transactions = getTransferService()
          .findTransactionsByAccountRef(account.getAccountRef());
      if (!transactions.isEmpty()) {
        sb.append(
            "\n\n" + String.format("%20s %20s %15s %4s %10s %10s %15s %5s", "Account", "|",
                "Transaction Leg Ref", "|", "Amount", "|", "Currency", "|"));
        sb.append(String.format("%s",
            "\n-------------------------------------------------------------------------------------------------------------------------"));
        transactions.forEach(transaction ->
            transaction.getLegs().forEach(leg -> sb.append("\n" + String
                .format("%20s %20s %10s %10s %10s %10s %10s %10s", account.getAccountRef(), "|",
                    leg.getAccountRef(), "|", leg.getAmount().getAmount(), "|",
                    leg.getAmount().getCurrency(), "|"))));
      }
    });

    return sb.toString();
  }

  @Override
  public String toString() {
    return formatAccounts() + "\n\n" + formatTransactionLog() + "\n\n";
  }

  public static class LedgerBuilder {

    private String name = "General Ledger";
    private ChartOfAccounts chartOfAccounts;
    private ConnectionOptions options = ConnectionOptions.EMBEDDED_H2_CONNECTION;

    public LedgerBuilder(ChartOfAccounts chartOfAccounts) {
      this.chartOfAccounts = chartOfAccounts;
    }

    public LedgerBuilder name(String name) {
      this.name = name;
      return this;
    }

    public LedgerBuilder options(ConnectionOptions options) {
      this.options = options;
      return this;
    }

    public Ledger build() {
      return new Ledger(this);
    }
  }
}
