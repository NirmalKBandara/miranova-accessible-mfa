# 05. Accessibility Design: TalkBack-First Interaction and Semantic Specification

* **Document Owner:** B. G. K. P. Beddawala (`@kisajaBeddawala`) — Android Application & Accessibility Lead
* **Status:** In Progress (Phase 0 Design Baseline)
* **Target Release:** Miranova Accessible MFA Prototype
* **Traceability:** [Issue #36](https://github.com/NirmalKBandara/miranova-accessible-mfa/issues/36), [ADR-005](file:///c:/Users/Kisaja%20Beddawala/Desktop/Projects/miranova-accessible-mfa/docs/adr/ADR-005-accessibility-strategy.md), WCAG 2.2 Level AA

---

## 1. Executive Summary & Philosophy

The Miranova Accessible Multi-Factor Authentication (MFA) client provides accessible, secure, and resilient identity verification for blind and low-vision individuals. 

Prior security prototypes often relied on voice biometrics or unconstrained conversational speech agents under the assumption that "audio equals accessible." In enterprise and daily mobile usage, however, voice-only interfaces fail catastrophically in loud environments, expose secrets to eavesdroppers, and create severe barriers for users with vocal fatigue or speech impediments.

Miranova rejects voice biometrics and voice-only credentials ([ADR-002](file:///c:/Users/Kisaja%20Beddawala/Desktop/Projects/miranova-accessible-mfa/docs/adr/ADR-002-remove-voice-biometrics.md), [ADR-005](file:///c:/Users/Kisaja%20Beddawala/Desktop/Projects/miranova-accessible-mfa/docs/adr/ADR-005-accessibility-strategy.md)). Instead, Miranova treats the **Android TalkBack Accessibility Tree as the primary user interface contract**, backed by device-bound hardware cryptography (Android Keystore) and multimodal feedback (haptic vibrations and audio earcons).

---

## 2. User Personas & Assistive Ecosystems

The interaction architecture is engineered against four realistic operational personas:

### Persona A: Suren — Fully Blind Daily Screen Reader User
* **Visual Profile:** Total blindness / no light perception.
* **Assistive Tech:** Google TalkBack (speed: 1.5x, verbosity: high), linear flick navigation, exploration by touch, optional hardware Bluetooth Braille display.
* **Operational Needs:** Unbroken linear focus order, meaningful semantic roles, descriptive state labels (e.g., "Key ready, pending server challenge" rather than "icon_check"), zero focus traps, and 100% functionality with Screen Curtain (screen off mode).

### Persona B: Maya — Low-Vision & Photophobic User
* **Visual Profile:** Severe visual acuity loss with light sensitivity.
* **Assistive Tech:** Android System Font Scaling (150%–200%), high contrast dark mode, system magnification gestures.
* **Operational Needs:** Fluid layouts without clipped text, strict contrast ratios ($\ge 4.5:1$ for text, $\ge 3:1$ for interactive bounds), minimum $48 \times 48\,\text{dp}$ touch targets with ample spatial separation, and non-reliance on subtle color gradations.

### Persona C: Tariq — Blind Professional in Public Transit / Shared Spaces
* **Visual Profile:** Legally blind.
* **Assistive Tech:** TalkBack with single-ear headphone or tactile feedback.
* **Operational Needs:** Cannot speak authentication codes or PINs aloud without exposing credentials to surrounding passengers. Requires accessible on-device cryptographic verification where approval can be executed silently and securely.

### Persona D: Elena — Situational Multi-Sensory Impairment (Noise & Haptic Muting)
* **Visual Profile:** Visually impaired user navigating high-ambient-noise environments (train platform, cafe) with vibration mode disabled or muted device.
* **Operational Needs:** Graceful fallbacks where missing audio or disabled haptics never prevents completion or causes unknown system states.

---

## 3. Normative Accessibility Requirements

The Android client must satisfy the following explicit, testable accessibility requirements:

| ID | Title | Description | WCAG 2.2 Reference |
| :--- | :--- | :--- | :--- |
| **ACC-01** | **Native TalkBack Parity** | 100% of enrollment, authentication, recovery, and settings workflows must be completable using TalkBack touch exploration and linear swipe gestures without visual cues. | 1.1.1, 4.1.2 |
| **ACC-02** | **Target Size & Spacing** | All interactive touch targets must have a minimum physical touch surface of $48 \times 48\,\text{dp}$ with a minimum $8\,\text{dp}$ margin from adjacent targets. | 2.5.5, 2.5.8 |
| **ACC-03** | **Contrast Ratios** | Text and images of text must achieve a contrast ratio of at least $4.5:1$ against adjacent backgrounds ($3:1$ for large text $\ge 18\,\text{sp}$ bold or $24\,\text{sp}$ regular). UI components and graphical boundaries must achieve at least $3:1$. | 1.4.3, 1.4.11 |
| **ACC-04** | **Color Independence** | Color must not be used as the sole visual or semantic means of conveying information, indicating an action, prompting a response, or distinguishing a visual element. | 1.4.1 |
| **ACC-05** | **Dynamic Font Scaling** | The UI layout must accommodate Android OS font scaling up to $200\%$ without clipping text, truncation without an accessible expander, or overlapping interactive containers. | 1.4.4, 1.4.12 |
| **ACC-06** | **Deterministic Focus Order** | Sequential navigation (`Modifier.semantics { traversalIndex }`) must follow the natural cognitive logic of the task. Screen titles and headings must receive focus first upon screen transitions. | 2.4.3, 1.3.1 |
| **ACC-07** | **Explicit Roles & States** | Every interactive element must expose an explicit semantic role (`Role.Button`, `Role.Switch`, `Role.Tab`) and dynamic state description (`stateDescription = "Enrolled"`, `"Pending approval"`). | 4.1.2 |
| **ACC-08** | **Non-Verbal Secret Entry** | The application must never mandate spoken input for secrets, PINs, or confidential cryptographic tokens. Non-visual PIN/credential entry must provide silent, masked feedback. | 3.3.8 |
| **ACC-09** | **Focus Containment & Anti-Trap** | Loading indicators and asynchronous background operations must never trap or hijack TalkBack focus. Dialog dismissals must return focus to the originating trigger element. | 2.1.2, 2.4.3 |
| **ACC-10** | **Live Region Communication** | Dynamic status updates (e.g., challenge received, biometric ready, timeout countdown) must be communicated via Compose semantic live regions using appropriate politeness levels (`Polite` vs `Assertive`). | 4.1.3 |
| **ACC-11** | **Actionable Error Recovery** | When validation or network errors occur, the screen reader must announce the exact cause and the specific corrective action required. Generic errors (e.g., "Error occurred") are prohibited. | 3.3.1, 3.3.3 |
| **ACC-12** | **Multimodal Confirmation** | Factor completion and high-consequence state changes must trigger synchronized haptic pulses and optional non-speech audio earcons to provide immediate tactile and auditory confirmation. | 1.1.1, 1.3.3 |
| **ACC-13** | **Acoustic Non-Interference** | In-app audio feedback (earcons/chimes) must never duck, interrupt, or drown out system TalkBack speech output. Audio output must be routed through the media stream and respect system mute settings. | 1.4.2 |
| **ACC-14** | **Environmental Degradation** | In the event of system audio muting or haptic motor failure, the authentication flow must remain completely usable and understandable via the semantic accessibility tree alone. | 1.1.1, 4.1.2 |
| **ACC-15** | **Screen Curtain Validation** | The complete end-to-end system test suite must include a 100% pass criterion with the display brightness set to zero / TalkBack Screen Curtain enabled. | 1.1.1 |

---

## 4. Jetpack Compose Accessibility Architecture

The Android user interface is constructed in **Jetpack Compose**. To maintain strict accessibility compliance, all custom Compose components must adhere to the following architecture rules:

### 4.1 Semantic Hierarchy and Merging
* Compound controls comprising an icon, label text, and status badge must merge descendants using `Modifier.semantics(mergeDescendants = true) { ... }` so TalkBack reads them as a single logical, cohesive item rather than fragmented elements.
* Decorative visual elements (background gradients, structural dividers, ambient illustrations) must be excluded from the semantic tree using `Modifier.semantics { invisibleToUser() }` or by passing `contentDescription = null` to `Icon`/`Image`.

### 4.2 Explicit Role and State Descriptions
* Every clickable surface must have an assigned `Role` (e.g., `role = Role.Button`) or use standard accessible composables (`Button`, `IconButton`, `Checkbox`).
* State descriptions must be explicitly updated during state changes:
  ```kotlin
  Modifier.semantics {
      role = Role.Button
      stateDescription = if (isKeyRegistered) "Device key registered" else "Unregistered"
      contentDescription = "Authenticate with registered device key"
  }
  ```

### 4.3 Focus Order Management (`traversalIndex` and `isTraversalGroup`)
* Compose 1.5+ traversal grouping must be used to group related input fields and prevent arbitrary layout-driven focus jumping:
  ```kotlin
  Column(
      modifier = Modifier.semantics { isTraversalGroup = true }
  ) {
      Text(
          text = "Challenge Verification",
          modifier = Modifier.semantics { heading(); traversalIndex = 0f }
      )
      // Controls with sequential indices
  }
  ```

### 4.4 Touch Target Enforcement
* In accordance with `ACC-02`, every clickable composable must enforce a minimum bounds of $48\,\text{dp}$:
  ```kotlin
  Modifier
      .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
      .clickable(
          onClickLabel = "Confirm authentication challenge",
          role = Role.Button
      ) { /* action */ }
  ```
