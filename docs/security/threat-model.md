# Bookkeeping API Threat Model

## Scope and assumptions
This is an initial threat-model worksheet for a bookkeeping API. Confirm actual
deployment architecture, authentication, persistence, and exposed endpoints
from the implementation before finalising it.

## Assets
- Monetary balances and bookkeeping entries
- Account identifiers and transfer requests
- Integrity of transaction-leg balancing
- Application configuration and secrets
- Availability and correctness of transfer processing

## Trust boundaries
- External caller to API boundary, if an HTTP interface is deployed
- Request data to validation/business logic
- Service layer to ledger/account collaborators
- Build/CI system to repository and external reporting services

## Threats and mitigations
| Threat | Potential impact | Suggested controls | Verification |
|---|---|---|---|
| Malformed or extreme monetary inputs | Incorrect calculations or rejected valid work | Strict parsing, scale/range checks, boundary tests | Unit and integration tests |
| Unbalanced transfer accepted | Ledger integrity failure | Enforce invariant in trusted service logic | Regression and property/boundary tests |
| Unauthorized account operation | Disclosure or unauthorized movement of value | Authentication, authorization, account ownership checks | Verify implementation; security tests |
| Duplicate/replayed request | Duplicate bookkeeping entries | Idempotency strategy where required | Integration tests |
| Sensitive data in logs | Confidentiality breach | Redaction and minimal logging | Code review and log inspection |
| Dependency vulnerability | Exploitation of vulnerable library | Dependency scanning and updates | Dependency scan |
| Leaked CI token | Unauthorized use of reporting service | Rotate exposed token; store credentials in CI secrets | Secret scan and CI review |
| Denial of service via expensive input | Reduced availability | Input limits, timeouts, resource controls | Abuse and load tests |

## SAST procedure
Run Semgrep with `semgrep --config=auto .` where available. Save the command,
version, exit code, and report. Triage each finding as actionable, false positive,
or accepted risk with rationale. Remediate actionable issues and rerun the scan.

## Limitations
This document is an initial threat model, not proof that controls exist.
No SAST scan result is claimed here. Verify the implementation and attach actual
scan evidence before submission.
