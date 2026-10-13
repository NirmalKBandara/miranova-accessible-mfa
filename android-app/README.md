# Miranova Accessible MFA — Android Application

Android client application for the **Miranova Accessible Multi-Factor Authentication** system. Engineered with a **TalkBack-First** accessibility architecture, Jetpack Compose, device-bound hardware cryptography (Android Keystore), and resilient backend communication.

---

## 1. Technical Stack & Prerequisites

* **Operating System:** Android 8.0+ (API level 26 minimum, API level 35 target)
* **Language & Runtime:** Kotlin 2.0.21, OpenJDK 17 LTS
* **UI Framework:** Jetpack Compose (BOM `2024.10.01`) with Material 3
* **Build System:** Gradle 8.14, Android Gradle Plugin (AGP) 8.7.3
* **Networking:** OkHttp 4.12.0
* **Testing:** JUnit 4, AndroidX Test, Compose UI Test

---

## 2. Architecture Overview

The application follows a clean, lightweight architecture avoiding unnecessary framework complexity:

```
android-app/
├── app/
│   ├── src/main/
│   │   ├── AndroidManifest.xml
│   │   └── java/com/miranova/mfa/
│   │       ├── MainActivity.kt                  # Root activity with Edge-to-Edge & MiranovaTheme
│   │       ├── data/
│   │       │   ├── config/AppConfig.kt          # Configurable backend base URL & endpoints
│   │       │   ├── network/NetworkClient.kt     # OkHttp client with timeouts & NetworkResult
│   │       │   ├── network/model/               # Data models (HealthResponse, etc.)
│   │       │   └── repository/HealthRepository.kt # Endpoint health checking
│   │       └── ui/
│   │           ├── accessibility/               # Compose modifiers for >=48dp touch targets,
│   │           │   └── AccessibilityModifiers.kt # headings, live regions, and traversal groups
│   │           ├── navigation/
│   │           │   ├── Screen.kt                # Routes: Enrollment, Challenge, Home, Recovery
│   │           │   └── NavGraph.kt              # Accessible Compose NavHost
│   │           ├── screens/                     # TalkBack-first accessible screen composables
│   │           └── theme/                       # Color (>=4.5:1 contrast), Type (200% zoom), Theme
│   └── src/test/                                # Unit test suite (AppConfig, Routes, NetworkResult)
├── gradle/
│   └── libs.versions.toml                       # Centralized Version Catalog
├── build.gradle.kts
└── settings.gradle.kts
```

---

## 3. Build & Test Commands

From the `android-app/` directory (or using local Gradle):

### Run Unit Tests
```bash
gradle testDebugUnitTest
# or with Gradle wrapper:
./gradlew testDebugUnitTest
```

### Build Debug APK
```bash
gradle assembleDebug
# or:
./gradlew assembleDebug
```
Output artifact: `app/build/outputs/apk/debug/app-debug.apk`

### Run Lint & Static Checks
```bash
gradle lintDebug
```

---

## 4. Backend Environment Configuration

The application communicates with the Miranova FastAPI backend. The base URL is centrally managed in [`AppConfig.kt`](file:///c:/Users/Kisaja%20Beddawala/Desktop/Projects/miranova-accessible-mfa/android-app/app/src/main/java/com/miranova/mfa/data/config/AppConfig.kt):

| Environment | Base URL | Notes |
| :--- | :--- | :--- |
| **Android Emulator** | `http://10.0.2.2:8000` *(Default)* | Maps loopback alias to host machine running Docker/FastAPI. |
| **Physical Test Device** | `http://<HOST_IP>:8000` | Requires phone and host laptop to share the same local Wi-Fi LAN. |
| **Production / Staging** | `https://api.miranova.example.com` | TLS encrypted endpoint. |

To verify backend connectivity from the app:
1. Launch the application.
2. Complete enrollment or navigate to **Ready Screen**.
3. Activate the **"Verify Backend Connection"** button. The screen reader will announce the live region status (`"Backend healthy: ok"` or error diagnostic).

---

## 5. Accessibility & TalkBack Verification

1. **Enable Google TalkBack:** Settings $\rightarrow$ Accessibility $\rightarrow$ TalkBack $\rightarrow$ On.
2. **Linear Navigation:** Swipe right with one finger to sequentially traverse:
   * Screen Heading (announced as Heading, index 0).
   * Context / Input cards.
   * Primary Action ($\ge 48 \times 48\,\text{dp}$ touch target).
   * Secondary Action.
3. **Screen Curtain Test:** Triple-tap with three fingers (or TalkBack menu $\rightarrow$ Turn screen curtain on). Complete the enrollment and authentication challenge flows non-visually.
