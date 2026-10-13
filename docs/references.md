# Authoritative References & Architectural Justifications

* **Project:** Miranova Accessible Multi-Factor Authentication
* **Last Updated:** 2026-10-13
* **Lead Maintainer:** B. G. K. P. Beddawala (`@kisajaBeddawala`)

---

## 1. Accessibility & Human Factors

### REF-ACC-01: Google Android App Accessibility Guidelines
* **Author / Publisher:** Google Android Open Source Project
* **URL:** [https://developer.android.com/guide/topics/ui/accessibility/apps](https://developer.android.com/guide/topics/ui/accessibility/apps)
* **Relevance & Justification:** Establishes Android platform norms for screen reader navigation, touch target bounding boxes ($\ge 48 \times 48\,\text{dp}$), TalkBack traversal indices, and live region announcements. Directly justifies requirements `ACC-01`, `ACC-02`, and `ACC-06` in [`docs/05-accessibility-design.md`](file:///c:/Users/Kisaja%20Beddawala/Desktop/Projects/miranova-accessible-mfa/docs/05-accessibility-design.md).

### REF-ACC-02: Jetpack Compose Accessibility Semantics
* **Author / Publisher:** Android Jetpack Engineering Team
* **URL:** [https://developer.android.com/jetpack/compose/accessibility](https://developer.android.com/jetpack/compose/accessibility)
* **Relevance & Justification:** Provides the technical implementation standard for composing accessibility trees, merging node descendants (`mergeDescendants = true`), assigning roles, defining custom actions, and configuring live region politeness. Governs Android UI implementation in Issue #41 and #52.

### REF-ACC-03: W3C Web Content Accessibility Guidelines (WCAG) 2.2
* **Author / Publisher:** World Wide Web Consortium (W3C Recommendation, Oct 2023)
* **URL:** [https://www.w3.org/TR/WCAG22/](https://www.w3.org/TR/WCAG22/)
* **Relevance & Justification:** Universal standard for digital accessibility. The Miranova architecture conforms to Level AA criteria including Non-text Content (1.1.1), Contrast Ratios (1.4.3), Focus Order (2.4.3), Target Size (2.5.8), and Status Messages (4.1.3).

### REF-ACC-04: WCAG 2.2 Success Criterion 3.3.8: Accessible Authentication (Minimum)
* **Author / Publisher:** W3C Web Accessibility Initiative (WAI)
* **URL:** [https://www.w3.org/WAI/WCAG22/Understanding/accessible-authentication-minimum](https://www.w3.org/WAI/WCAG22/Understanding/accessible-authentication-minimum)
* **Relevance & Justification:** Mandates that authentication processes must not rely on cognitive function tests (such as remembering complex strings, solving puzzles, or transcribing OTP codes) unless an alternative or assistive mechanism is provided. Justifies Miranova's design choice of hardware-backed public-key challenge signing over SMS/TOTP transcription.

---

## 2. Cryptography & Hardware Security

### REF-SEC-01: Android Keystore System
* **Author / Publisher:** Android Security Team
* **URL:** [https://developer.android.com/privacy-and-security/keystore](https://developer.android.com/privacy-and-security/keystore)
* **Relevance & Justification:** Authoritative reference for generating non-exportable hardware-backed asymmetric keys within Secure Element / StrongBox / TEE. Justifies device-bound key creation in [`ADR-003`](file:///c:/Users/Kisaja%20Beddawala/Desktop/Projects/miranova-accessible-mfa/docs/adr) and Issue #50.

### REF-SEC-02: NIST Special Publication 800-63B: Digital Identity Guidelines
* **Author / Publisher:** National Institute of Standards and Technology (NIST)
* **URL:** [https://pages.nist.gov/800-63-3/sp800-63b.html](https://pages.nist.gov/800-63-3/sp800-63b.html)
* **Relevance & Justification:** Defines multi-factor authentication (MFA) authenticator assurance levels (AAL2/AAL3). Classifies cryptographic multi-factor devices and specifies resistance against replay, verifier impersonation, and eavesdropping.

---

## 3. Multimodal & Audio Feedback

### REF-HAP-01: Android Haptics and VibrationEffect API
* **Author / Publisher:** Google Android Framework Team
* **URL:** [https://developer.android.com/reference/android/os/VibrationEffect](https://developer.android.com/reference/android/os/VibrationEffect)
* **Relevance & Justification:** Standardizes dynamic tactile feedback waveforms (`createWaveform`) and one-shot pulses for Linear Resonant Actuators on Android API 26+. Governs patterns `HAP-01` through `HAP-04` in Section 7 of [`docs/05-accessibility-design.md`](file:///c:/Users/Kisaja%20Beddawala/Desktop/Projects/miranova-accessible-mfa/docs/05-accessibility-design.md).
