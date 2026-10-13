package com.miranova.mfa.crypto

import java.util.Base64

/**
 * Platform-independent Base64 encoding/decoding utility using standard Java 8+ APIs (Android API 26+).
 * Eliminates android.util.Base64 stubbing issues in local JVM unit test runners.
 */
object Base64Util {
    fun encodeToString(bytes: ByteArray): String {
        return Base64.getEncoder().encodeToString(bytes)
    }

    fun decode(base64String: String): ByteArray {
        return Base64.getDecoder().decode(base64String.trim())
    }
}
