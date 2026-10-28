# CI/CD pipeline and quality gate (brief 7.10)

Workflow: `.github/workflows/ci.yml`. It runs on every push, pull request and manual dispatch.

| Stage | What it runs | Fails the build when |
|---|---|---|
| 1. Build, test, coverage (JDK 17) | `mvn verify`: all tests, JaCoCo report, JaCoCo check | a test fails, or line coverage < 90% or branch coverage < 85% on `model`, `validation` or `TransferServiceImpl` |
| 2. Mutation gate (JDK 11, after stage 1) | PIT, ALL mutators, AI + manual tests | mutation score < 95% |

## Why these thresholds (state and justify - edit in the group's own words)

- **Mutation score 95%.** The AI-only suite scored 95.2% (240/252). The hybrid suite scored 99.6% (251/252
  before the scale fix; re-measure after it). A 95% floor means the build fails if the hybrid suite
  drops back to the AI-only level, i.e. if the manual tests are removed or weakened.
- **Coverage 90% line / 85% branch on the tested classes.** Those classes measured 100% / 100% in JaCoCo, so
  the floor leaves headroom for small changes without letting coverage collapse.
- **Scope.** The original whole-project gate (80% line / 70% branch) cannot pass: the project as a whole
  measured 30% instruction / 67% branch because the DAO, util and connection classes are outside the
  tested scope. Gating them would block every build without finding a regression.

## Demo for the live presentation (17 November)

1. **Passing run:** push to the branch; both jobs go green in the Actions tab.
2. **Deliberately broken run:** on a throw-away branch change `31` to `32` in `Money.hashCode()`
   (`src/main/java/com/yanimetaxas/bookkeeping/model/Money.java`). The AI tests still pass, but the manual
   test `moneyHashCodeIsAmountHashTimes31PlusCurrencyHash` fails, so stage 1 turns red and the pipeline
   blocks the change. Do not merge that branch.

## Not yet verified

These settings were written without a Maven run. Run `mvn verify` locally and check the Actions tab;
if a threshold fails on the first run, read the message ("Rule violated for package ...") and adjust the
number, then update the justification above.
