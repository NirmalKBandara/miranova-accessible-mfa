package com.miranova.mfa.crypto

import java.nio.charset.StandardCharsets

/**
 * Deterministic canonical authentication challenge message contract.
 * Guarantees zero byte mismatch between Android signing client and FastAPI backend verifier.
 */
data class CanonicalChallenge(
    val nonce: String,
    val timestamp: Long,
    val origin: String = "https://mfa.miranova.internal",
    val action: String = "authenticate"
) {
    companion object {
        const val PROTOCOL_PREFIX = "miranova:auth:v1"

        fun create(nonce: String, timestamp: Long = System.currentTimeMillis(), origin: String = "https://mfa.miranova.internal"): CanonicalChallenge {
            return CanonicalChallenge(
                nonce = nonce.trim(),
                timestamp = timestamp,
                origin = origin.trim(),
                action = "authenticate"
            )
        }
    }

    /**
     * Serializes to the exact canonical UTF-8 byte representation required for ECDSA signing.
     */
    fun toCanonicalBytes(): ByteArray {
        return toCanonicalString().toByteArray(StandardCharsets.UTF_8)
    }

    /**
     * Serializes to standard colon-delimited canonical string.
     */
    fun toCanonicalString(): String {
        return "$PROTOCOL_PREFIX:$nonce:$timestamp:$origin:$action"
    }
}
