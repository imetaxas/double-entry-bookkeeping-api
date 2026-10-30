package com.yanimetaxas.bookkeeping.memory;

import static io.github.imetaxas.realitycheck.RealityAssertions.assertThat;

import com.yanimetaxas.bookkeeping.model.Money;
import com.yanimetaxas.bookkeeping.model.TransactionLeg;
import java.util.List;
import org.junit.jupiter.api.Test;

class InMemoryDaoEdgeTest {

  @Test
  void updateBalanceSkipsAMissingAccount() {
    InMemoryLedgerData data = new InMemoryLedgerData("client");
    InMemoryAccountDao accounts = new InMemoryAccountDao(data);

    accounts.updateBalance(new TransactionLeg("MISSING", Money.toMoney("1.00", "EUR")));

    assertThat(accounts.getAccounts()).isEmpty();
  }

  @Test
  void getTransactionsSkipsAMissingReference() {
    InMemoryLedgerData data = new InMemoryLedgerData("client");

    assertThat(new InMemoryTransactionDao(data).getTransactions(List.of("ABSENT"))).isEmpty();
  }
}
