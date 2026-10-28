# TDD Evidence Log

## Feature 1 — Scale-insensitive transfer balancing

- **Date:** 27 October 2026
- **Branch:** `automation/ci-cd-engineering`
- **Component:** `TransferValidatorImpl`

### Requirement

Numerically balanced transaction legs must be accepted regardless of `BigDecimal` scale. Genuinely unbalanced legs must still be rejected.

### Red phase

Removed `@Ignore` from the original regression test. The test failed with `UnbalancedLegsException` at `TransferValidatorImpl.java:88` for balanced legs of `-5` and `5`.

Evidence: `/tmp/comp441-tdd-feature1-red.log`

### Green phase

Replaced the scale-sensitive comparison `sum.equals(new BigDecimal("0.00"))` with `sum.compareTo(BigDecimal.ZERO) != 0`.

Evidence: `/tmp/comp441-tdd-feature1-green.log`

### Regression testing

The full Maven suite passed after the production fix: 181 tests, zero failures, zero errors, zero skipped, and `BUILD SUCCESS`.

Evidence: `/tmp/comp441-feature1-final.log`

### Boundary cases

The final regression class covers:
1. Balanced amounts without decimal places.
2. Balanced amounts with different decimal scales.
3. Genuinely unbalanced amounts.

Confirm the final targeted test result before submission.

### Review

The production change is minimal and limited to numeric balance comparison. `git diff --check` reported no whitespace errors.

### Commit

Record the commit hash after the verified changes are committed.

## Final verification update — 28 October 2026

- Command: `mvn -B test`
- Result: **BUILD SUCCESS**
- Tests: 182 run, 0 failures, 0 errors, 0 skipped.
- JaCoCo generated a report for 36 classes during this test run.
- The three scale/balance regression tests in `DefectCandidateTest` passed.
- Log: `/tmp/comp441-final-test.log`

This updates the earlier 181-test historical entry; it does not replace it.
The PIT mutation goal remains unresolved: an earlier execution failed during
baseline coverage calculation. The newly archived v1/v2 reports are historical
evidence and must not be represented as results from this final test run.
The drafted BDD feature, threat model, and exploratory report are not evidence
that those activities have been executed.

## Final verification update — 28 October 2026

- Command: `mvn -B test`
- Result: **BUILD SUCCESS**
- Tests: 182 run, 0 failures, 0 errors, 0 skipped.
- JaCoCo generated a report for 36 classes during this test run.
- The three scale/balance regression tests in `DefectCandidateTest` passed.
- Log: `/tmp/comp441-final-test.log`

This updates the earlier 181-test historical entry; it does not replace it.
The PIT mutation goal remains unresolved: an earlier execution failed during
baseline coverage calculation. The newly archived v1/v2 reports are historical
evidence and must not be represented as results from this final test run.
The drafted BDD feature, threat model, and exploratory report are not evidence
that those activities have been executed.
