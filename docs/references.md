# Authoritative References & Architectural Justifications

* **Project:** Miranova Accessible Multi-Factor Authentication
* **Last Updated:** 2026-10-13
* **Lead Maintainer:** B. G. K. P. Beddawala (`@kisajaBeddawala`)

---

## 1. Accessibility & Human Factors

### REF-ACC-01: Google Android App Accessibility Guidelines
* **Author / Publisher:** Google Android Open Source Project
* **URL:** [https://developer.android.com/guide/topics/ui/accessibility/apps](https://developer.android.com/guide/topics/ui/accessibility/apps)
* **Relevance & Justification:** Establishes Android platform norms for screen reader navigation, touch target bounding boxes ($\ge 48 \times 48\,\text{dp}$), TalkBack traversal indices, and live region announcements.

### REF-ACC-02: Jetpack Compose Accessibility Semantics
* **Author / Publisher:** Android Jetpack Engineering Team
* **URL:** [https://developer.android.com/jetpack/compose/accessibility](https://developer.android.com/jetpack/compose/accessibility)
* **Relevance & Justification:** Provides the technical implementation standard for composing accessibility trees, merging node descendants (`mergeDescendants = true`), assigning roles, defining custom actions, and configuring live region politeness.

### REF-ACC-03: W3C Web Content Accessibility Guidelines (WCAG) 2.2
* **Author / Publisher:** World Wide Web Consortium (W3C Recommendation, Oct 2023)
* **URL:** [https://www.w3.org/TR/WCAG22/](https://www.w3.org/TR/WCAG22/)
* **Relevance & Justification:** Universal standard for digital accessibility. The Miranova architecture conforms to Level AA criteria including Non-text Content (1.1.1), Contrast Ratios (1.4.3), Focus Order (2.4.3), Target Size (2.5.8), and Status Messages (4.1.3).

### REF-ACC-04: WCAG 2.2 Success Criterion 3.3.8: Accessible Authentication (Minimum)
* **Author / Publisher:** W3C Web Accessibility Initiative (WAI)
* **URL:** [https://www.w3.org/WAI/WCAG22/Understanding/accessible-authentication-minimum](https://www.w3.org/WAI/WCAG22/Understanding/accessible-authentication-minimum)
* **Relevance & Justification:** Mandates that authentication processes must not rely on cognitive function tests (such as remembering complex strings, solving puzzles, or transcribing OTP codes).

---

## 2. Android Architecture & Networking

### REF-AND-01: Android Emulator Virtual Networking
* **Author / Publisher:** Android Studio Documentation Team
* **URL:** [https://developer.android.com/studio/run/emulator-networking](https://developer.android.com/studio/run/emulator-networking)
* **Relevance & Justification:** Documents the router/firewall network architecture of the Android emulator and specifies `10.0.2.2` as the special alias IP to access the host loopback interface (`127.0.0.1`). Governs `AppConfig.DEFAULT_EMULATOR_BASE_URL`.

### REF-AND-02: Navigation in Jetpack Compose
* **Author / Publisher:** Android Jetpack Documentation
* **URL:** [https://developer.android.com/guide/navigation/design](https://developer.android.com/guide/navigation/design)
* **Relevance & Justification:** Architecture guide for single-activity Compose applications using `NavHost`, managing deep links, back stacks, and decoupled route definitions.

---

## 3. Cryptography & Hardware Security

### REF-SEC-01: Android Keystore System
* **Author / Publisher:** Android Security Team
* **URL:** [https://developer.android.com/privacy-and-security/keystore](https://developer.android.com/privacy-and-security/keystore)
* **Relevance & Justification:** Authoritative reference for generating non-exportable hardware-backed asymmetric keys within Secure Element / StrongBox / TEE. Governs `AndroidKeyStoreManager` and `KeyGenParameterSpec` configurations.

### REF-SEC-02: NIST SP 800-63B: Digital Identity Guidelines — Authentication and Lifecycle Management
* **Author / Publisher:** National Institute of Standards and Technology (NIST)
* **URL:** [https://pages.nist.gov/800-63-3/sp800-63b.html](https://pages.nist.gov/800-63-3/sp800-63b.html)
* **Relevance & Justification:** Establishes Authenticator Assurance Level 3 (AAL3) requirements for hardware-backed cryptographic proof-of-possession tokens, replay-resistant challenge-response protocols, and device-bound credentials. Governs `CanonicalChallenge` and `ChallengeSigner`.

### REF-SEC-03: FIPS 186-5: Digital Signature Standard (DSS)
* **Author / Publisher:** National Institute of Standards and Technology (NIST)
* **URL:** [https://csrc.nist.gov/pubs/fips/186-5/final](https://csrc.nist.gov/pubs/fips/186-5/final)
* **Relevance & Justification:** Defines the Elliptic Curve Digital Signature Algorithm (ECDSA) specification using the NIST P-256 (`secp256r1`) curve and SHA-256 digest (`SHA256withECDSA`).

### REF-SEC-04: Android Hardware-Backed Keystore & KeyMint Architecture
* **Author / Publisher:** Android Open Source Project (AOSP) Security
* **URL:** [https://source.android.com/docs/security/features/keystore](https://source.android.com/docs/security/features/keystore)
* **Relevance & Justification:** Details the hardware abstraction layer (HAL) interface between user-space Android OS and isolated execution environments (TrustZone TEE and dedicated StrongBox Secure Elements). Governs `SecurityLevel` discovery and `KeyPermanentlyInvalidatedException` handling.

---

## 4. UI Accessibility, Semantics & Multimodal Design (Issue #52)

### REF-UI-01: WCAG 2.2 Success Criteria 2.1.2, 2.5.8, and 4.1.3
* **Author / Publisher:** World Wide Web Consortium (W3C Recommendation, 2023)
* **URL:** [https://www.w3.org/TR/WCAG22/](https://www.w3.org/TR/WCAG22/)
* **Relevance & Justification:** Mandates No Keyboard/Focus Trap (2.1.2), Minimum Target Size of $48 \times 48\,\text{dp}$ (2.5.8), and dynamic asynchronous status announcements via live regions (4.1.3). Governs `AccessibleLoadingIndicator` and `AccessibleStatusBanner`.

### REF-UI-02: Android Developers: Jetpack Compose Accessibility Architecture
* **Author / Publisher:** Android Open Source Project (AOSP) Developer Relations
* **URL:** [https://developer.android.com/develop/ui/compose/accessibility](https://developer.android.com/develop/ui/compose/accessibility)
* **Relevance & Justification:** Defines Compose semantic properties including `invisibleToUser()`, `isTraversalGroup`, `traversalIndex`, `mergeDescendants`, and `liveRegion`. Governs `AccessibilityModifiers.kt` and `AuthChallengeScreen`.

### REF-UI-03: Android VibrationEffect & Audio USAGE_ASSISTANCE_SONIFICATION
* **Author / Publisher:** Android Open Source Project (AOSP) Media & Frameworks
* **URL:** [https://developer.android.com/reference/android/os/VibrationEffect](https://developer.android.com/reference/android/os/VibrationEffect)
* **Relevance & Justification:** Standardizes linear resonant actuator (LRA) waveform pulse synthesis and sonification stream routing without TalkBack speech attenuation. Governs `HapticFeedbackManager` and `EarconManager`.

---

## 5. Device Registration & Credential Lifecycle (Issue #51)

### REF-ENR-01: NIST SP 800-63B §5.1: Authenticator Registration & Binding
* **Author / Publisher:** National Institute of Standards and Technology (NIST)
* **URL:** [https://pages.nist.gov/800-63-3/sp800-63b.html#authenticator-reg](https://pages.nist.gov/800-63-3/sp800-63b.html#authenticator-reg)
* **Relevance & Justification:** Mandates atomic authenticator binding where hardware cryptographic keys generated on client devices must either successfully complete enrollment with the verifier or be revoked/deleted without leaving orphaned credentials. Governs `EnrollmentRepository.registerDevice` rollback semantics.

### REF-ENR-02: W3C Web Authentication (WebAuthn) Credential Registration & Exclusion
* **Author / Publisher:** W3C / FIDO Alliance
* **URL:** [https://www.w3.org/TR/webauthn-2/#sctn-registering-a-new-credential](https://www.w3.org/TR/webauthn-2/#sctn-registering-a-new-credential)
* **Relevance & Justification:** Defines best practices for public key credential creation, user verification policy (`requireUserAuth`), and authenticator re-registration flows upon key invalidation. Governs `DeviceRegistrationPayload` and `ReEnrollmentScreen`.


