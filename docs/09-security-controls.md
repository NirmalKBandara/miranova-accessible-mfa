# 09. Security Controls

* **Project:** Miranova Accessible Multi-Factor Authentication
* **Document owner:** De Silva N. N. B. (`@Nimsika-Bosilu`)
* **Related issues:** #38 (initial control mapping), #57 (verification tests), #58 (post-test evaluation)
* **Threat source:** [`08-threat-model.md`](08-threat-model.md)
* **Version:** 0.2 — pre-backend evaluation (Android client + repository only)
* **Last updated:** 2026-10-13

> **Status of this document.** §2–§3 map each threat to the controls the team **plans** to implement. A control is marked **Implemented** only where code already exists on `dev`. It is marked **Verified** only after a passing test in `tests/security/` (or another linked test) supports it. §4–§6 are the first part of issue #58. They evaluate what can be evaluated **before the backend exists**: the Android client and the repository. They also record the prototype's limitations and list overstated claims in project documents. Backend controls stay **Pending** until the #57 suite has run against the integrated system.

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

## 4. Evaluation results (issue #58)

### 4.1 Evaluation snapshot — before the backend exists

* **Evaluated commit:** `dev` @ `e499b64` (after PR #70), 2026-10-13.
* **Evidence used:** (a) code review of the Android client against each control; (b) `tests/security/` run with no backend: SEC-11 = 2 passed, 1 xfail (F-11); every backend test skipped.
* **Not used as evidence:** Android unit tests in `android-app/app/src/test/` exist, but the security owner did not re-run them for this evaluation (the repository has no Gradle wrapper script). They also run on the JVM with mocks, not on a device. Results from the throwaway reference server used to validate the #57 suite (see `tests/security/README.md`) say nothing about MiraNova and are **not** evidence for any control below.

| Control | Implementation evidence (`dev`) | Test evidence observed | Result | Status | Residual risk / gap |
| :--- | :--- | :--- | :--- | :--- | :--- |
| CTL-07 signing-context binding | [`CanonicalChallenge.kt`](../android-app/app/src/main/java/com/miranova/mfa/crypto/CanonicalChallenge.kt) signs `miranova:auth:v1:<nonce>:<ts>:<origin>:<action>` | None (SEC-02 needs the backend) | Binds nonce, origin and action only. No transaction, account or device binding. The delimiter is ambiguous (F-01). | **Partial** | TM-05 is open until the server defines the signed context |
| CTL-10 server-confirmed success only | [`AuthChallengeScreen.kt`](../android-app/app/src/main/java/com/miranova/mfa/ui/screens/AuthChallengeScreen.kt) | Code review | Announces "Session established" after local signing. The mock path approves without a key (F-07). | **Partial (stub)** | TM-08: the user is told they are authenticated when they are not |
| CTL-14 per-use user authentication on the key | [`AndroidKeyStoreManager.kt`](../android-app/app/src/main/java/com/miranova/mfa/crypto/AndroidKeyStoreManager.kt) `generateKeyPair()` | Code review | Correct when `requireBiometric = true`. The StrongBox fallback path drops the auth parameters (F-12). Auth is off by default. | **Partial** | TM-12: with auth off, an unlocked phone can use the key |
| CTL-16 non-exportable key, `allowBackup=false` | `AndroidKeyStoreManager.kt`, [`AndroidManifest.xml`](../android-app/app/src/main/AndroidManifest.xml) | Code review only. `KeyStoreManagerTest` uses a JVM mock (F-05). | The key is generated in `AndroidKeyStore` with `PURPOSE_SIGN`. Backups are disabled. The non-export check is a no-op. | **Implemented — not verified on a device** | RR-02: no attestation; emulator keys are software-backed |
| CTL-17 TLS / cleartext limited to debug | `AndroidManifest.xml`, [`AppConfig.kt`](../android-app/app/src/main/java/com/miranova/mfa/data/config/AppConfig.kt) | Code review | `usesCleartextTraffic="true"` for every host. Default URLs are `http://` (F-09). | **Partial** | TM-14 / RR-03: traffic is readable on the demo network |
| CTL-22 generic errors (client side) | [`EnrollmentRepository.kt`](../android-app/app/src/main/java/com/miranova/mfa/data/repository/EnrollmentRepository.kt), `AuthChallengeScreen.kt`, `EnrollmentScreen.kt` | Code review | User-facing errors include `${e.message}` and raw server bodies (F-10) | **Partial** | TM-20: internal details shown and spoken to the user |
| CTL-25 no spoken secrets / TTS filter | [`TtsManager.kt`](../android-app/app/src/main/java/com/miranova/mfa/ui/accessibility/TtsManager.kt) `containsSecret()`, `sanitizeUtterance()`; no `RECORD_AUDIO` permission | Code review; unit tests exist (`TtsManagerTest`), not re-run | The regex filter covers app TTS only. TalkBack reads the nonce (F-13). | **Implemented — partially effective** | TM-23: regex filtering is best effort |
| CTL-27 no secrets in Git | Root `.gitignore` empty (F-11) | **SEC-11:** `test_no_secret_patterns_in_tracked_files` passed; `test_no_credential_files_tracked` passed; `test_gitignore_excludes_local_secret_files` **xfail** | Nothing sensitive is tracked today, but `.env` would not be ignored | **Partial** | TM-28: risk returns as soon as the backend adds a `.env` |
| CTL-01–06, 08, 09, 11–13, 15, 18–21, 23, 24, 26, and the server side of CTL-22 (backend and new UI controls) | Not implemented yet (#40–#49, #54) | SEC-00…SEC-10 skipped: backend not reachable | — | **Pending** | Evaluated after the #57 run against the integrated build |

### 4.2 Interim conclusion

As of `e499b64`, **no MiraNova security control can be marked Verified.** The client-side protections that exist (Keystore key generation, disabled backups, the TTS secrecy filter, no microphone permission) are real, but they are either unverified on a device or only partly effective. The central MFA guarantees (server-side factor enforcement, single-use challenges, session control) do not exist yet. The current Android flow also has no knowledge factor (F-08) and announces success without a server (F-07). **The prototype must not yet be described as providing multi-factor authentication.**

---

## 5. Prototype limitations

These limitations hold even if every planned control is implemented. The final report and the viva should state them (SRS §12.1, NFR-S09).

| ID | Limitation | Consequence | Related |
| :--- | :--- | :--- | :--- |
| L-01 | The demo backend runs locally over HTTP (`10.0.2.2` / LAN). There is no TLS termination. | Transport confidentiality and integrity are **not demonstrated**. Captured traffic would expose the secret and tokens. | RR-03, TM-14, F-09 |
| L-02 | Emulator keys are software-backed (`SecurityLevel.SOFTWARE`). | Hardware non-exportability is **not shown** in an emulator demo. | RR-02, CTL-16 |
| L-03 | No Android key attestation. The server cannot verify `hardware_backed` / `security_level`. | Any hardware-backing claim is client-reported only. | RR-02, F-03 |
| L-04 | Recovery and re-enrollment are a controlled demo/operator process (DEC-08 is undecided). | Recovery cannot be called production-safe. Lost-device handling depends on an operator. | RR-04, TM-10 |
| L-05 | Accounts are synthetic, with no identity proofing (ASM-05). | The system authenticates "the enrolled holder", not a verified real person. | SRS §1.4 |
| L-06 | Tested device classes are not yet confirmed (ASM-01, ASM-03). Only one emulator or device is expected. | Behaviour on StrongBox / older TEE / OEM variants is untested. | NFR-A06 |
| L-07 | No usability study with visually impaired participants is confirmed. | Accessibility claims rest on design review and team testing only. | SRS §4.3, #56 |
| L-08 | Security testing is limited to the team's own black-box tests. No independent penetration test, fuzzing or code audit tool was used. | Defects outside the tested threat list may remain. | RR-07 |
| L-09 | An unlocked or compromised device can use the key (signing oracle). | MFA limits single-factor compromise, not endpoint compromise. | RR-01, TM-12 |
| L-10 | Rate limiting slows guessing but cannot make it impossible, and back-off can be abused for lockout. | Residual online-guessing and lockout risk. | RR-06, TM-25 |
| L-11 | No NIST AAL, WCAG or OWASP conformance assessment was performed. | The project may cite these standards as design references only, not as conformance (SRS §1 "How to use", §12.1). | §6 below |

---

## 6. Claims-wording review

#58 requires that results are not overstated. The following statements in project documents claim more than the design or the evidence supports. They are listed here for their owners to revise; **this review does not edit other owners' files.** Suggested wording follows SRS §12.1.

| ID | Location | Current wording (abridged) | Why it is overstated | Suggested wording | Owner |
| :--- | :--- | :--- | :--- | :--- | :--- |
| W-01 | [ADR-003](adr/ADR-003-android-keystore.md) §2.2 | "returns `null`, **mathematically guaranteeing** that the private key cannot be exported" | Non-exportability is a platform property that depends on the device and the security level, not a mathematical guarantee. The emulator is software-backed, and the code check is a no-op (F-05). | "On hardware-backed devices the Keystore is designed not to release private key material (`getEncoded()` returns `null`). Emulator keys are software-backed." | `@kisajaBeddawala` |
| W-02 | ADR-003 §3 | "**High Security (NIST AAL2 Alignment)**: Private keys **cannot be stolen** via software root exploits…" | SRS §4.2 excludes certification at any AAL. A rooted device can still *use* the key (RR-01). | "Reduces the risk of key extraction from app storage or backups. No AAL claim is made." | `@kisajaBeddawala` |
| W-03 | ADR-003 §3 | "canonical payloads **defeat** MITM tampering and challenge replay" | Replay resistance depends on server-side single-use and expiry checks, which are not implemented or tested yet (CTL-05/06). The current message has no transaction binding (F-01). | "Together with server-side single-use and expiry checks (to be tested in SEC-01/SEC-08), signed challenges are intended to resist replay." | `@kisajaBeddawala` |
| W-04 | ADR-003 §3 | "**Zero Secret Disclosure**: … **cannot intercept secrets**" | The approved model still uses a knowledge secret that can be observed or guessed (TM-01, TM-23). | "Device proof involves no spoken or displayed secret. The knowledge factor remains observable and is protected by masked input and rate limiting." | `@kisajaBeddawala` |
| W-05 | [`01-assumptions.md`](01-assumptions.md) ASM-SEC-01 | "plaintext key bytes **cannot be extracted** by user-space applications **or root compromise**" | Root compromise is outside what the design can resist (ASM-THR-05). The emulator is software-backed. | "…are intended to be non-exportable on hardware-backed devices. Root compromise can still misuse the key." | `@kisajaBeddawala` |
| W-06 | `01-assumptions.md` ASM-SEC-03 | "**guarantees non-repudiation and prevents** cross-protocol replay" | Non-repudiation is not a project goal. Replay protection is a server property (F-01, CTL-05). | "Domain-separates MiraNova signatures (prefix `miranova:auth:v1`). Replay is rejected by the server's single-use checks." | `@kisajaBeddawala` |
| W-07 | `01-assumptions.md` ASM-ACC-05 | "**completely eliminates** the need for cognitive tests" | The knowledge factor still requires remembering a secret. | "Removes the need to transcribe OTP codes. A remembered secret is still required for the knowledge factor." | `@kisajaBeddawala` |
| W-08 | `01-assumptions.md` ASM-UI-03 | "dialogs … **preventing leakage** of … raw server traces" | Contradicted by the code: error strings include `${e.message}` and raw server bodies (F-10). | Keep the goal, and add "requires mapping server errors to fixed messages (open, F-10)". | `@kisajaBeddawala` |
| W-09 | [ADR-005](adr/ADR-005-accessibility-strategy.md) §2.4; [`references.md`](references.md) REF-ACC-03 | "**committed to WCAG 2.2 Level AA compliance**" / "The Miranova architecture **conforms to Level AA**" | No conformance evaluation has been done. WCAG is a web standard used here as a design reference (SRS EXT-04, L-11). | "Uses WCAG 2.2 AA success criteria as design targets. Conformance has not been evaluated." | `@kisajaBeddawala` |
| W-10 | ADR-005 §3 | "Zero spoken secrets **prevents eavesdropping**" | TalkBack reads the nonce (F-13). The TTS filter is regex-based. The knowledge secret could still be observed when it is typed. | "Reduces acoustic exposure: no secret is requested by voice, and app TTS output is filtered." | `@kisajaBeddawala` |
| W-11 | [`05-accessibility-design.md`](05-accessibility-design.md) §9 | "Miranova **guarantees** deterministic behavior across all scenarios"; "**100%** operable" | Untested so far (#56 pending). | "Is designed to remain operable in these scenarios. Verified in #56." | `@kisajaBeddawala` |
| W-12 | [`11-traceability-matrix.md`](11-traceability-matrix.md) §6 "Secrecy Guard" row | "Regex filter **preventing** verbal output of…" | Regex filtering is best effort, and TalkBack output is not filtered. | "Regex filter that scrubs known secret patterns from app TTS output." | `@kisajaBeddawala` |

Self-review: `08-threat-model.md` and this document were checked with the same rule. One absolute statement (TM-12, "the key cannot be exported") was reworded to "is designed not to be exportable".

---

## 7. Remaining #58 work (after the backend is integrated)

- [ ] Run the #57 suite against the integrated build and attach `tests/security/results/security-test-report.md`.
- [ ] Update every **Pending** row in §4.1 with implementation links, SEC test results, date/commit, final status and residual risk.
- [ ] Re-check F-01…F-13 and W-01…W-12, and record which were fixed.
- [ ] Add the security findings to `docs/11-traceability-matrix.md` and hand a summary to the group design (#61).
