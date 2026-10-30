package com.yanimetaxas.bookkeeping.validation;

import static io.github.imetaxas.realitycheck.RealityAssertions.assertThatThrownBy;

import com.yanimetaxas.bookkeeping.exception.AccountNotFoundException;
import com.yanimetaxas.bookkeeping.model.Account;
import com.yanimetaxas.bookkeeping.model.Money;
import com.yanimetaxas.bookkeeping.model.TransactionLeg;
import com.yanimetaxas.bookkeeping.model.TransferRequest;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class TransferValidatorTest {

  private final TransferValidatorImpl validator = new TransferValidatorImpl();

  @Test
  void aRequestWithoutAReferenceIsRejected() {
    TransferRequest request =
        TransferRequest.builder()
            .reference(null)
            .type("sale")
            .account("CASH")
            .debit("1.00", "EUR")
            .account("REVENUE")
            .credit("1.00", "EUR")
            .build();

    assertThatThrownBy(() -> validator.validateTransferRequest(request))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void aRequestWithoutATypeIsRejected() {
    TransferRequest request =
        TransferRequest.builder()
            .reference("T1")
            .type(null)
            .account("CASH")
            .debit("1.00", "EUR")
            .account("REVENUE")
            .credit("1.00", "EUR")
            .build();

    assertThatThrownBy(() -> validator.validateTransferRequest(request))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void aRequestWithOneLegIsRejected() {
    TransferRequest request =
        TransferRequest.builder()
            .reference("T1")
            .type("sale")
            .account("CASH")
            .debit("1.00", "EUR")
            .build();

    assertThatThrownBy(() -> validator.validateTransferRequest(request))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void currenciesMatchRejectsAMissingAccount() {
    TransactionLeg leg = new TransactionLeg("CASH", Money.toMoney("-1.00", "EUR"));

    assertThatThrownBy(() -> validator.currenciesMatch(List.of(leg), Map.of()))
        .isInstanceOf(AccountNotFoundException.class);
    assertThatThrownBy(
            () -> validator.currenciesMatch(List.of(leg), Map.of("CASH", Account.nullAccount())))
        .isInstanceOf(AccountNotFoundException.class);
  }

  @Test
  void validBalanceRejectsAMissingAccount() {
    TransactionLeg leg = new TransactionLeg("CASH", Money.toMoney("-1.00", "EUR"));

    assertThatThrownBy(() -> validator.validBalance(List.of(leg), Map.of()))
        .isInstanceOf(AccountNotFoundException.class);
    assertThatThrownBy(
            () -> validator.validBalance(List.of(leg), Map.of("CASH", Account.nullAccount())))
        .isInstanceOf(AccountNotFoundException.class);
  }
}
