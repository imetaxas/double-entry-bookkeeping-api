#!/usr/bin/env python3
"""Risk-based test prioritisation. Run from the repo root:  python3 scripts/prioritise.py
risk = 0.5*impact + 0.3*complexity_norm + 0.2*churn_norm   (all scaled 0..1)
impact is a documented judgement (money-moving / invariant-enforcing code = high).
complexity = count of decision points (if, for, &&, ||, ?) as a cheap cyclomatic proxy.
churn = number of commits touching the file (git log)."""
import re, subprocess, pathlib
BASE = "src/main/java/com/yanimetaxas/bookkeeping/"
CLASSES = {
 "validation/TransferValidatorImpl": (5, "enforces balanced legs + no-overdraft invariants", "TransferValidatorDecisionTableTest"),
 "service/TransferServiceImpl":      (5, "orchestrates balance updates inside one transaction", "TransferServiceInteractionTest"),
 "model/Account":                    (4, "overdraft rule (balance < 0) used by every transfer", "AccountBoundaryTest"),
 "model/TransferRequest":            (3, "builds legs; debit sign handling", "(TransferRequest tests)"),
 "model/Money":                      (3, "value object / equality", "MoneyTest"),
 "model/TransactionLeg":             (2, "null guards only", "(TransactionLeg tests)"),
}
def metrics(c):
    src = pathlib.Path(BASE + c + ".java").read_text(encoding="utf-8")
    cx = len(re.findall(r"\b(if|for|while|case)\b|&&|\|\||\?", src))
    ch = len(subprocess.check_output(["git", "log", "--oneline", "--follow", "--", BASE + c + ".java"]).decode().splitlines())
    return cx, ch
rows = []
for c, (imp, why, t) in CLASSES.items():
    cx, ch = metrics(c); rows.append([c, imp, cx, ch, why, t])
mx_cx = max(r[2] for r in rows) or 1; mx_ch = max(r[3] for r in rows) or 1
for r in rows:
    r.append(round(0.5*r[1]/5 + 0.3*r[2]/mx_cx + 0.2*r[3]/mx_ch, 3))
rows.sort(key=lambda r: -r[-1])
print(f"{'rank':<5}{'class':<34}{'impact':<7}{'cx':<5}{'churn':<6}{'risk':<7}test / why")
for i, r in enumerate(rows, 1):
    print(f"{i:<5}{r[0]:<34}{r[1]:<7}{r[2]:<5}{r[3]:<6}{r[-1]:<7}{r[5]} - {r[4]}")
