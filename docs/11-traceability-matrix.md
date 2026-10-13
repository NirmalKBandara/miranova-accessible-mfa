# 11. Requirement Traceability Matrix

* **Project:** Miranova Accessible Multi-Factor Authentication
* **Baseline Version:** 1.0.0-draft
* **Last Updated:** 2026-10-13

---

## 1. Accessibility & Android Client Traceability

This matrix maps each normative accessibility requirement to its corresponding architectural decision, design specification, implementation component, test case, and responsible owner:

| Req ID | Requirement Statement | Architectural / Design Element | Implementation Target (Android App) | Verification Test Case | Owner |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **ACC-01** | Native TalkBack Parity | [ADR-005](file:///c:/Users/Kisaja%20Beddawala/Desktop/Projects/miranova-accessible-mfa/docs/adr/ADR-005-accessibility-strategy.md), [`05-accessibility-design.md`](file:///c:/Users/Kisaja%20Beddawala/Desktop/Projects/miranova-accessible-mfa/docs/05-accessibility-design.md) §3 | Compose UI Screens (`android-app/`) | `TC-ACC-01`, `TC-ACC-04` | `@kisajaBeddawala` |
| **ACC-02** | Target Size $\ge 48 \times 48\,\text{dp}$ | [`05-accessibility-design.md`](file:///c:/Users/Kisaja%20Beddawala/Desktop/Projects/miranova-accessible-mfa/docs/05-accessibility-design.md) §4.4 | `Modifier.sizeIn(minWidth=48.dp, minHeight=48.dp)` | `TC-ACC-04`, Accessibility Scanner | `@kisajaBeddawala` |
| **ACC-03** | Contrast Ratios $\ge 4.5:1$ text | [`05-accessibility-design.md`](file:///c:/Users/Kisaja%20Beddawala/Desktop/Projects/miranova-accessible-mfa/docs/05-accessibility-design.md) §3 | `MiranovaTheme` Color Palette | Accessibility Scanner automated scan | `@kisajaBeddawala` |
| **ACC-04** | Color Independence | [`05-accessibility-design.md`](file:///c:/Users/Kisaja%20Beddawala/Desktop/Projects/miranova-accessible-mfa/docs/05-accessibility-design.md) §3 | Text badges, icons with semantic labels | Grayscale display test | `@kisajaBeddawala` |
| **ACC-05** | Dynamic Font Scaling up to 200% | [`05-accessibility-design.md`](file:///c:/Users/Kisaja%20Beddawala/Desktop/Projects/miranova-accessible-mfa/docs/05-accessibility-design.md) §3 | Responsive Compose layout containers | Font scale 2.0x emulator test | `@kisajaBeddawala` |
| **ACC-06** | Deterministic Focus Order | [`05-accessibility-design.md`](file:///c:/Users/Kisaja%20Beddawala/Desktop/Projects/miranova-accessible-mfa/docs/05-accessibility-design.md) §4.3, §6.1 | `Modifier.semantics { traversalIndex }` | `TC-ACC-04` linear swipe test | `@kisajaBeddawala` |
| **ACC-07** | Explicit Roles & State Descriptions | [`05-accessibility-design.md`](file:///c:/Users/Kisaja%20Beddawala/Desktop/Projects/miranova-accessible-mfa/docs/05-accessibility-design.md) §4.2 | `role = Role.Button`, `stateDescription` | Compose Semantics Tree assertions | `@kisajaBeddawala` |
| **ACC-08** | Non-Verbal Secret Entry | [ADR-005](file:///c:/Users/Kisaja%20Beddawala/Desktop/Projects/miranova-accessible-mfa/docs/adr/ADR-005-accessibility-strategy.md), [`05-accessibility-design.md`](file:///c:/Users/Kisaja%20Beddawala/Desktop/Projects/miranova-accessible-mfa/docs/05-accessibility-design.md) §8 | Silent on-device signature / masked PIN pad | Eavesdropping privacy inspection | `@kisajaBeddawala` |
| **ACC-09** | Focus Containment & Anti-Trap | [`05-accessibility-design.md`](file:///c:/Users/Kisaja%20Beddawala/Desktop/Projects/miranova-accessible-mfa/docs/05-accessibility-design.md) §6.3 | Spinner `invisibleToUser()`, `FocusRequester` | `TC-ACC-02` async spinner test | `@kisajaBeddawala` |
| **ACC-10** | Live Region Communication | [`05-accessibility-design.md`](file:///c:/Users/Kisaja%20Beddawala/Desktop/Projects/miranova-accessible-mfa/docs/05-accessibility-design.md) §6.4 | `liveRegion = LiveRegionMode.Polite/Assertive` | `TC-ACC-02`, `TC-ACC-05` | `@kisajaBeddawala` |
| **ACC-11** | Actionable Error Recovery | [`05-accessibility-design.md`](file:///c:/Users/Kisaja%20Beddawala/Desktop/Projects/miranova-accessible-mfa/docs/05-accessibility-design.md) §5.3 | Accessible error dialogs & Retry buttons | `TC-ACC-06`, `TC-ACC-07` | `@kisajaBeddawala` |
| **ACC-12** | Multimodal Confirmation | [`05-accessibility-design.md`](file:///c:/Users/Kisaja%20Beddawala/Desktop/Projects/miranova-accessible-mfa/docs/05-accessibility-design.md) §7 | Haptic Manager (`Vibrator`), Sound Manager | `TC-ACC-05` tactile verification | `@kisajaBeddawala` |
| **ACC-13** | Acoustic Non-Interference | [`05-accessibility-design.md`](file:///c:/Users/Kisaja%20Beddawala/Desktop/Projects/miranova-accessible-mfa/docs/05-accessibility-design.md) §7.2 | Attenuated SoundPool playback | Simultaneous TalkBack playback test | `@kisajaBeddawala` |
| **ACC-14** | Environmental Degradation | [`05-accessibility-design.md`](file:///c:/Users/Kisaja%20Beddawala/Desktop/Projects/miranova-accessible-mfa/docs/05-accessibility-design.md) §9 | Decoupled semantic fallback architecture | Muted device / vibration-off test | `@kisajaBeddawala` |
| **ACC-15** | Screen Curtain Operability | [ADR-005](file:///c:/Users/Kisaja%20Beddawala/Desktop/Projects/miranova-accessible-mfa/docs/adr/ADR-005-accessibility-strategy.md), [`05-accessibility-design.md`](file:///c:/Users/Kisaja%20Beddawala/Desktop/Projects/miranova-accessible-mfa/docs/05-accessibility-design.md) §8.3 | 100% Non-visual Compose UI contract | `TC-ACC-08` Screen Curtain E2E test | `@kisajaBeddawala` |
