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

---

## 5. Screen-by-Screen User Flows and Screen Reader Interaction Specifications

Every screen in the Miranova application has an explicit accessibility traversal map and speech script:

### 5.1 Flow 1: Device Registration & Factor Enrollment

```
[Screen Title (Heading)]
       │
       ▼
[Username / Identity Input Field]
       │
       ▼
[Biometric Protection Preference Checkbox]
       │
       ▼
[Primary Action: "Generate Device Key and Register"]
       │
       ▼
[Live Region: Asynchronous Registration Feedback]
```

#### Traversal Sequence & Semantic Specifications
1. **Screen Heading:**
   * **Node:** `Text("Miranova Device Enrollment")`
   * **Semantics:** `heading(); traversalIndex = 0f`
   * **Announcement:** *"Miranova Device Enrollment, Heading"*
2. **Username / Account Identifier Input:**
   * **Node:** `OutlinedTextField`
   * **Semantics:** `contentDescription = "Username or account email address"; traversalIndex = 1f`
   * **Behavior:** Automatically exposes `autofillHints` and error descriptions via `stateDescription` when invalid.
3. **Biometric Protection Checkbox:**
   * **Node:** `Row(Modifier.semantics(mergeDescendants = true) { role = Role.Checkbox })`
   * **Semantics:** `stateDescription = if (checked) "Checked: Requires biometric authentication before each challenge" else "Not checked: Allows direct device signature"`
   * **Target:** Minimum $48 \times 48\,\text{dp}$.
4. **Primary Action Button:**
   * **Node:** `Button("Register This Device")`
   * **Semantics:** `role = Role.Button; onClickLabel = "Double tap to generate hardware key in Android Keystore and send public key to Miranova server"`
5. **Live Region Status Announcements:**
   * When user triggers generation: `LiveRegionMode.Polite` $\rightarrow$ *"Generating non-exportable hardware key on your device. Please wait."*
   * When server registers public key: `LiveRegionMode.Assertive` $\rightarrow$ *"Device enrollment complete. Registered key bound to username."*
   * Focus automatically transitions to the Home / Ready screen.

---

### 5.2 Flow 2: Multi-Factor Authentication Challenge & Cryptographic Signing

```
[Screen Title: "Authentication Challenge"]
       │
       ▼
[Challenge Context Card (Merged Semantics)]
 ├─ Service Name / Origin
 ├─ Session / Request ID
 └─ Countdown Timer (Polite periodic update)
       │
       ▼
[Primary Action: "Approve & Sign Challenge"]
       │
       ▼
[Secondary Action: "Reject Request"]
       │
       ▼
[Live Region: Signing State & Result]
```

#### Traversal Sequence & Semantic Specifications
1. **Screen Heading:**
   * **Announcement:** *"Authentication Challenge, Heading. One pending request."*
   * Initial focus lands here on challenge push receipt.
2. **Challenge Context Card:**
   * **Container:** `Card(Modifier.semantics(mergeDescendants = true) { ... })`
   * **Semantic Text:** *"Authentication request from Internal Portal. Request ID 8831. Challenge expires in 45 seconds."*
   * **Timer Behavior:** The remaining time is **not** spoken every single second (which creates intolerable TalkBack verbosity). It is announced only at milestones (e.g., arrival, 30s remaining, 10s remaining) via a polite live region.
3. **Approve Action:**
   * **Node:** `Button("Approve and Sign")`
   * **Semantics:** `role = Role.Button; onClickLabel = "Double tap to cryptographically sign challenge and approve authentication"`
   * **Target Bounds:** Full width, height $56\,\text{dp}$ ($\ge 48\,\text{dp}$).
4. **Reject Action:**
   * **Node:** `OutlinedButton("Reject")`
   * **Semantics:** `role = Role.Button; onClickLabel = "Double tap to decline this authentication request and flag as unrecognized"`
5. **BiometricPrompt / OS Credential Overlay:**
   * If key requires user presence: System `BiometricPrompt` triggers. TalkBack handles the native OS biometric dialog natively.
   * On dialog completion/cancellation: Miranova explicitly regains window focus and announces the result.
6. **Signing & Verification Announcements:**
   * During signing: `LiveRegionMode.Polite` $\rightarrow$ *"Signing challenge with device key. Verifying with server."*
   * Verification Success: `LiveRegionMode.Assertive` $\rightarrow$ *"Authentication approved. Session established."* Accompanied by tactile haptic confirmation.

---

### 5.3 Flow 3: Error Handling, Expiry, and Recovery

#### Scenario A: Challenge Expiration (Timeout)
1. Live region immediately fires: *"Challenge expired after 60 seconds without signature. Request was aborted."*
2. An accessible alert dialog appears.
3. Focus is programmatically moved to the dialog heading: *"Challenge Timed Out"*.
4. Action button provided: *"Dismiss and Return"* (`Role.Button`, $\ge 48\,\text{dp}$).

#### Scenario B: Network Failure / Backend Unavailable
1. Error Announcement: *"Network connection error. Server could not be reached. Verify your network connection."*
2. Primary button: *"Retry Connection"* (`onClickLabel = "Attempts to re-contact the authentication service"`).
3. Focus is held on the Retry button, preventing lost or reset focus.

#### Scenario C: Key Invalidation / Re-enrollment UX
1. If the underlying Android Keystore key is revoked or invalidated by OS lock-screen reset:
2. Announcement: *"Security alert: Device hardware key is no longer valid. Re-enrollment is required to restore access."*
3. Primary button: *"Start Device Re-Enrollment"*, immediately guiding the user to the accessible enrollment flow without leaving them trapped in a broken state.

---

## 6. Focus Management, Anti-Trap Rules, and Live Region Policy

### 6.1 Initial Focus Placement
Upon any Compose navigation event (`NavController.navigate`):
* Focus must be programmatically transferred to the top-level screen heading via `FocusRequester.requestFocus()`.
* TalkBack will immediately identify the new context to the blind user rather than remaining stranded at the bottom of the previous screen.

### 6.2 Focus Restoration
* When an alert dialog, modal bottom sheet, or BiometricPrompt is dismissed, focus must return to the exact UI element that triggered the dialog.
* If the trigger element no longer exists (e.g., challenge consumed), focus shifts to the newly mounted top-level status heading.

### 6.3 Anti-Trap Rules for Asynchronous Operations
* **No Focus Traps in Progress Spinners:** Circular progress indicators (`CircularProgressIndicator`) must be marked `invisibleToUser()` in Compose semantics. 
* Background progress must **never** hold or steal TalkBack focus. Instead, progress is conveyed via decoupled `Modifier.semantics { liveRegion = LiveRegionMode.Polite }` container strings.
* While busy, interactive buttons set their state to `stateDescription = "In progress, please wait"` and `enabled = false`.

### 6.4 Live Region Politeness Levels
| Event Type | Politeness Level | Rationale |
| :--- | :--- | :--- |
| Background key generation progress | `LiveRegionMode.Polite` | Queues announcement after user finishes listening to current control. |
| Time remaining milestone (30s, 10s) | `LiveRegionMode.Polite` | Informs user without interrupting active swipe exploration. |
| Authentication Success / Access Granted | `LiveRegionMode.Assertive` | Critical outcome; immediate delivery required. |
| Authentication Denial / Security Tampering | `LiveRegionMode.Assertive` | Critical safety state; interrupts queue. |
| Session Expiry or Network Drop | `LiveRegionMode.Assertive` | Urgent actionable error requiring user attention. |

---

## 7. Multimodal Feedback Specification (Haptics, Audio Earcons & TTS)

To complement TalkBack screen reader synthesis without competing for audio bandwidth or disclosing sensitive data, Miranova implements a synchronized multimodal feedback system combining tactile vibrations, non-speech earcons, and spoken TTS guidance:

### 7.1 Haptic Feedback Taxonomy (`VibrationEffect` API)
Modern Android devices equipped with linear resonant actuators (LRA) provide nuanced tactile feedback. Miranova standardizes seven discrete haptic patterns:

| Pattern ID | Trigger Event | Waveform Timings (ms) | Amplitude Profile | Tactile Perception |
| :--- | :--- | :--- | :--- | :--- |
| **HAP-01** | Factor Success / Verification Approval | `[0, 30, 40, 30, 40, 50]` | `[0, 160, 0, 200, 0, 255]` | Triple crisp tick with satisfying confirmation crescendo. |
| **HAP-02** | Authentication Challenge Arrival | `[0, 100, 80, 120]` | `[0, 180, 0, 220]` | Prominent double pulse indicating incoming challenge. |
| **HAP-03** | Authentication Rejection / Tamper Alert | `[0, 250]` | `[0, 255]` | Long, heavy warning buzz signaling denied access. |
| **HAP-04** | Timeout Countdown Milestone (10s) | `[0, 40]` | `[0, 140]` | Subtle warning tick nudging user to take action. |
| **HAP-05** | Cautionary Irreversible Warning | `[0, 150, 100, 150]` | `[0, 200, 0, 240]` | Double pulsed cautionary buzz for key revocation / resets. |
| **HAP-06** | Setup Wizard Step Navigation | `[0, 25]` | `[0, 180]` | Crisp single tactile bump when progressing wizard steps. |
| **HAP-07** | Secrecy / Privacy Leak Alert | `[0, 80, 50, 80, 50, 120]` | `[0, 255, 0, 255, 0, 255]` | Firm staccato alarm pulse signaling secret disclosure interception. |

### 7.2 Non-Speech Audio Earcons
Rather than speaking conversational sentences that collide with TalkBack speech synthesizers, Miranova emits brief, distinct non-speech auditory cues (earcons) under 250 milliseconds in duration:

* **EAR-01 (Enrollment Key Ready):** Ascending two-note chime ($C_5 \rightarrow G_5$, 120ms).
* **EAR-02 (Authentication Approved):** Harmonious major chord chime ($C_5 - E_5 - G_5$, 180ms).
* **EAR-03 (Challenge Aborted / Expired):** Descending minor tone ($E_5 \rightarrow B_4$, 200ms).

#### Audio Routing and Politeness Rules
1. Earcons are bundled locally as compressed lossless assets in `res/raw/` (or synthesized dynamically via PCM for testing).
2. Sounds are played using Android `SoundPool` with `AudioAttributes.USAGE_ASSISTANCE_SONIFICATION` on `AudioAttributes.CONTENT_TYPE_SONIFICATION`.
3. The sound stream strictly observes system volume settings and Do Not Disturb (DND) modes.
4. Volume is attenuated to $-6\,\text{dB}$ relative to media volume to guarantee TalkBack speech remains crystal-clear and intelligible above the earcon.

### 7.3 Accessible Spoken Text-to-Speech (TTS) Guidance & Secrecy Guard (`TtsManager`)
For users navigating without an active screen reader, or who require complementary spoken confirmations, Miranova integrates an accessible Text-to-Speech engine (`TtsManager`) that operates alongside the native accessibility tree.

#### Secrecy Protection Rules for Audio Channels
In compliance with WCAG 2.2 Criterion 3.3.8 and Section 8 of this specification:
1. **Zero Secret Utterances:** The speech synthesizer must **never** speak cryptographic nonces, raw key material, session tokens, or numeric OTP codes aloud into the environment (`TtsManager.containsSecret()`).
2. **Deterministic Token Scrubbing:** Any utterance containing sensitive cryptographic bytes is intercepted and scrubbed (`TtsManager.sanitizeUtterance()`), replacing raw tokens with non-disclosing placeholders (e.g. `[protected token]`).
3. **Privacy Alert Waveform:** Attempted secret disclosures trigger tactile `HAP-07` (Secrecy Alert) to notify the user tactilely that a secret output was blocked from public audio broadcast.
4. **Mute & TalkBack Coexistence:** Spoken TTS guidance automatically degrades to silence when system ringer/notification streams are muted or in vibrate mode, and respects TalkBack focus traversal without speech collision.

### 7.4 User Accessibility Preferences & Diagnostics (`SettingsScreen`)
Users maintain complete sovereignty over sensory feedback channels via the dedicated Accessibility Settings & Diagnostics screen:
* **Granular Toggles:** Independent on/off controls for Tactile Haptic Vibration, Non-Speech Audio Chimes (Earcons), and Spoken TTS Guidance.
* **Audition & Diagnostics:** Live test buttons with $\ge 48\,\text{dp}$ touch targets allowing users to feel approval haptics, hear earcons, and verify that the Secrecy Guard properly scrubs simulated tokens.
* **Persistence & Fallback:** Preferences are stored in `SharedPreferencesAccessibilityPreferencesStore` on Android, with a fast in-memory implementation (`MemoryAccessibilityPreferencesStore`) for JVM testing.

---

## 8. Secrecy and Privacy Protection (Non-Verbal Secret Handling)

### 8.1 Prohibition of Spoken Passcodes & PINs
* **Privacy Threat:** In public or workplace settings, any system that requires blind users to speak credentials (e.g., "Miranova, my PIN is 4-8-2-1") creates fatal vulnerabilities to acoustic eavesdropping, directional microphones, and opportunistic shoulder-surfers.
* **Architecture Mandate:** Miranova strictly prohibits verbal/microphone secret capture. User authentication relies on:
  1. Cryptographic proof-of-possession of the device hardware key (Android Keystore).
  2. Local device biometric or PIN verification executed via the secure Android OS `BiometricPrompt`.

### 8.2 Non-Visual Numeric Entry & Concealment
* If a backup PIN or code is required by the factor policy:
  * Keypad targets must be large ($> 64 \times 64\,\text{dp}$).
  * TalkBack must **not** speak the entered digit aloud into the environment when headphones are not connected. Instead, Compose provides a non-informative announcement: *"PIN character entered"* accompanied by a subtle tactile tap (`HAP-04`).
  * Echoing characters aloud is disallowed to protect against ambient eavesdropping.

### 8.3 Screen Curtain Operability (Privacy & Non-Visual Assurance)
* TalkBack includes a "Screen Curtain" feature that disables the display backlight and renders the screen black while keeping touch and screen reader active.
* Visually impaired individuals commonly use Screen Curtain in public spaces to preserve battery and prevent sighted onlookers from viewing banking or identity credentials.
* **Verification Criterion:** All flows (enrollment, approval, rejection, recovery) must be 100% executable under active Screen Curtain mode with zero dependencies on visual feedback.

---

## 9. Environmental Robustness & Fallback Strategy

Authentication frequently occurs under adverse environmental conditions. Miranova guarantees deterministic behavior across all scenarios:

| Operational Scenario | Environmental Barrier | Primary Channel | Secondary Channel | Fallback Guarantee |
| :--- | :--- | :--- | :--- | :--- |
| **Subway / Construction Site** | Extreme ambient acoustic noise ($> 85\,\text{dB}$). Audio cues inaudible. | Haptic feedback (`HAP-01`, `HAP-02`) | TalkBack with earphone | Vibration pattern alerts user; linear swipe guides touch approval. |
| **Executive Meeting / Library** | Device set to Silent/Mute; no audio allowed. | Haptic feedback & TalkBack over headset / Bluetooth Braille display | Visual high-contrast screen | 100% operable with zero audio emission. |
| **Low-End Hardware / Broken Vibrator** | Device lacks haptic motor or user disabled system vibration. | TalkBack speech synthesizer | Optional Earcon chimes | Semantic accessibility tree conveys all status and error transitions. |
| **Incoming Phone Call / Interruption** | Challenge arrival interrupted by cellular call or OS notification. | System notification shade integration | Persistent pending challenge card | On app resume, state is re-queried; if time remains, user resumes seamlessly; if expired, polite timeout dialog is shown. |
| **Offline / Airplane Mode** | No cellular or Wi-Fi connectivity. | TalkBack announcement | High-contrast error banner | Clear immediate notification: *"Network unavailable. Cannot reach Miranova authentication server."* No infinite loading loop. |

---

## 10. Accessibility Verification & Testing Matrix

To guarantee compliance before releases and viva demonstrations, the accessibility matrix defines exact test procedures:

| Test ID | Journey / Screen | Interaction / TalkBack Gesture | Expected Screen Reader Output | Tactile / Audio Cue | Acceptance Verification Criteria |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **TC-ACC-01** | Enrollment: Screen Arrival | Launch App / Navigate to Enrollment | *"Miranova Device Enrollment, Heading. Focus on username input field."* | None | Initial focus lands on heading (or input); title identified as heading. |
| **TC-ACC-02** | Enrollment: Input & Key Gen | Enter username, swipe right to Register, double-tap | 1. *"Generating non-exportable hardware key on your device. Please wait."*<br>2. *"Device enrollment complete. Registered key bound to username."* | `EAR-01` chime upon completion | Polite async progress announcement; assertive confirmation announcement upon success; no focus trap. |
| **TC-ACC-03** | Auth: Challenge Arrival | App foregrounded / push notification received | *"Authentication Challenge, Heading. One pending request."* Focus rests on heading. | `HAP-02` (double pulse) | Prominent tactile arrival alert; challenge card exposes origin, request ID, and expiry window. |
| **TC-ACC-04** | Auth: Swipe Traversal & Focus | Linear swipe right across screen controls | Traverses sequentially: 1. Heading $\rightarrow$ 2. Challenge Summary Card $\rightarrow$ 3. Approve Button $\rightarrow$ 4. Reject Button. | None | Logical reading order (`ACC-06`); no skipped elements; interactive elements announce role. |
| **TC-ACC-05** | Auth: Approve & Sign | Focus on "Approve and Sign", double-tap | 1. *"Signing challenge with device key. Verifying with server."*<br>2. *"Authentication approved. Access granted."* | `HAP-01` (triple tick) + `EAR-02` (chord) | Crypto signature executed; server verifies; simultaneous haptic and audio confirmation. |
| **TC-ACC-06** | Auth: Expiry / Timeout | Idle on challenge screen for 60 seconds | *"Challenge expired after 60 seconds without signature. Request was aborted."* Focus moves to dismiss action. | `EAR-03` descending tone | Timeout live announcement fires immediately; user never left in indefinite pending state. |
| **TC-ACC-07** | Network Drop / Offline | Trigger authentication in Airplane mode | *"Network connection error. Server could not be reached. Verify your network connection."* Focus on Retry. | `HAP-03` buzz | Unambiguous actionable error message; focus pinned to retry control; no silent failure. |
| **TC-ACC-08** | Screen Curtain Operability | Enable TalkBack Screen Curtain (screen black) | Perform full enrollment and authentication using touch exploration and double-tap gestures only. | Full tactile & audio feedback active | User successfully enrolls and completes authentication with display completely off. |


