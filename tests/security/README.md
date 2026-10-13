# Security Tests (`tests/security/`)

* **Owner:** De Silva N. N. B. (`@Nimsika-Bosilu`) — issue #57
* **Threat model:** [`docs/08-threat-model.md`](../../docs/08-threat-model.md) · **Controls:** [`docs/09-security-controls.md`](../../docs/09-security-controls.md)

These are controlled, black-box adversarial tests against the **team's own local** MiraNova backend. Each test plays the role of an attacker from the threat model (replay, tampering, guessing, stolen or foreign device, token forgery, enumeration, races, log leakage) and checks that the attack is **rejected**.

> **Current status (2026-10-13):** the FastAPI backend is not implemented yet (#40, #42, #44–#49). Every backend test is **skipped** with the reason "backend not reachable". Only SEC-11 (repository secret scan) runs today. Once the backend is merged, adapt [`sec_contract.py`](sec_contract.py) to the final API and run the suite to produce evidence for #58.

## Ethics and scope

* Run these tests only against your own local or demo environment, with **synthetic accounts** (SRS §12, ASM-THR-06).
* The suite **refuses to start** if `MIRANOVA_BASE_URL` is not a local host. Set `MIRANOVA_ALLOW_REMOTE=1` only for a team-owned demo server.
* Test accounts and device keys are generated at random in memory. Nothing is written to disk except the optional report.

## Test catalogue

| ID | File | What it attacks | Threats | SRS tests / requirements |
| :--- | :--- | :--- | :--- | :--- |
| SEC-00 | `test_sec00_baseline.py` | Confirms the legitimate flow works, so that denials in other tests are meaningful | — | TC-02, FR-04, FR-30 |
| SEC-01 | `test_sec01_replay.py` | Resends an already accepted device proof | TM-03 | TC-05, FR-21 |
| SEC-02 | `test_sec02_tampering.py` | Bit-flipped signature, signature over a different nonce, proof moved to another transaction, random IDs, malformed signatures | TM-05, TM-20 | FR-19, FR-23, FR-44 |
| SEC-03 | `test_sec03_wrong_device.py` | Unregistered key, client-supplied public key, another account's device, unknown device | TM-06 | TC-03, FR-20 |
| SEC-04 | `test_sec04_guessing.py` | Wrong secret, unknown account vs. wrong secret (enumeration), token issued too early, brute force / throttling | TM-01, TM-02, TM-25 | TC-04, FR-16, FR-17 |
| SEC-05 | `test_sec05_factor_bypass.py` | Device factor without the knowledge factor, after a wrong secret, forged "factor passed" flags, out-of-order calls | TM-07 | FR-22, FR-23 |
| SEC-06 | `test_sec06_tokens.py` | Malformed, tampered, `alg: none` and expired tokens; token reuse after logout; secret fields in `/v1/me` | TM-16, TM-17, TM-29 | TC-07, TC-08, FR-27, FR-29 |
| SEC-07 | `test_sec07_enrollment_and_revocation.py` | Enrolling an attacker device for a victim, missing proof of possession, replayed registration, revoked device | TM-09, TM-10, TM-11 | TC-01, TC-14, FR-07, FR-12 |
| SEC-08 | `test_sec08_expiry.py` | Challenge without an expiry; use of an expired challenge | TM-27 | TC-06, FR-18 |
| SEC-09 | `test_sec09_concurrency.py` | The same proof submitted in parallel (double-spend) | TM-04 | TC-13, NFR-R02 |
| SEC-10 | `test_sec10_log_leakage.py` | Secrets, tokens, signatures or private keys in backend logs; missing auth events | TM-19, TM-26 | TC-12, FR-42 |
| SEC-11 | `test_sec11_repo_secrets.py` | Private keys, JWTs or API keys committed to Git; tracked `.env`, keystore or DB files; `.gitignore` coverage | TM-28 | FR-03 |

Each test's docstring lists its `Refs:` (threats and requirements) and `Expected:` secure result. The report generator copies these into the evidence table.

## Running

```bash
pip install -r tests/security/requirements.txt

# backend running locally on port 8000
MIRANOVA_BASE_URL=http://127.0.0.1:8000 \
MIRANOVA_LOG_PATH=backend/logs/app.log \
python -m pytest tests/security -v \
  --junitxml=tests/security/results/junit.xml \
  --sec-report=tests/security/results/security-test-report.md
```

PowerShell:

```powershell
$env:MIRANOVA_BASE_URL = "http://127.0.0.1:8000"
python -m pytest tests/security -v --sec-report=tests/security/results/security-test-report.md
```

Useful selections:

* `-m "not ratelimit"` runs everything except the brute-force test. If the backend rate-limits per IP, run `-m ratelimit` **last and on its own**, so that throttling does not affect other tests.
* `-m "not slow"` skips the test that waits for a challenge to expire.
* `-k sec01` runs one area only.

### Configuration

| Variable | Default | Purpose |
| :--- | :--- | :--- |
| `MIRANOVA_BASE_URL` | `http://127.0.0.1:8000` | Backend under test (local hosts only) |
| `MIRANOVA_ALLOW_REMOTE` | unset | `1` allows a non-local, team-owned host |
| `MIRANOVA_TIMEOUT` | `10` | HTTP timeout (seconds) |
| `MIRANOVA_LOG_PATH` | unset | Backend log file for SEC-10 (SEC-10 is skipped when this is unset) |
| `MIRANOVA_MAX_FAILED_ATTEMPTS` | `10` | Documented throttling threshold (FR-17 policy) |
| `MIRANOVA_MAX_EXPIRY_WAIT` | `130` | Longest time SEC-08 may wait for a challenge to expire |
| `MIRANOVA_TEST_JWT_SECRET` | unset | HS256 key of a **local test** backend, used to mint a correctly signed expired token (never a real key) |
| `MIRANOVA_CONCURRENCY` | `10` | Parallel requests in SEC-09 |

## Adapting to the final API

The API contract is not frozen yet (`docs/07-api-contract.md`). All endpoint paths and field names live in **[`sec_contract.py`](sec_contract.py)**:

* `ENDPOINTS` maps logical names to paths. Set an entry to `None` if the backend does not offer that endpoint; tests that need it are then skipped with a clear reason (e.g. `revoke_device`).
* The `*_body()` builders and `parse_*()` functions translate requests and responses.
* `ChallengeInfo.bytes_to_sign()` uses the server-supplied `signing_payload` when the backend returns one (recommended fix for finding F-01). Otherwise it falls back to the current Android format `miranova:auth:v1:<nonce>:<timestamp>:<origin>:<action>`.

The device key (`SoftwareDeviceKey`) signs exactly as the Android client does: ECDSA P-256, SHA-256, ASN.1 DER signature, X.509 SubjectPublicKeyInfo public key, standard Base64.

## Reading results

| Outcome | Meaning | Action |
| :--- | :--- | :--- |
| **passed** | The attack was rejected as expected | Record it as evidence in `docs/09-security-controls.md` (#58) |
| **failed** | **Security defect**, or a broken setup step (the message names it) | Open a tracked GitHub issue for the owning developer before the demo (#57 acceptance criterion) |
| **skipped** | A precondition is missing (no backend, endpoint not offered, `MIRANOVA_LOG_PATH` unset) | State it as "not tested" in the evaluation; do not claim the control |
| **xfail** | Known open defect linked to a finding (e.g. F-11) | Becomes **XPASS** once fixed; then remove the marker |

If SEC-00 fails, the legitimate flow is broken (or `sec_contract.py` does not match the API). Fix that first: negative results are not meaningful until SEC-00 passes.

## How the suite itself was validated

Before the real backend existed, the suite was run against a throwaway in-memory reference server. That server implemented the proposed SRS §10.3 contract and could switch on deliberate weaknesses one at a time. It was used only to check the tests and is **not** part of this repository or of MiraNova.

* Secure mode: 48 passed, 1 skipped (no revocation endpoint), 1 xfail (F-11).
* Each weakness was detected by the intended test(s):

| Weakness switched on | Tests that failed (as intended) |
| :--- | :--- |
| Challenge not consumed (replay) | SEC-01 ×2, SEC-09 |
| Client-supplied public key trusted | SEC-03 `test_client_supplied_public_key_is_ignored` |
| Device step allowed before the knowledge factor | SEC-05 `test_device_factor_without_knowledge_factor_is_denied` |
| Different error for unknown account | SEC-04 `test_unknown_account_and_wrong_secret_are_indistinguishable` |
| No rate limiting | SEC-04 `test_repeated_wrong_secrets_are_throttled` |
| Logout does not revoke the token | SEC-06 `test_token_is_rejected_after_logout` |
| `alg: none` accepted | SEC-06 `test_alg_none_token_is_rejected` |
| Secret and token written to the log | SEC-10 `test_logs_do_not_contain_secrets_tokens_or_signatures` |
| Non-atomic challenge consumption (race) | SEC-09 |

This shows that the tests can detect these defects. It says nothing about the real backend, which must be tested separately once it exists.
