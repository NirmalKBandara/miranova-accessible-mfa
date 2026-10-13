# ADR-003: Device-Bound Cryptographic Key Generation and Challenge Signing via Android Keystore

* **Status:** Accepted
* **Date:** 2026-10-13
* **Author:** B. G. K. P. Beddawala (`@kisajaBeddawala`) — Android Application & Accessibility Lead
* **Related Issues:** #50 (Android Keystore Signing), #35 (Authentication Protocol), #45 (Challenge Verification), #51 (Enrollment Flows)
* **Related ADRs:** ADR-001 (Authentication Method), ADR-002 (Remove Voice Biometrics), ADR-005 (Accessibility Strategy)

---

## 1. Context and Problem Statement

Multi-Factor Authentication (MFA) requires robust possession of a distinct authentication factor. In the Miranova architecture, the mobile device serves as a hardware-bound cryptographic authenticator.

Storing private keys in traditional software storage (such as Android `SharedPreferences`, encrypted SQLite databases, or local app data directories) exposes credentials to severe extraction attacks:
1. **Device Root / Privilege Escalation:** Rooted devices or kernel vulnerabilities allow malicious processes to inspect app memory and filesystem storage.
2. **Backup & Cloud Extraction:** Android ADB backups or cloud synchronization can unintentionally exfiltrate file-based secrets.
3. **Malware & Side-Channel Leaks:** File-based keys are loaded into application process memory, exposing them to memory dump exploitation.

To satisfy NIST SP 800-63B requirements for cryptographic authenticators without burdening users with manual physical tokens (e.g., USB security keys), the application must generate and utilize asymmetric keys anchored directly within Android's hardware security architecture.

---

## 2. Decision

We decide to implement device binding exclusively using the **Android Keystore System** (`"AndroidKeyStore"` provider) with the following cryptographic and operational parameters:

### 2.1 Cryptographic Algorithm Selection
* **Asymmetric Algorithm:** **ECDSA (Elliptic Curve Digital Signature Algorithm)** using the **NIST P-256 (`secp256r1` / `prime256v1`)** elliptic curve.
* **Digest Algorithm:** **SHA-256** (`KeyProperties.DIGEST_SHA256`).
* **Signature Standard:** **`SHA256withECDSA`** producing IEEE P1363 or standard ASN.1 DER-encoded `(r, s)` signature byte sequences.
* **Key Purposes:** Strictly restricted to **`KeyProperties.PURPOSE_SIGN`**. Key decryption, encryption, and key agreement purposes are disallowed on the signing key.

#### Justification for ECDSA P-256 over RSA
1. **Performance & Battery:** ECDSA P-256 256-bit operations are significantly faster and consume drastically fewer CPU cycles and battery power than equivalent 2048-bit or 4096-bit RSA keys.
2. **Payload Size:** A P-256 public key is 64–91 bytes (in raw or X.509 DER format), and the signature is ~70 bytes, minimizing mobile network bandwidth during MFA challenges.
3. **Universal Android Support:** Hardware acceleration for P-256 is universally supported across Android Trusted Execution Environments (TEE) and Secure Elements (StrongBox) from Android 8.0 through Android 15+.
4. **Standard Alignment:** Aligns with FIDO2 / WebAuthn `ES256` standards and Python `cryptography` backend verification.

### 2.2 Non-Exportable Private Key Guarantee
* Private keys are generated directly inside the secure hardware boundary using `KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_EC, "AndroidKeyStore")`.
* The private key material never enters the Android OS user space or application memory. 
* Any programmatic attempt to extract raw key bytes (e.g., `privateKey.encoded`) returns `null`, mathematically guaranteeing that the private key cannot be exported, saved, or uploaded to the backend.
* Only the **public key** (in standard X.509 `SubjectPublicKeyInfo` DER format, Base64-encoded) is transmitted to the Miranova backend during device enrollment.

### 2.3 Canonical Challenge Signature Contract
To prevent challenge tampering, semantic mismatch, and cross-protocol replay attacks, the client signs a strict canonical message format:
```text
miranova:auth:v1:<challenge_nonce>:<timestamp>:<service_origin>
```
* **Encoding:** Deterministic UTF-8 bytes.
* **Validation:** Both the mobile app and backend verify this exact canonical byte representation before signature calculation/verification.

### 2.4 User Authentication & Biometric Presence
* Where configured, key use is gated by local user authentication using `KeyGenParameterSpec.Builder.setUserAuthenticationRequired(true)`.
* Authentication validity is scoped per operation or to a 30-second time window via Android `BiometricPrompt`.

### 2.5 Security Level Probing without False Claims
* The implementation probes `KeyInfo.isInsideSecureHardware()` and `KeyInfo.getSecurityLevel()`:
  * `SecurityLevel.STRONGBOX`: Dedicated tamper-resistant hardware (titanium chip / Secure Element).
  * `SecurityLevel.TRUSTED_ENVIRONMENT`: Hardware-isolated processor (TEE).
  * `SecurityLevel.SOFTWARE`: Software emulation (permitted only in emulators / development).
* The app logs and reports the exact hardware isolation level accurately to the backend without making false claims of hardware isolation when running on emulator environments.

### 2.6 Lifecycle & Invalidation Handling
* When a user changes or removes their lock screen credentials, Android invalidates hardware keys bound to biometric state, throwing `KeyPermanentlyInvalidatedException`.
* The client traps this exception and routes the user into an accessible re-enrollment workflow rather than crashing or stranding the user.

---

## 3. Consequences

### Positive
* **High Security (NIST AAL2 Alignment):** Private keys cannot be stolen via software root exploits, filesystem scans, or backup extractions.
* **Replay & Tampering Resistance:** Server nonces signed with canonical payloads defeat MITM tampering and challenge replay.
* **Zero Secret Disclosure:** Sighted bystanders and acoustic eavesdroppers cannot intercept secrets because authentication relies on cryptographic proof of possession.
* **Low Network Overhead:** Compact ECDSA public keys and signatures reduce network latency.

### Negative / Trade-offs
* **Key Lifecycle Sensitivity:** System lock-screen changes invalidate keys, requiring clean re-enrollment flows.
* **Hardware Fragmentation:** Older low-end devices without StrongBox fall back to TEE, requiring graceful detection.

---

## 4. Alternatives Considered

* **Alternative 1: Software-Generated RSA 2048-bit Key in Encrypted SharedPreferences:**
  * *Rejected:* Vulnerable to memory dumping, rooting tools (Frida/Xposed), and requires master key management that is itself vulnerable.
* **Alternative 2: Symmetric Shared Secret (HMAC-SHA256 / TOTP):**
  * *Rejected:* Requires the server to store the shared symmetric secret. Server database compromise compromises all authenticators. Asymmetric public-key cryptography ensures server compromise does not yield signing keys.

---

## 5. Traceability & References
* Android Keystore System: [https://developer.android.com/privacy-and-security/keystore](https://developer.android.com/privacy-and-security/keystore)
* KeyGenParameterSpec: [https://developer.android.com/reference/android/security/keystore/KeyGenParameterSpec](https://developer.android.com/reference/android/security/keystore/KeyGenParameterSpec)
* NIST SP 800-63B-4: Digital Identity Guidelines: [https://pages.nist.gov/800-63-4/sp800-63b.html](https://pages.nist.gov/800-63-4/sp800-63b.html)
* Issue #50 (Implement device-bound key generation and challenge signing with Android Keystore)
