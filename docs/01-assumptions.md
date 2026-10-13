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


