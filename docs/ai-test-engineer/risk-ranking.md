# Risk-based test ranking (brief 7.4, bullet 5)

Produced with `python docs/ai-test-engineer/scripts/prioritise.py` (run from the repo root, needs the git history).
Formula: `risk = 0.5 * impact/5 + 0.3 * complexity/max + 0.2 * churn/max`
- **impact** (1-5): judgement. 5 = enforces a money invariant (balance, overdraft) or runs the transfer.
- **complexity**: count of decision points (`if`, `for`, `while`, `case`, `&&`, `||`, `?`) in the source file.
- **churn**: number of commits that touched the file (`git log --follow`). In this repo it only ranges from 3 to 5, so it barely separates the classes.

## Script output

| Rank | Class | Impact | Complexity | Churn | Risk |
|---|---|---|---|---|---|
| 1 | validation/TransferValidatorImpl | 5 | 16 | 4 | 0.960 |
| 2 | service/TransferServiceImpl | 5 | 3 | 4 | 0.716 |
| 3 | model/Money | 3 | 8 | 5 | 0.650 |
| 4 | model/TransferRequest | 3 | 4 | 5 | 0.575 |
| 5 | model/Account | 4 | 0 | 3 | 0.520 |
| 6 | model/TransactionLeg | 2 | 2 | 3 | 0.358 |

Known limitation: `Account` has complexity 0 because its only decision is the comparison `balance < 0`, which the regex does not count. Its impact (4) is high (every transfer uses the overdraft rule), so treat it as rank 3 in practice.
`Transaction` (two PIT survivors in v1) is not in the script's table; it is a low-impact data holder, so it stays at the end.

## Execution order proposed for CI (fast, high-risk first)

| Order | Test classes | Why first |
|---|---|---|
| 1 | `TransferValidatorImplAiTest`, `TransferValidatorCurrencyTest`, `TransferValidatorDecisionTableTest`, `ManualMutationKillTests` (validator part) | Highest risk: a regression here lets unbalanced or overdrawn transfers through. Pure unit tests, run in milliseconds. |
| 2 | `TransferServiceImplAiTest`, `TransferServiceInteractionTest` | Orchestrates balance updates and storage; a mistake corrupts balances. Mock-based, fast. |
| 3 | `AccountBoundaryTest`, `MoneyAiTest`, `MoneyTest`, `TransferRequestAiTest`, the builder tests in `ManualMutationKillTests`, `TransactionTimestampBoundaryTest` | Lower impact value objects; still fast. |
| 4 | `LedgerTest`, `BankFunctionalTest` (upstream, real in-memory databases) | Slowest (database start-up) and failures there are usually caused by a unit-level fault already caught in stages 1-3. Run last, but always run them. |

## Why this reduces time-to-detect a regression in CI
- A failure in stage 1 or 2 stops the pipeline within seconds, before the multi-second database tests start, so the developer gets the most likely and most expensive failure first.
- The ranking follows where faults hurt most (money invariants), not alphabetical order.
- Implement it as two CI stages: a "fast unit" job (stages 1-3) and a "database" job (stage 4) that only starts if the first passes. Optionally add Surefire `<runOrder>failedfirst</runOrder>` so tests that failed last time run first.

## Honest limits To be communicated as a limitation in the report
- The ranking is a reasoned argument, **not a measurement**: nobody timed time-to-first-failure with and without it.
- The formula is a heuristic: the impact scores are judgement and churn carries almost no information in a 67-commit, 2018-era repository.
