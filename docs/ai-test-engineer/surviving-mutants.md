# Surviving mutants - v1 (AI-only suite)

Source: `target/pit-reports/v1/mutations.xml`. v1 headline: 240/252 killed (95.2%), 12 survivors.
v2 (AI + manual): 251/252 killed (99.6%); the 12 original survivors -> 11 killed, 1 equivalent left.



| # | Class:line | PIT mutator | Mutation | Why the AI tests missed it | Manual test that kills it (verified by PIT v2) | Design technique |
|---|---|---|---|---|---|---|
| 1 | TransferValidatorImpl:95 | NonVoidMethodCallMutator | removed call to `Money::getCurrency` | Its balance tests used legs balanced in every currency or unbalanced in every currency. Merging all currencies into one bucket gives the same verdict, and it never tried legs that only cancel across currencies. | `ManualMutationKillTests.legsThatOnlyCancelAcrossDifferentCurrenciesAreUnbalanced` and `TransferValidatorCurrencyTest` (rules R1-R4) | Decision table (rule R3 below) |
| 2 | TransferServiceImpl:68 | EmptyObjectReturnValsMutator | `Lists.newArrayList()` replaced by `Collections.emptyList()` | It asserted `isEmpty()`, which an immutable empty list also satisfies. | `ManualMutationKillTests.noTransactionsReturnsAMutableEmptyList` | Error guessing (return-value contract) |
| 3 | TransferRequest:119 | MemberVariableMutator | removed `accountRef = null` in `amount()` | The builder forces `account(...)` before every amount, which overwrites the field, so the missing reset cannot be seen through the public API. | `ManualMutationKillTests.amountClearsAccountRefAfterAddingLeg` | White-box state check (reflection) |
| 4 | TransferRequest:127 | MemberVariableMutator | removed `accountRef = null` in `debit()` | same as #3 | `ManualMutationKillTests.debitClearsAccountRefAfterAddingLeg` | White-box state check (reflection) |
| 5 | TransferRequest:134 | MemberVariableMutator | removed `accountRef = null` in `credit()` | same as #3 | `ManualMutationKillTests.creditClearsAccountRefAfterAddingLeg` | White-box state check (reflection) |
| 6 | Transaction:45 | ArgumentPropagationMutator | `Collections.unmodifiableList(legs)` replaced by `legs` | It tested immutability on `TransferRequest.getLegs()` but never on `Transaction.getLegs()`. | `ManualMutationKillTests.transactionLegsCannotBeModifiedFromOutside` | Negative testing |
| 7 | Transaction:49 | NonVoidMethodCallMutator | removed call to `Date::getTime` | It only asserted `assertNotNull` on the date, never its value. | `ManualMutationKillTests.transactionKeepsTheExactTimestampItWasGiven` and `TransactionTimestampBoundaryTest` (-1, 0, 1, 1700000000000) | Boundary value analysis (the mutant returns epoch 0, so the value 0 alone would not detect it) |
| 8 | Money:97 | MathMutator | `*` replaced by `/` in `hashCode` | It tested only the hash contract (equal objects, different currencies), never the formula. | `ManualMutationKillTests.moneyHashCodeIsAmountHashTimes31PlusCurrency` | Exact-value oracle |

Also killed by the same hash test (not counted in the 8): Money:96 (`BigDecimal::hashCode` call removed), Money:97 (`31` to `32`), Money:97 (`+` to `-`).

## Equivalent mutant (excluded from the 8)

**TransferValidatorImpl:85, BigDecimalMutator, `add` replaced by `subtract`.**
The loop starts from `BigDecimal.ZERO` and computes `sum = sum.add(amount)`. With `subtract` the loop computes the negated total. The check is `sum.equals(new BigDecimal("0.00"))`, and the negation of zero is still `0.00` at the same scale, so the verdict is identical for every input. No test can tell the two versions apart. PIT v2 still reports it as SURVIVED, as expected.

## Decision table for TransferValidatorImpl balance check (supports #1)

Inputs: are the legs of each currency balanced on their own, and do all legs sum to zero across currencies?

| Rule | Each currency balanced? | Sum across all currencies zero? | Expected result | AI test? |
|---|---|---|---|---|
| R1 | yes, all | yes | accept | yes |
| R2 | no, at least one | no | reject | yes |
| R3 | no (USD +100.00, EUR -100.00) | yes | **reject** | **no: this is what the mutant hides** |
| R4 | yes, all | no | impossible (zero plus zero cannot be non-zero) | n/a |

## Candidates I did not use
All 12 original survivors are accounted for above (11 killed, 1 equivalent).
