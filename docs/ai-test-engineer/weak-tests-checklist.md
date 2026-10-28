# Flagging weak AI tests (Section 7.4, bullet 2)

Categories: A Meaningful - B No-oracle (only "does not throw") - C Loose assertion - D Tautology (asserts what the mock was told to return) - E Missing boundary.

## 1. Coverage of the AI suite alone (JaCoCo 0.8.8, 122 AI tests)

Run: `mvn test -Dtest=MoneyAiTest,TransferRequestAiTest,TransferServiceImplAiTest,TransferValidatorImplAiTest` (screenshots in `docs/ai-test-engineer/evidence/`).

| Scope | Instruction (statement) coverage | Branch coverage |
|---|---|---|
| `model` package (in scope) | 100% | 100% (0 of 48 missed) |
| `validation` package (in scope) | 100% (0 of 205 missed) | 100% (0 of 28 missed) |
| `TransferServiceImpl` (in scope) | 100% | 100% |
| `service` package as a whole | 70% | 60% |
| Whole project (all 36 classes) | 30% (1,585 of 2,277 missed) | 67% (26 of 80 missed) |

How to read this:
- The three classes the AI was asked to test have **100% statement and 100% branch coverage**, yet PIT found 12 survivors (240/252, 95.2%). Coverage shows code was executed, not that results were checked.
- The low project total is not a defect in the AI tests. The prompts only covered `model`, `validation` and `TransferServiceImpl`. Everything else (`Ledger`, `dao`, `util`, `AccountServiceImpl`, connection classes) is 0% because it was out of scope and the functional tests were excluded from this run.

## 2. Weak tests found

| Test | Cat. | Why it is weak | What would strengthen it |
|---|---|---|---|
| `TransferValidatorImplAiTest.isTransactionBalancedShouldAcceptBalancedUsdLegs` | B | No assertion; passes if nothing is thrown. | Also test a rejected variant (it did, elsewhere) and assert on the exact exception cases. |
| `TransferValidatorImplAiTest.isTransactionBalancedShouldAcceptSeveralBalancedCurrencies` | B / E | Only "does not throw". Never tests legs that cancel across currencies, which hid the Validator:95 survivor. | Add the cross-currency case (USD +100, EUR -100 must be rejected). |
| `TransferValidatorImplAiTest.isTransactionBalancedShouldAcceptMultipleLegsSummingToZero` | B | "Does not throw" only. | Same as above. |
| `TransferValidatorImplAiTest.validateTransferRequestShouldAcceptCompleteRequest` | B | No oracle. | Assert the request is unchanged or the call is side-effect free. |
| `TransferServiceImplAiTest.transferFunds_validRequest_updatesBalancesValidatesAndStoresTransaction` | B | Contains `assertNotNull(stored.getTransactionDate())`, which cannot fail. The survivor Transaction:49 passed because of this. | Assert the exact timestamp, not just non-null. |
| `TransferServiceImplAiTest.findTransactionsByAccountRef_noTransactionReferencesReturnsEmptyList` | C | `assertTrue(result.isEmpty())` is satisfied by any empty list, including an immutable one (Service:68 survivor). | Also assert the list can be modified. |
| `TransferRequestAiTest.hashCodeShouldAccountForCurrency` | C / E | `assertNotEquals` on two hashes is not guaranteed by the hash contract and never checks the formula (all four Money hash survivors). | Assert the exact expected hash value. |
| `TransferRequestAiTest.hashCodeShouldBeConsistentForEqualMoney` and `MoneyAiTest.equalsReturnsTrueForEqualMoney` | C | Contract only; any consistent formula passes. | Same as above. |
| `TransferServiceImplAiTest.getTransactionByRef_returnsTransactionFromDao` | D | Asserts that the mock returns what it was stubbed to return; this only checks delegation. | Acceptable only as a delegation test; do not count it as behaviour coverage. |
| `TransferServiceImplAiTest.getTransactionByRef_nullReferenceIsPassedToDao` and `..._daoFailurePropagates` | D | Same: pure pass-through of a mock. | Same. |
| `TransferRequestAiTest.transactionLeg*` and `Money*` tests | duplicate | These repeat tests already in `MoneyAiTest`, which inflates the test count (122) without adding strength. | Remove duplicates or merge. |
| `Transaction.getLegs()` / `getTransactionDate()` | E | No test at all on immutability or the timestamp value, so two mutants survived. | See manual tests. |

## 3. Notes on the AI output (for the prompt log)

- Five generated tests were disabled (commented out) because they did not compile or failed on the real behaviour: `builderShouldReplaceAccountWhenAccountIsCalledAgain`, `toMoneyShouldRejectNullAmountString`, `builderShouldNotEnforceMinimumLegCount`, `validateTransferRequestShouldRejectEmptyRequest`, `isTransactionBalancedShouldRejectDifferentBigDecimalScale`. Each shows the AI guessed behaviour wrongly.
- Editor warnings in the AI files are code-style issues, not weak assertions: `TransferValidatorImplAiTest` lines 18 and 21 are unused imports (`BigDecimal`, `Currency`); `TransferServiceImplAiTest` lines 356, 367 and 376 use `anyList()`, which is a raw type and gives an unchecked warning. The AI files were left unedited so the v1 measurement stays fair.
