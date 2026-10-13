# 09. Security Controls

* **Project:** Miranova Accessible Multi-Factor Authentication
* **Document owner:** De Silva N. N. B. (`@Nimsika-Bosilu`)
* **Related issues:** #38 (initial control mapping), #57 (verification tests), #58 (post-test evaluation)
* **Threat source:** [`08-threat-model.md`](08-threat-model.md)
* **Version:** 0.1 — initial mapping (pre-implementation)
* **Last updated:** 2026-10-13

> **Status of this document.** This version maps each threat to the controls the team **plans** to implement. A control is marked **Implemented** only where code already exists on `dev`. A control is marked **Verified** only after a passing test in `tests/security/` (or another linked test) supports it. Issue #58 will update the status, evidence and residual-risk columns after the #57 tests have run against the integrated system.

## 1. Status and type legend

| Status | Meaning |
| :--- | :--- |
| **Planned** | Agreed control. No code yet. |
| **Partial** | Some code exists, but it does not yet meet the control as written, or is only a stub. |
| **Implemented** | Code exists on `dev`. **Not yet verified** by a security test. |
| **Verified** | Implemented, and a linked test passed against the integrated build (date and commit recorded in #57 results). |
| **Not implemented** | Deliberately not built in the prototype. Recorded as residual risk. |

**Type:** **P** = Preventive (reduces the chance an attack succeeds), **D** = Detective (makes an attack visible), **R** = Recovery / response (limits damage after an attack).

---

## 2. Control catalogue

| ID | Control | Type | Threats | SRS | Owner / issue | Status | Verification |
| :--- | :--- | :---: | :--- | :--- | :--- | :--- | :--- |
| **CTL-01** | Store the knowledge secret only as a salted, slow hash (bcrypt per the original design, or Argon2id if ADR-justified, DEC-05). Never store or log plaintext. | P | TM-01, TM-18 | FR-13, NFR-S01 | `@Viranske-1` #48 | Planned | TC-04; DB inspection TC-12 |
| **CTL-02** | Rate limiting / progressive back-off per account **and** per client IP on `auth/start`, `verify-secret`, `device-challenge` and `register`. Temporary back-off, not permanent lockout. Return `429` with a generic body. | P/D | TM-01, TM-24, TM-25 | FR-17, NFR-S05 | `@Viranske-1` #48 | Planned | SEC-04 |
| **CTL-03** | Two independent factors: a stolen secret alone or a stolen device alone must not produce a session. | P | TM-01, TM-11, TM-12 | FR-22, G-01 | `@hasarangadinuj` #46 | Planned | SEC-05, TC-03, TC-04 |
| **CTL-04** | Uniform public error body and status for unknown account vs. wrong secret. Dummy hash comparison for unknown accounts so timing is similar. | P | TM-02 | FR-16 | `@Viranske-1` #48 | Planned | SEC-04 |
| **CTL-05** | Challenges come from a CSPRNG (≥128 bits), are stored server-side, and are **consumed atomically** (single `UPDATE … WHERE consumed_at IS NULL`, or a row lock) before the signature result is accepted. | P | TM-03, TM-04 | FR-18, FR-21, NFR-R02 | `@hasarangadinuj` #45 | Planned | SEC-01, SEC-09, TC-05, TC-13 |
| **CTL-06** | Challenge and transaction expiry come from **server time only**. Client timestamps are ignored for security decisions. Expired rows are rejected and cleaned up. | P | TM-03, TM-24, TM-27 | FR-18, FR-23 | `@hasarangadinuj` #45 | Planned | SEC-08, TC-06 |
| **CTL-07** | The signed message binds `purpose`, `transaction_id`, `account_id`, `device_id`, `nonce` and `expires_at` with unambiguous encoding. The server rebuilds the bytes from its own records. | P | TM-05 | FR-19, NFR-S02 | `@hasarangadinuj` #35/#45; `@kisajaBeddawala` (client) | **Partial** — the client signs `miranova:auth:v1:<nonce>:<ts>:<origin>:<action>` without transaction/account/device binding (F-01) | SEC-02 |
| **CTL-08** | Verify the signature **only** with the stored public key of an **active** device that belongs to the transaction's account. Ignore any client-supplied public key or message. | P | TM-06, TM-11 | FR-20 | `@hasarangadinuj` #45 | Planned (F-02) | SEC-03 |
| **CTL-09** | Server-side MFA state machine (`SecretPending → DevicePending → Authenticated`). Out-of-order calls fail closed. Unknown request fields are rejected (Pydantic `extra="forbid"`). | P | TM-07 | FR-22, FR-23, NFR-S03 | `@hasarangadinuj` #46 | Planned | SEC-05 |
| **CTL-10** | The client shows or announces success **only** after a server success response that contains a session. No mock-approve path in release builds. | P | TM-08 | FR-02, NFR-R01 | `@kisajaBeddawala` #54 | **Partial** — the UI announces success after local signing; the mock path approves without a key (F-07) | Code review; TC-11 |
| **CTL-11** | Controlled enrollment bootstrap: a device can be registered to an account only inside an authorised enrollment context (DEC-04), e.g. immediately after account creation in the same transaction, or after the knowledge factor plus a one-time enrollment code. | P | TM-09 | FR-06, FR-07 | `@hasarangadinuj` #35, `@Viranske-1` #44 | Planned (F-03, F-06) | SEC-07, TC-01 |
| **CTL-12** | Proof of possession at registration: the client signs a server enrollment challenge, and the server verifies it with the submitted public key before storing it. | P | TM-09, TM-06 | FR-07, FR-10 | `@Viranske-1` #44 | Planned (F-03) | SEC-07 |
| **CTL-13** | Re-enrollment and recovery require the same authorisation as first enrollment, and revoke the previous device server-side. A demo-only operator reset is documented with its residual risk (DEC-08). | P/R | TM-10 | FR-12 | `@hasarangadinuj` #35, `@Viranske-1` #44 | Planned (F-06) | SEC-07, TC-14 |
| **CTL-14** | Optional per-operation user authentication on the device key (`setUserAuthenticationRequired(true)`, `setUserAuthenticationParameters(0, BIOMETRIC_STRONG or DEVICE_CREDENTIAL)`), applied consistently on every key-generation path. | P | TM-11, TM-12, TM-30 | FR-25 | `@kisajaBeddawala` #50 | **Partial** — implemented when `requireBiometric` is true; the StrongBox-fallback path drops the parameters (F-12) | Manual device test |
| **CTL-15** | Server-side device revocation (status `revoked`). A revoked device is rejected at `device-challenge` and `device-verify`. | R | TM-10, TM-11, TM-12 | FR-12, FR-20 | `@Viranske-1` #44 | Planned | SEC-07 |
| **CTL-16** | Non-exportable `AndroidKeyStore` EC P-256 key with `PURPOSE_SIGN` only; `android:allowBackup="false"`; the private key never leaves the device. | P | TM-13 | FR-08, NFR-S01 | `@kisajaBeddawala` #50 | **Implemented** (`AndroidKeyStoreManager.kt`, manifest). Hardware level not attested (RR-02). Non-export check is a no-op (F-05). | On-device instrumentation test needed: `KeyStoreManagerTest` uses a JVM mock (`MockJvmKeyStoreManager`), so it does not exercise the real `AndroidKeyStore`; DB inspection TC-12 |
| **CTL-17** | TLS with platform certificate validation outside local development. A Network Security Config allows cleartext only to `10.0.2.2`/localhost in debug builds. The base-URL override is debug-only. | P | TM-14, TM-15 | FR-43, NFR-S06 | `@kisajaBeddawala`, `@NirmalKBandara` #59 | **Partial** — app-wide `usesCleartextTraffic="true"` (F-09) | Config review; TC-15 |
| **CTL-18** | Token validation with a server-pinned algorithm (no `none`, no algorithm taken from the token header), signature, `exp`, `iat`, `aud`/scope, and account/device status. | P | TM-16 | FR-27 | `@Viranske-1` #47 | Planned | SEC-06, TC-07 |
| **CTL-19** | Short-lived access token. Server-side revocation on logout (session row or JTI deny-list, per ADR-004 / DEC-07). The client keeps the token in memory or encrypted storage and clears it on logout. | P/R | TM-17 | FR-28, FR-29 | `@Viranske-1` #47, `@kisajaBeddawala` #54 | Planned (logout currently navigates only) | SEC-06, TC-08 |
| **CTL-20** | Audit and application logging uses an allow-list of fields. No passwords, bearer tokens, signatures, private keys or raw auth request bodies. Tokens are referenced by a hash/JTI prefix only. | P | TM-17, TM-19 | FR-42, NFR-S04 | `@Viranske-1` #49 | Planned | SEC-10, TC-12 |
| **CTL-21** | The DB stores only a hash of the session token / JTI, public keys, and password hashes. A least-privilege DB user is used. | P | TM-18 | NFR-S01 | `@Viranske-1` #42 | Planned | TC-12 |
| **CTL-22** | Generic public errors (`{"error":{"code","message","request_id"}}`) with no stack traces. Response models use explicit fields. The client maps error codes to fixed accessible messages. | P | TM-20, TM-29 | FR-44, NFR-R03 | `@Viranske-1` #48; `@kisajaBeddawala` | Planned (client shows raw messages, F-10) | SEC-04, SEC-06 |
| **CTL-23** | Device approval is only possible inside a login the user started on the same device (no unsolicited push approvals). Origin and action text are defined by the server. Per-account challenge rate limits. Spoken context includes when the request was started. | P | TM-21 | FR-35 | `@kisajaBeddawala`, `@hasarangadinuj` | Planned | Accessibility review #56; SEC-04 |
| **CTL-24** | Password-type secret field. `filterTouchesWhenObscured` on approve/sign controls. | P | TM-22 | FR-14 | `@kisajaBeddawala` | Planned (no secret field yet, F-08) | Manual review |
| **CTL-25** | No spoken secret entry (ADR-005 §2.2). The TTS secrecy filter scrubs hex/base64/numeric-code/private-key patterns before speaking. No echo of secret values. | P | TM-23 | FR-15, FR-40 | `@kisajaBeddawala` #53 | **Implemented** (`TtsManager.containsSecret/sanitizeUtterance`). The filter is regex-based (best effort) and does not cover TalkBack semantics (F-13). | `TtsManagerTest`, `MultimodalFeedbackManagerTest` |
| **CTL-26** | Security audit events for: account created, enrollment success/failure, secret failure, rate-limited, challenge issued/consumed/replayed/expired, device-verify success/failure, session issued/revoked, device revoked. Each event records time, type, outcome, reason code, account/transaction reference and request ID. | D | TM-26, TM-03, TM-09 | FR-24, FR-42 | `@Viranske-1` #49 | Planned | SEC-10 |
| **CTL-27** | `.gitignore` covers `.env*` (except `.env.example`), keys, keystores, DB files and venvs. Secret scanning runs in CI and in `tests/security`. | P/D | TM-28 | FR-03 | `@NirmalKBandara` #43; `@Nimsika-Bosilu` | **Partial** — root `.gitignore` is empty (F-11) | SEC-11 |

---

## 3. Threat → control coverage

Every P0 threat must have at least one preventive control and one verification test before the demo (acceptance criterion of #38).

| Threat | P | Preventive | Detective | Recovery | Test |
| :--- | :---: | :--- | :--- | :--- | :--- |
| TM-01 Online guessing | P0 | CTL-01, CTL-02, CTL-03 | CTL-26 | — | SEC-04 |
| TM-02 Enumeration | P0 | CTL-04 | CTL-26 | — | SEC-04 |
| TM-03 Replay | P0 | CTL-05, CTL-06 | CTL-26 | — | SEC-01 |
| TM-04 Race | P0 | CTL-05 | CTL-26 | — | SEC-09 |
| TM-05 Context substitution | P0 | CTL-07 | CTL-26 | — | SEC-02 |
| TM-06 Key substitution | P0 | CTL-08, CTL-12 | CTL-26 | — | SEC-03 |
| TM-07 Factor bypass | P0 | CTL-09, CTL-03 | CTL-26 | — | SEC-05 |
| TM-08 Client-declared success | P0 | CTL-10 | — | — | TC-11, review |
| TM-09 Enrollment hijack | P0 | CTL-11, CTL-12 | CTL-26 | CTL-15 | SEC-07 |
| TM-10 Recovery bypass | P0 | CTL-13 | CTL-26 | CTL-15 | SEC-07 |
| TM-14 MITM / cleartext | P0 | CTL-17 | — | — | TC-15, config review |
| TM-16 Token forgery | P0 | CTL-18 | CTL-26 | — | SEC-06 |
| TM-17 Token theft / reuse after logout | P0 | CTL-19, CTL-20 | CTL-26 | CTL-19 | SEC-06 |
| TM-19 Log leakage | P0 | CTL-20 | — | — | SEC-10 |
| TM-27 Clock manipulation | P0 | CTL-06 | — | — | SEC-08 |
| TM-28 Secrets in Git | P0 | CTL-27 | CTL-27 (scan) | Rotate secret | SEC-11 |
| TM-11/12 Stolen device | P1 | CTL-03, CTL-14 | CTL-26 | CTL-15 | SEC-07 |
| TM-13 Key extraction | P1 | CTL-16 | — | CTL-15 | Review |
| TM-15 Rogue backend | P1 | CTL-17 | — | — | Review |
| TM-18 DB disclosure | P1 | CTL-01, CTL-21 | — | Rotate JWT key | TC-12 |
| TM-20 Diagnostic leakage | P1 | CTL-22 | — | — | SEC-04 |
| TM-21 Approval fatigue | P1 | CTL-23, CTL-02 | CTL-26 | CTL-15 | #56 review |
| TM-23 Acoustic eavesdropping | P1 | CTL-25 | — | — | `TtsManagerTest` |
| TM-26 Repudiation | P1 | — | CTL-26 | — | SEC-10 |
| TM-29 Over-broad response | P1 | CTL-22 | — | — | SEC-06 |
| TM-30 Key-policy fallback | P1 | CTL-14 | — | — | Review |
| TM-22 Malicious a11y service / overlay | P2 | CTL-24 | — | — | Manual |
| TM-24 Resource exhaustion | P2 | CTL-02, CTL-06 | CTL-26 | — | SEC-04 |
| TM-25 Targeted lockout | P2 | CTL-02 | CTL-26 | Back-off expiry | SEC-04 |

---

## 4. Evaluation results

*To be completed under #58 after the #57 suite runs against the integrated build. For each control, #58 records the implementation link, test ID, observed result, date/commit, final status and residual risk. It also adds a prototype-limitations section.*
