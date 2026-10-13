# 01. Project Assumptions Baseline

* **Project:** Miranova Accessible Multi-Factor Authentication
* **Baseline Date:** 2026-10-13
* **Contributors:** 
  * B. G. K. P. Beddawala (`@kisajaBeddawala`) — Android Application & Accessibility Lead

---

## 1. Accessibility & Client Assumptions

| Assumption ID | Statement | Rationale / Evidence | Risk if False | Validation Method |
| :--- | :--- | :--- | :--- | :--- |
| **ASM-ACC-01** | Visually impaired Android users navigate using Google TalkBack with linear swipe and exploration-by-touch gestures. | Primary Android screen reader pre-installed on Android 8.0+ devices worldwide; standard accessibility service supported by Jetpack Compose. | User cannot discover or activate custom non-standard gestures without specialized training. | Manual usability testing with TalkBack enabled; verify linear traversal across all screens. |
| **ASM-ACC-02** | Spoken credential input (e.g., speaking PINs or passwords aloud) presents severe privacy and security risks in public environments. | Speaking secrets aloud enables acoustic eavesdropping, bystander interception, and violates privacy in shared workplaces/public transit. | Users are exposed to credential theft or refuse to use the application in public. | Architectural decision record ([ADR-005](file:///c:/Users/Kisaja%20Beddawala/Desktop/Projects/miranova-accessible-mfa/docs/adr/ADR-005-accessibility-strategy.md)); prohibit voice secret input; enforce silent on-device verification. |
| **ASM-ACC-03** | Touch targets of at least $48 \times 48\,\text{dp}$ with $\ge 8\,\text{dp}$ margins allow accurate activation by blind and motor-impaired users. | Google Material Design Accessibility Guidelines and WCAG 2.2 Target Size Success Criterion 2.5.8. | Increased touch exploration errors, accidental activation of adjacent controls, user frustration. | Automated testing with Android Accessibility Scanner and Compose UI Semantics assertions. |
| **ASM-ACC-04** | Short haptic vibration pulses and non-speech earcons provide distinguishable confirmation without interfering with TalkBack speech synthesis. | Linear resonant actuators (LRA) on Android API 26+ can reproduce distinct waveforms (`VibrationEffect`); short earcons (<250ms) avoid speech collision. | Feedback goes unnoticed in loud environments or annoys users by interrupting TalkBack announcements. | Testing with TalkBack speech rate set to 1.5x; verify audio stream separation and haptic perception across physical test devices. |
| **ASM-ACC-05** | Android Keystore cryptographic challenge signing completely eliminates the need for cognitive tests (e.g., memorizing or transcribing rotating OTP digits). | WCAG 2.2 Accessible Authentication (Minimum) Criterion 3.3.8 explicitly favors object-recognition or device-bound cryptographic tokens over transcription. | Cognitive overload and manual transcription errors for visually impaired users trying to read and re-type 6-digit codes. | End-to-end challenge-response testing; verify one-tap/biometric signing without OTP transcription. |

---

## 2. Android Client Platform & Environment Assumptions

| Assumption ID | Statement | Rationale / Evidence | Risk if False | Validation Method |
| :--- | :--- | :--- | :--- | :--- |
| **ASM-AND-01** | The application targets Android API 26 (Android 8.0) as the minimum SDK and API 35 as the compile/target SDK, utilizing OpenJDK 17. | API 26 provides native `VibrationEffect`, Android Keystore modern algorithms, and baseline Compose runtime compatibility across >95% of active Android devices. | Legacy devices (< Android 8) cannot run the application. | Documented project constraints in charter; build script configuration `minSdk = 26`, `compileSdk = 35`. |
| **ASM-AND-02** | The Android emulator alias IP `10.0.2.2:8000` transparently maps to `localhost:8000` on the host machine where the FastAPI backend service runs. | Standard Android Studio emulator virtual networking architecture. | Emulator cannot communicate with backend service during local demonstration. | Automated health-check client testing via `HealthRepository` and configurable base URL fallback in `AppConfig`. |

---

## 3. Cryptographic & Keystore Assumptions (Issue #50)

| Assumption ID | Statement | Rationale / Evidence | Risk if False | Validation Method |
| :--- | :--- | :--- | :--- | :--- |
| **ASM-SEC-01** | Asymmetric private keys generated within Android Keystore provider are hardware-bound and non-exportable; plaintext key bytes cannot be extracted by user-space applications or root compromise. | Android Keystore design guarantees that private key operations execute inside isolated hardware (TEE or StrongBox SE); `privateKey.encoded` returns `null` on hardware-backed devices. | Attacker copies or clones authenticator private key across devices. | Verification in `AndroidKeyStoreManager` checking non-exportability and `KeyInfo.isInsideSecureHardware` / `securityLevel`. |
| **ASM-SEC-02** | Device lock screen credential removal or biometric alteration permanently invalidates authorization-bound cryptographic keys (`setInvalidatedByBiometricEnrollment(true)`). | Android Keystore hardware Keymaster/KeyMint invalidation policy raises `KeyPermanentlyInvalidatedException` if the security boundary is altered. | Unauthorized user accesses device after credential wipe and uses existing MFA keys. | Tested in `KeyStoreManagerTest` and trapped by `KeyLifecycleManager` returning `KeyStatus.PERMANENTLY_INVALIDATED`. |
| **ASM-SEC-03** | Canonical challenge message encoding (`miranova:auth:v1:<nonce>:<timestamp>:<origin>:<action>`) combined with SHA-256 with ECDSA signing guarantees non-repudiation and prevents cross-protocol replay attacks. | NIST SP 800-63B §5.1.4 and FIPS 186-5 require structured challenge framing with cryptographic nonce, origin binding, and replay freshness checks. | Man-in-the-middle or malicious proxy replays intercepted signatures or modifies origin context. | Unit testing in `CanonicalChallengeTest` ensuring exact deterministic string and byte representation. |

---

## 4. TalkBack-First UI & Interaction Assumptions (Issue #52)

| Assumption ID | Statement | Rationale / Evidence | Risk if False | Validation Method |
| :--- | :--- | :--- | :--- | :--- |
| **ASM-UI-01** | Excluding visual progress spinners from the Compose accessibility tree (`invisibleToUser()`) while using decoupled polite live regions prevents TalkBack focus traps during asynchronous cryptographic operations. | WCAG 2.2 Criterion 2.1.2 (No Keyboard Trap) and Android TalkBack interaction guidelines; spinners steal continuous focus if announced as active animated nodes. | Blind users become trapped in rotating progress loops and cannot reach cancel/recovery controls. | Unit & Compose semantics testing in `AccessibleLoadingIndicator` verifying exclusion from semantic hierarchy. |
| **ASM-UI-02** | Milestone-only countdown announcements (arrival, 30s, 10s) coupled with subtle tactile vibration (`HAP-04`) provide temporal awareness without overwhelming TalkBack speech channels. | Continuous 1-second spoken announcements drown out TalkBack navigation gestures and user focus exploration. | Speech collision, cognitive overload, inability to read challenge context details. | Verification in `ChallengeContextCard` and `MultimodalFeedbackManagerTest`. |
| **ASM-UI-03** | Sanitized, actionable error recovery dialogs stating the next safe action protect secrecy by preventing leakage of private key bytes, session cookies, or raw server traces. | Acceptance criterion for Issue #52 and OWASP Mobile Top 10 M4 (Insufficient Secrecy / Sensitive Data Exposure). | Eavesdropping bystanders or screen recording intercepts internal tokens or key material. | Validated in `ActionableErrorTest` and `AccessibleErrorDialog`. |

---

## 5. Device Enrollment & Re-Enrollment Assumptions (Issue #51)

| Assumption ID | Statement | Rationale / Evidence | Risk if False | Validation Method |
| :--- | :--- | :--- | :--- | :--- |
| **ASM-ENR-01** | Atomic Rollback on Registration Failure: If backend registration fails or the user cancels, newly generated Keystore key material is atomically deleted so the device never retains orphaned keys. | NIST SP 800-63B §5.1 authenticator registration requires consistency between client-stored keys and server-registered credentials. Stale orphaned keys create confusion and security drift. | Orphaned keys accumulate in Android Keystore, causing signature failures or re-enrollment lockout. | Verified in `EnrollmentRepositoryTest.registration failure triggers atomic rollback and deletes orphaned keystore key`. |
| **ASM-ENR-02** | Guided Multi-Step Wizard with TalkBack Progress Announcements: Breaking device enrollment into distinct steps (`IDENTITY`, `SECURITY_POLICY`, `CONFIRMATION`) with merged step ratio announcements ("Step X of Y: [Title]") reduces cognitive burden and provides predictable orientation for screen reader users. | WCAG 2.2 Criterion 3.3.8 (Accessible Authentication) and Android accessibility best practices; multi-step chunking prevents cognitive overload compared to long, cluttered single forms. | Screen reader users get disoriented or miss security policy preferences during setup. | Verified in `EnrollmentStepTest` and `AccessibleStepProgressIndicator`. |
| **ASM-ENR-03** | Irreversible Re-Enrollment Warnings & Two-Stage Safety Barrier: Re-enrolling or recovering an invalidated key permanently revokes prior device credentials, requiring explicit confirmation checkboxes and TalkBack warnings before destructive key deletion occurs. | FIPS 186-5 and NIST SP 800-63B §5.2 lifecycle revocation rules; accidental re-enrollment invalidates the user's active second factor across all enrolled systems. | Accidental revocation of authenticators by visually impaired users unable to anticipate destructive side-effects. | Verified in `ReEnrollmentScreen` two-stage dialog and `EnrollmentRepositoryTest.reEnrollDevice revokes old key material`. |

---

## 6. Audio, TTS & Haptic Channel Assumptions (Issue #53)

| Assumption ID | Statement | Rationale / Evidence | Risk if False | Validation Method |
| :--- | :--- | :--- | :--- | :--- |
| **ASM-MUL-01** | Audio Secrecy Guard Enforcement: Outgoing spoken guidance must never articulate hex nonces, base64 key material, or numeric OTP codes aloud into public spaces. | WCAG 2.2 Criterion 3.3.8, Section 8 of accessibility design, and OWASP Mobile M4; acoustic eavesdropping in public places exposes authentication credentials to bystander recording. | Malicious eavesdroppers intercept spoken tokens or blind users suffer security breach in public. | Unit tests in `TtsManagerTest` and `MultimodalFeedbackManagerTest` verifying regex filtering, token scrubbing, and secrecy alert haptic triggering. |
| **ASM-MUL-02** | TalkBack Coexistence & Acoustic Non-Interference: Audio earcons and spoken TTS must never interrupt or drown out TalkBack screen reader exploration. | Google Accessibility Guidelines for assistive audio sonification; competing speech streams disorient blind screen reader users. | Users disable accessibility features due to audio collisions or speech confusion. | Verification of `AudioAttributes.USAGE_ASSISTANCE_SONIFICATION`, TalkBack detection in `TtsManager.isTalkBackActive()`, and polite queuing. |
| **ASM-MUL-03** | Graceful Sensory Degradation & User Sovereignty: Tactile haptics, non-speech earcons, and spoken TTS must operate independently with individual user toggles and degrade silently when hardware is absent or muted. | WCAG 2.2 Criterion 1.4.2 (Audio Control) and ACC-14 (Environmental Degradation); users in meetings or noisy subways require selective channel silencing. | Uncontrollable audio triggers in quiet environments, or crashes on devices lacking hardware vibrators. | Unit tests in `AccessibilityPreferencesTest` and JVM degradation checks in `MultimodalFeedbackManagerTest`. |



---

## 7. Security & Threat Model Assumptions (Issue #38)

* **Contributor:** De Silva N. N. B. (`@Nimsika-Bosilu`) — Threat Modeling & Security Evaluation. See [`08-threat-model.md`](08-threat-model.md).

| Assumption ID | Statement | Rationale / Evidence | Risk if False | Validation Method |
| :--- | :--- | :--- | :--- | :--- |
| **ASM-THR-01** | Attackers know the full source code and API contract, because the repository is public. Security must not depend on hiding endpoints or message formats. | Kerckhoffs's principle; the repository is public on GitHub. | Controls that rely on obscurity fail silently. | Threat model §4 (AT-01); all `tests/security` tests use only the public contract. |
| **ASM-THR-02** | Every field sent by the Android client is attacker-controlled: factor flags, timestamps, `hardware_backed`, `security_level`, and any public key inside a proof. | SRS §6.1 authority rule (FR-02); a modified client or a script can send any JSON. | The server trusts forged client state and grants access without both factors. | SEC-03 and SEC-05 negative tests. |
| **ASM-THR-03** | The prototype runs only on a local/demo network with synthetic accounts. Local HTTP to `10.0.2.2` is a documented development exception and is not evidence of transport security. | ASM-04, SRS §10.4, FR-43. | Results are over-claimed as production transport security. | Residual risk RR-03; configuration review. |
| **ASM-THR-04** | Device private keys are not exportable on hardware-backed devices, but the server **cannot verify** the hardware level because key attestation is not implemented. Emulators report `SOFTWARE`. | Android Keystore documentation (REF-SEC-01); `DeviceRegistrationPayload` sends no attestation chain. | A server-side policy based on `hardware_backed` can be bypassed. | Residual risk RR-02; finding F-03. |
| **ASM-THR-05** | A compromised (rooted or malware-infected) **unlocked** device is outside what the design fully protects against. The key can still be *used* there, even though it cannot be *exported*. | MFA limits the damage of a single compromised factor, not a fully compromised endpoint (SRS §12.1). | Protection against device malware is over-claimed. | Residual risk RR-01; TM-12. |
| **ASM-THR-06** | Security tests (#57) run only against the team's own local backend with synthetic accounts, never against third-party systems. | SRS §12 testing ethics. | Unauthorised testing / ethical breach. | `tests/security/README.md`; `MIRANOVA_BASE_URL` defaults to localhost. |
