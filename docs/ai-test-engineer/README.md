# AI Test Engineer workspace (Section 7.4 - 7.5 of the project brief)

Prerequisites: JDK 11 (PIT needs 11+ to run; project compiles to Java 8), Maven 3.6+, Python 3 + matplotlib.
Windows tip: `git config core.autocrlf true` (the upstream repo is LF, Windows checkouts show every file modified otherwise).

## Order of work 
1. `mvn test`  -> upstream baseline ("before AI"). Save the console summary.
2. **Fresh chat, fresh context** in your chosen AI tool. Paste prompt-inputs/P1..P4 one by one. Save the raw transcript + fill prompt-log.md.
   Put the generated classes in `src/test/java/com/yanimetaxas/bookkeeping/ai/`. Do not hand-edit them (record any deletions).
   Commit: "AI baseline v1 (unedited)".
3. Coverage of the AI suite only:  `mvn clean test -Dtest='com.yanimetaxas.bookkeeping.ai.*' ` then open `target/site/jacoco/index.html` (statement + branch). Fill weak-tests-checklist.md.
4. PIT v1:  `mvn test-compile org.pitest:pitest-maven:mutationCoverage`   -> `target/pit-reports/v1/` . Commit the report folder (`git add -f target/pit-reports/v1`).
5. Pick 8 survivors from the HTML report; fill surviving-mutants.md. Then `./docs/ai-test-engineer/apply-manual-tests.sh`, adapt tests to your 8, commit.
6. `python3 docs/ai-test-engineer/scripts/prioritise.py` -> risk ranking for the report.
7. PIT v2:  `mvn test-compile org.pitest:pitest-maven:mutationCoverage -Dpit.targetTests="com.yanimetaxas.bookkeeping.ai.*,com.yanimetaxas.bookkeeping.manual.*" -Dpit.reportsDir=target/pit-reports/v2`
8. `python3 docs/ai-test-engineer/scripts/mutation_chart.py target/pit-reports/v1/mutations.xml target/pit-reports/v2/mutations.xml`

# AI Test Engineer - how to run, reproduce and check everything (COMP 441, Track B)

Everything below can be typed into the **PowerShell terminal in VS Code** (Terminal > New Terminal), opened in the project folder.
No prior knowledge needed: read top to bottom; each block says what it does and what you should see.

## 0. What was done, in one table

| Stage | Result | Evidence in the repo |
|---|---|---|
| AI baseline tests (4 files, 122 tests) | all pass | `src/test/java/.../ai/`, `prompt-log.md`, `prompt-inputs/` |
| Coverage of the AI suite | whole project 30% statement / 67% branch; the 3 targeted classes 100% / 100% | `evidence/jacoco-*.png`, `weak-tests-checklist.md` |
| PIT v1 (AI tests only, ALL mutators) | 240/252 killed = **95.2%** (12 survivors) | `target/pit-reports/v1/` |
| PIT v2 (AI + manual tests) | 251/252 killed = **99.6%** (1 survivor, equivalent) | `target/pit-reports/v2/` |
| Survivors explained and killed | 12 survivors: 11 killed by manual tests, 1 equivalent | `surviving-mutants.md`, `src/test/.../manual/` |
| Risk ranking | script output and CI order | `risk-ranking.md` |
| Chart | v1 -> v2 | `mutation_v1_v2.png` |

Both runs generate the same 252 mutants (same code, same JDK 11, same mutators), so v1 and v2 are directly comparable: 95.2% -> 99.6%, +4.4 points.
(A run made earlier in a different environment produced 250 mutants; ignore it and do not mix its numbers with these.)

## 1. One-time setup (every new terminal window)

PIT needs Java 11+, but the project and its libraries were written for Java 8. Use **JDK 11**.
Download Temurin 11 from adoptium.net if `java -version` does not already say 11.

```powershell
# Point this terminal at Java 11 (derive the folder from the java.exe that runs; do not type a version number by hand)
$env:JAVA_HOME = Split-Path (Split-Path (Get-Command java).Source)
# Put Maven on the PATH for this terminal (change the folder if yours is different)
$env:Path = "C:\Users\user\OneDrive - biust.ac.bw\Desktop\programe files\Apache\apache-maven-3.9.16\bin;" + $env:Path
java -version     # must say 11
mvn -v            # must say Maven 3.9.x and Java version 11
git config core.autocrlf true     # once: stops Windows line endings showing every file as modified
python --version; pip install matplotlib    # once, for the chart
```

If `java -version` says 25 (or anything not 11) the first command picked the wrong Java: install JDK 11 and make sure its `bin` folder is first on `$env:Path`.

## 2. Check the project builds and the original tests pass ("before AI")

```powershell
mvn clean test
```
Expect `BUILD SUCCESS`. Save the summary lines (`Tests run: ...`) as evidence (see `docs/00-upstream-baseline.txt`).
(Without the JAXB dependency in `pom.xml` this fails on Java 11+ with `NoClassDefFoundError: javax/xml/bind/JAXBException`. See Troubleshooting.)

## 3. AI baseline tests (what was done, how to repeat)

1. Open a **fresh** chat in your AI tool (no project context). Note tool name, model and date in `prompt-log.md`.
2. Open `docs/ai-test-engineer/prompt-inputs/P1_model_money_account_leg.txt`, copy everything, send it. Repeat for P2, P3, P4 (one message each).
3. Save each answer as a class in `src/test/java/com/yanimetaxas/bookkeeping/ai/` (e.g. `MoneyAiTest.java`). Do not improve the tests; fix only what stops them compiling and log every edit and deletion in `prompt-log.md`.
4. Run only the AI tests (the path style with slashes matters, see Troubleshooting):

```powershell
mvn clean test "-Dtest=com/yanimetaxas/bookkeeping/ai/*" "-Dsurefire.failIfNoSpecifiedTests=false"
```
Expect `Tests run: 122, Failures: 0, Errors: 0`.

## 4. Coverage and weak tests

The command in step 3 also writes the JaCoCo report. Open `target\site\jacoco\index.html` in a browser (in VS Code: right-click the file > Open with Live Server, or just double-click it in File Explorer).
Read the **Cov.** column for instructions (statements) and for branches, per package, and take screenshots into `docs/ai-test-engineer/evidence/`.
Then go through every AI test with `weak-tests-checklist.md` (categories B-E) and fill in its table.
Note: in the JaCoCo table, `Cxty` is cyclomatic complexity, **not** branches. Branch numbers are in the "Missed Branches" column.

## 5. PIT mutation testing

PIT changes ("mutates") the production code in small ways, then reruns the tests. A mutant the tests catch is KILLED; one they do not catch SURVIVED.
Score = killed / generated. Configuration lives in the `pitest-maven` block of `pom.xml` (scope = `model`, `validation`, `TransferServiceImpl`; mutators = `ALL`).

### 5a. Reproduce v1 (AI tests only) without touching your current work
The commit `e0eeb0e` ("PIT v1 report with ALL mutators (AI-only)") contains exactly the v1 configuration. Check it out in a separate folder:

```powershell
git worktree add ..\v1-check e0eeb0e
cd ..\v1-check
mvn clean test-compile org.pitest:pitest-maven:mutationCoverage
# report: target\pit-reports\v1\index.html   (expect 240/252 = 95.2% with 12 survivors)
cd ..\double-entry-bookkeeping-api-Comp-441-group-Project-
git worktree remove ..\v1-check --force
```

### 5b. Run v2 (AI + manual tests) in the current folder
`pom.xml` here already lists both test packages and writes to `target/pit-reports/v2`:

```powershell
mvn clean test-compile org.pitest:pitest-maven:mutationCoverage
# open target\pit-reports\v2\index.html  (expect 251/252 = 99.6%)
```

### 5c. List the survivors of any report
```powershell
[xml]$x = Get-Content target\pit-reports\v2\mutations.xml
$x.mutations.mutation | Where-Object { $_.status -eq 'SURVIVED' } | ForEach-Object { "$($_.sourceFile):$($_.lineNumber) | $($_.mutator.Split('.')[-1]) | $($_.description)" }
```
(change `v2` to `v1` to see the 12 v1 survivors). In the HTML report, survivors are the red lines in each class's source view.

## 6. Manual tests (the "killers")

Located in `src/test/java/com/yanimetaxas/bookkeeping/manual/`. Each of the 12 v1 survivors is explained and mapped to a test in `surviving-mutants.md`.
Run only them:
```powershell
mvn test "-Dtest=com/yanimetaxas/bookkeeping/manual/*" "-Dsurefire.failIfNoSpecifiedTests=false"
```
Techniques used: decision table (`TransferValidatorCurrencyTest`, `TransferValidatorDecisionTableTest`), boundary values (`TransactionTimestampBoundaryTest`, `AccountBoundaryTest`),
state checks (`ManualMutationKillTests`), exact-value oracles (hash code, timestamp).
`DefectCandidateTest` is `@Ignore`d on purpose: remove the `@Ignore` line to reproduce a real defect (`BigDecimal.equals` is scale-sensitive, so legs "5" and "-5" are wrongly rejected as unbalanced).

**Try a mutant yourself** (to see a test fail for the right reason): in `src/main/java/.../model/Account.java` change `< 0.0` to `<= 0.0`, run the manual tests, watch `AccountBoundaryTest` fail, then undo the change with `git checkout -- src/main`.

## 7. Risk ranking and chart

```powershell
python docs/ai-test-engineer/scripts/prioritise.py
python docs/ai-test-engineer/scripts/mutation_chart.py target/pit-reports/v1/mutations.xml target/pit-reports/v2/mutations.xml
```
Explanation of the ranking: `risk-ranking.md`. The chart is saved as `mutation_v1_v2.png` in the folder where you run the command (move it into `docs/ai-test-engineer/`).

## 8. Saving your work to git

```powershell
git status
git add -A src docs pom.xml
git add -f target/pit-reports/v1 target/pit-reports/v1-defaults target/pit-reports/v2   # -f because target/ is git-ignored; the reports are evidence
git commit -m "AI Test Engineer: manual tests, PIT v1/v2 reports, risk ranking, docs"
git push origin feature/ai-test-engineer
```
Do **not** commit `derby.log` (an automatic log file from the database tests). Add the line `derby.log` to `.gitignore` or delete the file.

## 9. Troubleshooting (every problem hit during this work)

| Message | Cause | Fix |
|---|---|---|
| `NoClassDefFoundError: javax/xml/bind/JAXBException` (25 of 36 upstream tests error) | Java removed JAXB in version 11; Hibernate 5.2 needs it | Add `jaxb-api` and `jaxb-runtime` 2.3.1 to `pom.xml` `<dependencies>` (already done here) |
| `The JAVA_HOME environment variable is not defined correctly` | JAVA_HOME points at a folder that does not exist (typed the wrong version) | Use the `Split-Path (Get-Command java)` line from section 1 |
| `mvn : The term 'mvn' is not recognized` | Maven is not on the PATH of this terminal | Re-run the `$env:Path = ...` line (it lasts only for that terminal window) |
| `java -version` still says 25 | Java 25 is first on the PATH | Put JDK 11's `bin` first, or reopen the terminal and redo section 1 |
| `Unsupported class file major version 69` (long JaCoCo stack traces) | JaCoCo 0.8.8 cannot read Java 25 classes | Use JDK 11 |
| `mvn test -Dtest=...ai.*` runs 0 tests, JaCoCo says "missing execution data file" | Surefire does not read a dotted package wildcard | Use slashes: `"-Dtest=com/yanimetaxas/bookkeeping/ai/*"` |
| PowerShell splits `-Dtest=a,b` or `-D...` oddly | PowerShell treats some characters specially | Wrap each `-D...` argument in double quotes |
| The v1 report folder disappeared after `mvn clean` | `clean` deletes the whole `target` folder, including reports | Restore committed reports: `git checkout -- target/pit-reports/v1 target/pit-reports/v1-defaults`. Commit reports with `git add -f` so they survive. |
| `git` shows ~50 modified files you did not touch | Windows line endings | `git config core.autocrlf true` |

Everything below can be typed into the **PowerShell terminal in VS Code** (Terminal > New Terminal), opened in the project folder.
No prior knowledge needed: read top to bottom; each block says what it does and what you should see.

## 0. What was done, in one table

| Stage | Result | Evidence in the repo |
|---|---|---|
| AI baseline tests (4 files, 122 tests) | all pass | `src/test/java/.../ai/`, `prompt-log.md`, `prompt-inputs/` |
| Coverage of the AI suite | whole project 30% statement / 67% branch; the 3 targeted classes 100% / 100% | `evidence/jacoco-*.png`, `weak-tests-checklist.md` |
| PIT v1 (AI tests only, ALL mutators) | 238/250 killed = **95.2%** (12 survivors) | `target/pit-reports/v1-jdk11/` |
| PIT v2 (AI + manual tests) | 249/250 killed = **99.6%** (1 survivor, equivalent) | `target/pit-reports/v2/` |
| Survivors explained and killed | 12 survivors: 11 killed by manual tests, 1 equivalent | `surviving-mutants.md`, `src/test/.../manual/` |
| Risk ranking | script output and CI order | `risk-ranking.md` |
| Chart | v1 -> v2 | `mutation_v1_v2.png` |

Why two v1 numbers? The first v1 run reported 240/252 (95.2%) and the later one 238/250 (95.2%). The mutant count differs between runs
(the compiled code differed between them, most likely because of the JDK used). `v1-jdk11` was produced from the same compiled code as v2,
so **use v1-jdk11 vs v2 for the headline comparison** and mention the first run in a footnote.

## 1. One-time setup (every new terminal window)

PIT needs Java 11+, but the project and its libraries were written for Java 8. Use **JDK 11**.
Download Temurin 11 from adoptium.net if `java -version` does not already say 11.

```powershell
# Point this terminal at Java 11 (derive the folder from the java.exe that runs; do not type a version number by hand)
$env:JAVA_HOME = Split-Path (Split-Path (Get-Command java).Source)
# Put Maven on the PATH for this terminal (change the folder if yours is different)
$env:Path = "C:\Users\user\OneDrive - biust.ac.bw\Desktop\programe files\Apache\apache-maven-3.9.16\bin;" + $env:Path
java -version     # must say 11
mvn -v            # must say Maven 3.9.x and Java version 11
git config core.autocrlf true     # once: stops Windows line endings showing every file as modified
python --version; pip install matplotlib    # once, for the chart
```

If `java -version` says 25 (or anything not 11) the first command picked the wrong Java: install JDK 11 and make sure its `bin` folder is first on `$env:Path`.

## 2. Check the project builds and the original tests pass ("before AI")

```powershell
mvn clean test
```
Expect `BUILD SUCCESS`. Save the summary lines (`Tests run: ...`) as evidence (see `docs/00-upstream-baseline.txt`).
(Without the JAXB dependency in `pom.xml` this fails on Java 11+ with `NoClassDefFoundError: javax/xml/bind/JAXBException`. See Troubleshooting.)

## 3. AI baseline tests (what was done, how to repeat)

1. Open a **fresh** chat in your AI tool (no project context). Note tool name, model and date in `prompt-log.md`.
2. Open `docs/ai-test-engineer/prompt-inputs/P1_model_money_account_leg.txt`, copy everything, send it. Repeat for P2, P3, P4 (one message each).
3. Save each answer as a class in `src/test/java/com/yanimetaxas/bookkeeping/ai/` (e.g. `MoneyAiTest.java`). Do not improve the tests; fix only what stops them compiling and log every edit and deletion in `prompt-log.md`.
4. Run only the AI tests (the path style with slashes matters, see Troubleshooting):

```powershell
mvn clean test "-Dtest=com/yanimetaxas/bookkeeping/ai/*" "-Dsurefire.failIfNoSpecifiedTests=false"
```
Expect `Tests run: 122, Failures: 0, Errors: 0`.

## 4. Coverage and weak tests

The command in step 3 also writes the JaCoCo report. Open `target\site\jacoco\index.html` in a browser (in VS Code: right-click the file > Open with Live Server, or just double-click it in File Explorer).
Read the **Cov.** column for instructions (statements) and for branches, per package, and take screenshots into `docs/ai-test-engineer/evidence/`.
Then go through every AI test with `weak-tests-checklist.md` (categories B-E) and fill in its table.
Note: in the JaCoCo table, `Cxty` is cyclomatic complexity, **not** branches. Branch numbers are in the "Missed Branches" column.

## 5. PIT mutation testing

PIT changes ("mutates") the production code in small ways, then reruns the tests. A mutant the tests catch is KILLED; one they do not catch SURVIVED.
Score = killed / generated. Configuration lives in the `pitest-maven` block of `pom.xml` (scope = `model`, `validation`, `TransferServiceImpl`; mutators = `ALL`).

### 5a. Reproduce v1 (AI tests only) without touching your current work
The commit `e0eeb0e` ("PIT v1 report with ALL mutators (AI-only)") contains exactly the v1 configuration. Check it out in a separate folder:

```powershell
git worktree add ..\v1-check e0eeb0e
cd ..\v1-check
mvn clean test-compile org.pitest:pitest-maven:mutationCoverage
# report: target\pit-reports\v1\index.html   (expect about 95% with 12 survivors)
cd ..\double-entry-bookkeeping-api-Comp-441-group-Project-
git worktree remove ..\v1-check --force
```

### 5b. Run v2 (AI + manual tests) in the current folder
`pom.xml` here already lists both test packages and writes to `target/pit-reports/v2`:

```powershell
mvn clean test-compile org.pitest:pitest-maven:mutationCoverage
# open target\pit-reports\v2\index.html  (expect 249/250 = 99.6%)
```

### 5c. List the survivors of any report
```powershell
[xml]$x = Get-Content target\pit-reports\v2\mutations.xml
$x.mutations.mutation | Where-Object { $_.status -eq 'SURVIVED' } | ForEach-Object { "$($_.sourceFile):$($_.lineNumber) | $($_.mutator.Split('.')[-1]) | $($_.description)" }
```
(change `v2` to `v1-jdk11` to see the 12 v1 survivors). In the HTML report, survivors are the red lines in each class's source view.

## 6. Manual tests (the "killers")

Located in `src/test/java/com/yanimetaxas/bookkeeping/manual/`. Each of the 12 v1 survivors is explained and mapped to a test in `surviving-mutants.md`.
Run only them:
```powershell
mvn test "-Dtest=com/yanimetaxas/bookkeeping/manual/*" "-Dsurefire.failIfNoSpecifiedTests=false"
```
Techniques used: decision table (`TransferValidatorCurrencyTest`, `TransferValidatorDecisionTableTest`), boundary values (`TransactionTimestampBoundaryTest`, `AccountBoundaryTest`),
state checks (`ManualMutationKillTests`), exact-value oracles (hash code, timestamp).
`DefectCandidateTest` is `@Ignore`d on purpose: remove the `@Ignore` line to reproduce a real defect (`BigDecimal.equals` is scale-sensitive, so legs "5" and "-5" are wrongly rejected as unbalanced).

**Try a mutant yourself** (to see a test fail for the right reason): in `src/main/java/.../model/Account.java` change `< 0.0` to `<= 0.0`, run the manual tests, watch `AccountBoundaryTest` fail, then undo the change with `git checkout -- src/main`.

## 7. Risk ranking and chart

```powershell
python docs/ai-test-engineer/scripts/prioritise.py
python docs/ai-test-engineer/scripts/mutation_chart.py target/pit-reports/v1-jdk11/mutations.xml target/pit-reports/v2/mutations.xml
```
Explanation of the ranking: `risk-ranking.md`. The chart is saved as `mutation_v1_v2.png` in the folder where you run the command (move it into `docs/ai-test-engineer/`).

## 8. Saving your work to git

```powershell
git status
git add -A src docs pom.xml
git add -f target/pit-reports/v1-jdk11 target/pit-reports/v2   # -f because target/ is git-ignored; the reports are evidence
git commit -m "AI Test Engineer: manual tests, PIT v1/v2 reports, risk ranking, docs"
git push origin feature/ai-test-engineer
```
Do **not** commit `derby.log` (an automatic log file from the database tests). Add the line `derby.log` to `.gitignore` or delete the file.

## 9. Troubleshooting (every problem hit during this work)

| Message | Cause | Fix |
|---|---|---|
| `NoClassDefFoundError: javax/xml/bind/JAXBException` (25 of 36 upstream tests error) | Java removed JAXB in version 11; Hibernate 5.2 needs it | Add `jaxb-api` and `jaxb-runtime` 2.3.1 to `pom.xml` `<dependencies>` (already done here) |
| `The JAVA_HOME environment variable is not defined correctly` | JAVA_HOME points at a folder that does not exist (typed the wrong version) | Use the `Split-Path (Get-Command java)` line from section 1 |
| `mvn : The term 'mvn' is not recognized` | Maven is not on the PATH of this terminal | Re-run the `$env:Path = ...` line (it lasts only for that terminal window) |
| `java -version` still says 25 | Java 25 is first on the PATH | Put JDK 11's `bin` first, or reopen the terminal and redo section 1 |
| `Unsupported class file major version 69` (long JaCoCo stack traces) | JaCoCo 0.8.8 cannot read Java 25 classes | Use JDK 11 |
| `mvn test -Dtest=...ai.*` runs 0 tests, JaCoCo says "missing execution data file" | Surefire does not read a dotted package wildcard | Use slashes: `"-Dtest=com/yanimetaxas/bookkeeping/ai/*"` |
| PowerShell splits `-Dtest=a,b` or `-D...` oddly | PowerShell treats some characters specially | Wrap each `-D...` argument in double quotes |
| v1 shows 252 mutants, v2 shows 250 | Different compiled code between runs (JDK) | Compare v1-jdk11 with v2; see the note in section 0 |
| `git` shows ~50 modified files you did not touch | Windows line endings | `git config core.autocrlf true` |