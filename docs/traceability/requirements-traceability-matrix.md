# Requirements Traceability Matrix (RTM)

This matrix is a working traceability record based on the repository's current
test classes and stated bookkeeping behaviour. Confirm requirement wording
against the authoritative project specification before submission.

| Requirement ID | Requirement | Test case(s) / evidence | Test level | Status |
|---|---|---|---|---|
| REQ-MON-01 | Monetary values preserve correct monetary semantics | MoneyTest; MoneyAiTest | Unit | Existing tests; verify final run |
| REQ-TRF-01 | Numerically balanced transaction legs are accepted regardless of scale | DefectCandidateTest: balanced amounts without decimal places; different scales | Unit/regression | Fix and tests recorded; reverify |
| REQ-TRF-02 | Genuinely unbalanced transaction legs are rejected | DefectCandidateTest; TransferValidatorDecisionTableTest | Unit | Existing tests; verify final run |
| REQ-TRF-03 | Transfer request inputs are validated | TransferRequestAiTest; TransferValidatorImplAiTest; TransferValidatorCurrencyTest | Unit | Existing tests; verify final run |
| REQ-SVC-01 | Transfer service coordinates required collaborators | TransferServiceImplAiTest; TransferServiceInteractionTest | Unit | Existing tests; verify final run |
| REQ-ACC-01 | Account boundary behaviour is checked | AccountBoundaryTest | Unit | Existing test; verify final run |
| REQ-LED-01 | Ledger behaviour is checked | LedgerTest | Unit | Existing test; verify final run |
| REQ-INT-01 | Bank-level functional behaviour is checked | BankFunctionalTest; BankFactoryImplTest | Integration/functional | Existing tests; confirm scope |
| REQ-QUAL-01 | Automated tests run in CI | .github/workflows/ci.yml | CI | Workflow exists; current PIT gate requires repair |
| REQ-MUT-01 | Test effectiveness is assessed with mutation testing | evidence-archive/mutation/v1 and v2; docs/ai-test-engineer/mutant-triage.md | Mutation | Historical evidence; confirm commit provenance |
| REQ-SEC-01 | Static security findings are identified and triaged | Planned Semgrep scan; docs/security/ | Security | Pending execution |
| REQ-ACC-02 | Acceptance scenarios cover successful and rejected transfers | Planned BDD scenarios in docs/bdd/ | Acceptance | Specifications drafted; execution pending |

Update this matrix after running the final tests. Do not mark a test passed
solely because a test class exists.
