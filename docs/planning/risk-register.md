# COMP441 Risk Register

| ID | Risk | Likelihood | Impact | Mitigation | Status |
|---|---|---|---|---|---|
| R1 | Monetary scale comparison rejects numerically balanced legs | Medium | High | Regression tests for different BigDecimal scales; compare numerically | Fix recorded; reverify |
| R2 | Weak tests allow faulty transfer validation to survive | High | High | PIT analysis, decision tables, survivor triage | Historical v1/v2 evidence |
| R3 | PIT baseline coverage fails despite Surefire passing | High | High | Diagnose test selection/runtime mismatch; rerun PIT independently | Open |
| R4 | Requirements lack explicit test-case traceability | Medium | Medium | Maintain RTM and update it with verified test IDs | RTM started |
| R5 | Security issues remain undetected without SAST | Medium | High | Run Semgrep, triage findings, remediate and rescan | Pending execution |
| R6 | Hard-coded CI service token is exposed | Medium | High | Revoke/rotate token; use repository secret; inspect Git history | Action required |
| R7 | Integration/acceptance behaviour is under-tested | Medium | High | Add scenarios for valid, invalid, and boundary transfers | Pending verification |
| R8 | Historical reports do not represent final source state | High | High | Record commit hashes and regenerate final reports | Archive preserved |
| R9 | Deadline prevents completion of all evidence | High | High | Prioritise reproducible results, missing required documents, and submission checks | Active |
