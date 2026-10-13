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
