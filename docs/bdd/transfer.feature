Feature: Transfer validation and bookkeeping

  Background:
    Given the bookkeeping test environment is initialized

  Scenario: Accept transaction legs that balance at different decimal scales
    Given a transfer has transaction legs whose numeric sum is zero
    And the legs use different decimal scales
    When the transfer is validated
    Then the transfer should pass balance validation

  Scenario: Reject genuinely unbalanced transaction legs
    Given a transfer has transaction legs whose numeric sum is not zero
    When the transfer is validated
    Then the transfer should be rejected as unbalanced

  Scenario: Reject an invalid transfer request
    Given a transfer request violates a required validation rule
    When the transfer is submitted for validation
    Then the request should be rejected with the appropriate validation outcome

  Scenario: Complete a valid transfer
    Given a valid transfer request between eligible accounts
    When the transfer is processed
    Then the service should report the appropriate successful outcome
    And the resulting bookkeeping entries should satisfy the ledger invariants
