# Mutant triage - PIT v1 survivors (AI-only suite)

Section 7.4 / 7.5 of the brief. This is the step between "PIT found 12 survivors" and "here are the
8 we explain and kill": every survivor is classified before any test is written.

- **Input:** the 12 SURVIVED mutants in `target/pit-reports/v1-jdk11/mutations.xml` (also `survivors.txt`).
  Baseline: 238/250 killed = 95.2% (`v1-jdk11` has the same bytecode as v2, so v1 and v2 are comparable).
- **Result:** `target/pit-reports/v2/mutations.xml`: 249/250 killed = **99.6%**. The one survivor is the
  equivalent mutant in row 1.

## Triage method

For each survivor, read the source line and ask if *there any input for which the original and the
mutated code give a different observable result?*

| Verdict | Meaning | Action |
|---|---|---|
| **Killable** | Some input or call sequence exposes the difference through the public API. | Write a test using a named design technique; confirm PIT v2 kills it. |
| **Killable, white-box only** | The difference sits in a private field the public API never exposes. | Kill by reflection if wanted, and say so in the report; a marker may call this an implementation test. |
| **Killable, weak contract** | The difference is real but nothing documents it as required behaviour. | Kill it only if you can defend the contract; otherwise report it as "not worth killing". |
| **Equivalent** | No input can tell the two versions apart. | Do not write a test. Explain the reasoning and exclude it from the "killable" score. |

## Triage of all 12

| # | Location | PIT mutator / mutation | Verdict | Reasoning from the source | Killed by (PIT v2) |
|---|---|---|---|---|---|
| 1 | `TransferValidatorImpl:85` | BigDecimalMutator: `add` replaced by `subtract` | **Equivalent** | `sum` starts at `BigDecimal.ZERO` and the check is `sum.equals(new BigDecimal("0.00"))`. Subtracting gives exactly the negated total at the same scale, and the negation of zero is zero, so the verdict is identical for every input. | not killable; still SURVIVED in v2 (expected) |
| 2 | `TransferValidatorImpl:95` | NonVoidMethodCallMutator: removed `Money::getCurrency` | **Killable** | The call is the key in `sortByCurrency`. Without it every leg goes into one bucket, so legs that cancel only across currencies (USD +100, EUR -100) would be accepted. | `legsThatOnlyCancelAcrossDifferentCurrenciesAreUnbalanced` |
| 3 | `Money:96` | NonVoidMethodCallMutator: removed `BigDecimal::hashCode` | **Killable** (exact-value only) | The hash changes, but only a test that checks the actual value sees it; the contract (equal objects, equal hashes) still holds. | `moneyHashCodeIsAmountHashTimes31PlusCurrencyHash` |
| 4 | `Money:97` | InlineConstantMutator: `31` to `32` | **Killable** (exact-value only) | Same as #3. | same hash test |
| 5 | `Money:97` | MathMutator: `*` to `/` | **Killable** (exact-value only) | Same as #3. | same hash test |
| 6 | `Money:97` | MathMutator: `+` to `-` | **Killable** (exact-value only) | Same as #3. | same hash test |
| 7 | `TransferServiceImpl:68` | EmptyObjectReturnValsMutator: `Lists.newArrayList()` to `Collections.emptyList()` | **Killable, weak contract** | An empty result looks the same either way. Only mutability differs (`add` works on the original, throws on the immutable list). The code does not document the list as modifiable, so this is defensible but contestable. | `noTransactionsReturnsAMutableEmptyList` |
| 8 | `TransferRequest:119` | MemberVariableMutator: removed `accountRef = null` in `amount()` | **Killable, white-box only** | After `amount()` the fluent interface forces `account(...)`, which overwrites the field, so no public call sequence observes the missing reset. | `amountClearsAccountRefAfterAddingLeg` (reflection) |
| 9 | `TransferRequest:127` | same, in `debit()` | **Killable, white-box only** | same as #8 | `debitClearsAccountRefAfterAddingLeg` (reflection) |
| 10 | `TransferRequest:134` | same, in `credit()` | **Killable, white-box only** | same as #8 | `creditClearsAccountRefAfterAddingLeg` (reflection) |
| 11 | `Transaction:45` | ArgumentPropagationMutator: `Collections.unmodifiableList(legs)` replaced by `legs` | **Killable** | Callers could modify a stored transaction's legs. The AI tested immutability only on `TransferRequest`. | `transactionLegsCannotBeModifiedFromOutside` |
| 12 | `Transaction:49` | NonVoidMethodCallMutator: removed `Date::getTime` | **Killable** | The copy is built from a default value, so `getTransactionDate()` returns the wrong date. The AI asserted only `assertNotNull`. A test must use a non-zero timestamp, because zero is what the mutant returns. | `transactionKeepsTheExactTimestampItWasGiven` |

Totals: 1 equivalent, 3 white-box only, 1 weak contract, 7 plainly killable.
PIT v2 result: 11 of 12 killed; the remaining one is the equivalent mutant (#1).

## Which 8 go in `surviving-mutants.md`, and why

The brief asks for at least 8 survivors  and the manual test. The table in
`surviving-mutants.md` uses #2, #7, #8, #9, #10, #11, #12 and one representative of the hash group
(#5). Selection rules that were used:

1. Exclude the equivalent mutant (#1): there is no test to show.
2. Take every mutant from a different code location, so the eight reasons are different.
3. Count the four `hashCode` mutants as one group (they share one test) and use one in the table; the
   others are listed beside it as "also killed by the same test".
4. Keep the white-box mutants (#8-#10) but label them honestly, as above.

## Triage history (what changed after reading the source)

An earlier draft triage was written from the PIT mutator names alone, before the source was visible.
It was corrected once the lines were read:

| # | First guess | After reading the source |
|---|---|---|
| 1 | "killable, strong candidate" | equivalent (negated sum, same scale) |
| 2 | "possibly equivalent" | killable (all legs fall into one currency bucket) |
| 7 | "killable, easy" | killable only through list mutability (weak contract) |
| 12 | "possibly equivalent" | killable with a non-zero timestamp |

This is evidence that triage was done by reasoning about the code, not
by trusting the tool's labels.

## Traceability note (Section 7.5 / RTM)

| Manual test | Technique | Mutants | Code under test |
|---|---|---|---|
| `legsThatOnlyCancelAcrossDifferentCurrenciesAreUnbalanced` | Decision table (rule R3 in `surviving-mutants.md`) | #2 | `TransferValidatorImpl.isTransactionBalanced` |
| `moneyHashCodeIsAmountHashTimes31PlusCurrencyHash` | Exact-value oracle derived from the hash formula | #3, #4, #5, #6 | `Money.hashCode` |
| `noTransactionsReturnsAMutableEmptyList` | Error guessing (return-value contract) | #7 | `TransferServiceImpl.findTransactionsByAccountRef` |
| `amountClearsAccountRefAfterAddingLeg`, `debitClears...`, `creditClears...` | White-box state check (reflection) | #8, #9, #10 | `TransferRequest.Builder` |
| `transactionLegsCannotBeModifiedFromOutside` | Negative testing | #11 | `Transaction.getLegs` |
| `transactionKeepsTheExactTimestampItWasGiven` | Exact-value oracle with a non-zero value | #12 | `Transaction.getTransactionDate` |
| `TransferValidatorDecisionTableTest`, `AccountBoundaryTest`, `TransferServiceInteractionTest` | Decision table, boundary value analysis, interaction testing | coverage and manual-code-reading gaps, not v1 survivors | validator, account, service |
| `DefectCandidateTest` (`@Ignore`d) | Boundary: scale of a `BigDecimal` | defect candidate (log in defect density) | `TransferValidatorImpl:85` area |

