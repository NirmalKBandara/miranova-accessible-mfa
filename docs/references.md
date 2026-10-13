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

