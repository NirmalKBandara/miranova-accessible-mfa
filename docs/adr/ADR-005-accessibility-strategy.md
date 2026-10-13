# ADR-005: TalkBack-First Screen Reader Interaction and Multimodal Accessibility Strategy

* **Status:** Accepted
* **Date:** 2026-10-13
* **Author:** B. G. K. P. Beddawala (`@kisajaBeddawala`) - Android Application & Accessibility Lead
* **Related Issues:** #36 (Define accessibility requirements), #41 (Android bootstrap), #52 (TalkBack UI), #53 (Audio/TTS & Haptics), #56 (Accessibility testing)
* **Related ADRs:** ADR-001 (Authentication Method), ADR-002 (Reconcile / Remove Voice Biometrics), ADR-003 (Android Keystore)

---

## 1. Context and Problem Statement

The Miranova Accessible MFA project is designed to provide secure, robust multi-factor authentication for visually impaired and blind users on Android. 

Prior early conceptual explorations considered voice biometrics and voice-only conversational agents as the core authentication mechanism. However, practical and security analysis reveals severe limitations with voice-centric authentication:
1. **Security & Eavesdropping:** Speaking passcodes, PINs, or confidential phrases aloud in public or shared spaces exposes credentials to shoulder-surfing, acoustic eavesdropping, and recording.
2. **Environmental Fragility:** Ambient noise (traffic, transit, office background, wind) sharply degrades Automated Speech Recognition (ASR) accuracy, causing unacceptable false-rejection rates during time-sensitive MFA challenges.
3. **Biological & Physical Variability:** Temporary vocal cord strain, colds, laryngeal illness, or aging alter acoustic voice characteristics, leading to lockout under biometric models.
4. **Synthetic Spoofing:** Generative AI and voice cloning techniques introduce significant spoofing vulnerabilities against acoustic voice biometrics.
5. **Existing User Familiarity:** Visually impaired Android users already rely daily on **Google TalkBack** (Android's standard screen reader) and established exploration gestures (linear swiping, touch exploration, rotor control). Introducing an idiosyncratic, non-standard voice loop breaks standard accessibility paradigms and disables hardware-based security controls.

Therefore, the system requires an explicit architectural decision defining how accessibility is implemented, structured, and validated across the Android application.

---

## 2. Decision

We decide to adopt a **TalkBack-First Screen Reader Interaction Strategy** complemented by non-intrusive **Multimodal Feedback** (haptics and audio earcons) and **Cryptographic Device Binding**:

### 2.1 Native TalkBack-First Architecture
* All Android UI components must be implemented using **Jetpack Compose Semantics APIs** (`Modifier.semantics`, `contentDescription`, `role`, `stateDescription`, `traversalIndex`, `liveRegion`).
* Visual appearance is treated as a secondary representation; the accessibility tree (semantics node hierarchy) is treated as the primary interface contract.
* Standard touch and gesture exploration patterns supported by Android Accessibility Services must be preserved without interference.

### 2.2 Prohibition of Spoken Secrets & Accessible Credential Entry
* Users must never be required or prompted to speak sensitive credentials (PINs, passwords, cryptographic recovery seeds) into the microphone.
* Credential or confirmation inputs must use accessible on-screen controls (e.g., accessible large-target keypads, biometric confirmation prompts, or single-tap cryptographic challenge approvals) with masked or silent audio feedback to prevent ambient exposure.

### 2.3 Multimodal Feedback Hierarchy
* **Primary Channel (Semantics & Speech):** Android TalkBack handles textual and semantic screen reading, focus announcements, element states, and actionable hints.
* **Secondary Channel (Haptic Patterns):** Distinct, recognizable vibration patterns (`VibrationEffect` / `Vibrator`) provide immediate tactile confirmation for high-stakes events (challenge arrival, successful factor verification, authentication failure, session timeout).
* **Tertiary Channel (Earcons / Audio Cues):** Short, pleasant, non-speech acoustic tones (earcons) provide instant status cues without blocking or talking over TalkBack speech synthesizers.
* **Graceful Degradation:** If audio is muted, vibration is disabled in system settings, or TalkBack is temporarily replaced by braille displays, the core authentication flow remains 100% operable through standard semantic nodes.

### 2.4 WCAG 2.2 Level AA Conformance
The Android application architecture is committed to WCAG 2.2 Level AA compliance, specifically focusing on:
* **WCAG 1.1.1 (Non-text Content):** Every icon, decorative element, or visual indicator has an explicit content description or is marked `invisibleToUser()`.
* **WCAG 1.3.1 (Info and Relationships):** Logical hierarchy, headings, and grouping in Compose semantic trees.
* **WCAG 1.4.1 (Use of Color):** No state (e.g., error, pending, success) is indicated solely by color.
* **WCAG 2.4.3 (Focus Order):** Sequential focus traversal follows strict logical task flow (`Modifier.semantics { traversalIndex = ... }`).
* **WCAG 2.5.5 / 2.5.8 (Target Size):** Minimum interactive touch targets are $\ge 48 \times 48\,\text{dp}$ with $\ge 8\,\text{dp}$ separation.
* **WCAG 3.3.1 & 3.3.3 (Error Handling):** Form and protocol errors are explicitly announced via live regions, identifying the exact issue and immediate recovery action.
* **WCAG 3.3.8 (Accessible Authentication):** Avoid cognitive function tests (such as transcription, puzzle solving, or memorizing complex rotating OTPs); rely on device-bound cryptographic signing and simple user verification.
* **WCAG 4.1.2 (Name, Role, Value):** Every interactive component exposes its purpose, role (Button, Switch, Tab), and dynamic state (Selected, Disabled, Busy).

### 2.5 "Screen Curtain" Operability
The design mandates that the entire enrollment, challenge verification, logout, and recovery journey can be executed with 100% accuracy with visual feedback simulated off (Screen Curtain enabled).

---

## 3. Consequences

### Positive
* **High Reliability & Determinism:** Native Android accessibility services are battle-tested, eliminating acoustic noise sensitivity and speech recognition false-reject rates.
* **Privacy & Security:** Zero spoken secrets prevents eavesdropping in public transport or shared environments.
* **Reduced Cognitive Load:** Users rely on their accustomed TalkBack gesture repertoire rather than learning bespoke gesture/voice mechanics.
* **Automated Testability:** Jetpack Compose accessibility semantics can be tested programmatically in CI via `compose-ui-test`, `AccessibilityScanner`, and Robolectric without requiring specialized audio capture test benches.
* **Full Alignment with ADR-002:** Resolves the architectural drift between voice biometrics and modern cryptographic FIDO2/WebAuthn-style device binding.

### Negative / Trade-offs
* **Strict Semantic Discipline:** Developers must deliberately author custom accessibility semantics for custom Compose components, manage `traversalIndex` order, and maintain live regions.
* **Focus State Management:** Dynamic navigation, progress spinners, and dialog dismissals require careful focus management to prevent TalkBack focus traps or orphaned focus states.

---

## 4. Alternatives Considered

* **Alternative 1: Voice-Only Conversational Interface (ASR + TTS):**
  * *Rejected:* Unusable in loud environments, leaks secrets verbally, fails for speech-impaired individuals, requires continuous network streaming or large on-device speech models, and violates privacy in public settings.
* **Alternative 2: Custom Embedded Screen Reader:**
  * *Rejected:* Interacting via an in-app custom TTS engine rather than the system TalkBack engine conflicts with system accessibility tools, breaks Bluetooth Braille displays, and ignores system-wide user preferences (e.g., custom speech rates, pitch, verbosity).

---

## 5. Compliance and Traceability

| Artifact | Requirement / Standard | Implementation Evidence |
| :--- | :--- | :--- |
| `docs/05-accessibility-design.md` | ACC-01 through ACC-15 | Full accessibility interaction model and semantic contract |
| `docs/10-test-plan.md` | Verification Strategy | TalkBack manual testing checklists and Compose automated semantic assertions |
| `docs/11-traceability-matrix.md` | Traceability Gate | Mapped to WCAG 2.2 AA and Issue #36 |
| Android App Implementation | Jetpack Compose | Semantic tree inspection via Android Accessibility Scanner and Compose Test Rules |

---

## 6. References
* Android App Accessibility Guide: [https://developer.android.com/guide/topics/ui/accessibility/apps](https://developer.android.com/guide/topics/ui/accessibility/apps)
* Jetpack Compose Accessibility: [https://developer.android.com/jetpack/compose/accessibility](https://developer.android.com/jetpack/compose/accessibility)
* W3C WCAG 2.2 Guidelines: [https://www.w3.org/TR/WCAG22/](https://www.w3.org/TR/WCAG22/)
* WCAG 2.2 Accessible Authentication (Minimum): [https://www.w3.org/WAI/WCAG22/Understanding/accessible-authentication-minimum](https://www.w3.org/WAI/WCAG22/Understanding/accessible-authentication-minimum)
