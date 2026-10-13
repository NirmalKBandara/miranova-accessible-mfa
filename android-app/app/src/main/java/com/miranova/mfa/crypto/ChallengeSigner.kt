package com.miranova.mfa.crypto


/**
 * Signed challenge response payload matching backend /api/auth/verify expectation.
 */
data class SignedChallengeResponse(
    val alias: String,
    val challengeNonce: String,
    val signatureBase64: String,
    val canonicalMessage: String,
    val publicKeyBase64: String,
    val timestamp: Long
)

/**
 * Helper component that coordinates canonical serialization and Keystore signing.
 */
class ChallengeSigner(
    private val keyStoreManager: KeyStoreManager
) {

    /**
     * Signs a canonical challenge with the device-bound hardware key.
     */
    @Throws(CryptoException::class)
    fun signChallenge(alias: String, challenge: CanonicalChallenge): SignedChallengeResponse {
        val canonicalBytes = challenge.toCanonicalBytes()
        val signatureBytes = keyStoreManager.sign(alias, canonicalBytes)
        val signatureBase64 = Base64Util.encodeToString(signatureBytes)
        val publicKeyBase64 = keyStoreManager.getPublicKeyBase64(alias).orEmpty()

        return SignedChallengeResponse(
            alias = alias,
            challengeNonce = challenge.nonce,
            signatureBase64 = signatureBase64,
            canonicalMessage = challenge.toCanonicalString(),
            publicKeyBase64 = publicKeyBase64,
            timestamp = challenge.timestamp
        )
    }

    /**
     * Verifies that a signed challenge response matches the canonical challenge and registered public key.
     */
    fun verifyChallenge(alias: String, challenge: CanonicalChallenge, signatureBase64: String): Boolean {
        return try {
            val signatureBytes = Base64Util.decode(signatureBase64)
            val canonicalBytes = challenge.toCanonicalBytes()
            keyStoreManager.verify(alias, canonicalBytes, signatureBytes)
        } catch (e: Exception) {
            false
        }
    }
}
