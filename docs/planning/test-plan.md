# COMP441 Test Plan

## 1. Purpose and scope
Evaluate the bookkeeping API through unit, integration, acceptance, mutation,
static-analysis, security, and exploratory testing.

## 2. Test objectives
- Verify monetary values and transaction-leg balancing.
- Verify transfer request validation and service interactions.
- Detect regressions in account and ledger behaviour.
- Assess test effectiveness using coverage and mutation testing.
- Identify security, reliability, and usability risks.

## 3. Test levels
| Level | Focus | Evidence |
|---|---|---|
| Unit | Money, request models, validators, service logic | JUnit reports |
| Integration | Interactions among service, ledger, and account components | Integration test reports |
| Acceptance | Externally observable transfer and ledger outcomes | Acceptance scenarios |
| Mutation | Tests' ability to kill injected faults | PIT v1/v2 reports |
| Static analysis | Maintainability and defect patterns | Tool output and triage |
| Security | Input handling, authorization assumptions, sensitive data | SAST output and threat model |
| Exploratory | Unscripted investigation of risky behaviours | Session report |

## 4. Approach
Use automated JUnit tests for repeatable checks. Use boundary and decision-table
tests for monetary values and validation. Use mutation testing to find weak
assertions. Record each defect with reproduction steps, impact, evidence,
remediation, and regression coverage.

## 5. Entry and exit criteria
Entry: source is available, Java/Maven environment is configured, and tests can
be executed.
Exit: required suites have recorded results; failures and surviving mutants are
triaged; coverage and mutation results are reproducible; unresolved risks are
documented. A failed quality gate must be reported as failed, not passed.

## 6. Environment and limitations
Record Java and Maven versions, commit hash, commands, and tool versions with
each result. Distinguish historical reports from reports generated from the
final commit. Any suite not executed remains pending.

## 7. Defect lifecycle
New -> Triaged -> Assigned -> Fixed -> Retested -> Closed.
Use Reopened when a regression remains. Severity and priority are recorded
separately.
