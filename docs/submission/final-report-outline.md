# COMP441 Software Analysis and Testing — Final Report

## 1. Introduction and objectives
Describe the system under test, project objectives, and scope.

## 2. Test strategy and plan
Summarise test levels, environment, entry/exit criteria, and risk-based priorities.
Refer to `docs/planning/test-plan.md` and `docs/planning/risk-register.md`.

## 3. Requirements traceability
Present the final RTM and explain any untested or partially tested requirements.

## 4. AI-generated baseline tests
Describe the prompts, generated test classes, strengths, weaknesses, and review.
Refer to `docs/ai-test-engineer/prompt-log.md` and `weak-tests-checklist.md`.

## 5. Manual/hybrid unit tests and TDD
Explain the defect hypothesis, red/green evidence, regression cases, and limitations.
Refer to `docs/TDD-LOG.md`.

## 6. Coverage and mutation testing
Report actual line/branch coverage and PIT v1/v2 results, with commit provenance.
Explain surviving mutants and prioritisation. Disclose the current PIT gate failure
unless resolved and rerun before submission.

## 7. Integration, acceptance, and BDD
Report executed tests only. Distinguish implemented tests from draft feature files.

## 8. Static analysis and security
Record tools, versions, commands, findings, triage, remediation, and rescan evidence.
Refer to `docs/static-analysis/` and `docs/security/threat-model.md`.

## 9. Performance, usability, and exploratory testing
Include measured results and session evidence. Identify work not completed.

## 10. CI/CD and quality gates
Describe workflow triggers, build/test/coverage gates, artifacts, and actual run status.
Do not describe a failing gate as passing.

## 11. Limitations and threats to validity
Discuss test selection, mutation limitations, untested behaviours, and evidence age.

## 12. Conclusion and recommendations
Summarise evidence-supported findings and remaining risks.

## Appendix
Include AI prompts/declaration, test summaries, defect triage, mutation charts,
static-analysis outputs, and other required evidence.
