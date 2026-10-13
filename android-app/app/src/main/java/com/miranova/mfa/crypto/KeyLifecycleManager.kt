package com.miranova.mfa.crypto

/**
 * High-level state representing the lifecycle health of the device key.
 */
sealed interface KeyStatus {
    data class Valid(val keyInfo: KeyInfoModel) : KeyStatus
    data class Invalidated(val reason: String) : KeyStatus
    data object NotEnrolled : KeyStatus
    data class Error(val exception: CryptoException) : KeyStatus
}

/**
 * Manages key lifecycle, revocation, and recovery from permanent invalidation.
 */
class KeyLifecycleManager(
    private val keyStoreManager: KeyStoreManager
) {

    /**
     * Probes the key pair health and checks for system lock screen invalidation.
     */
    fun checkKeyStatus(alias: String): KeyStatus {
        if (!keyStoreManager.hasKey(alias)) {
            return KeyStatus.NotEnrolled
        }

        return try {
            val info = keyStoreManager.getKeyInfo(alias)
                ?: return KeyStatus.NotEnrolled

            // Perform probe signature to detect KeyPermanentlyInvalidatedException
            val probeBytes = "miranova:health:probe".toByteArray(Charsets.UTF_8)
            keyStoreManager.sign(alias, probeBytes)

            KeyStatus.Valid(info)
        } catch (e: CryptoException.KeyPermanentlyInvalidatedException) {
            KeyStatus.Invalidated(
                "Hardware key was permanently invalidated by OS lock screen changes: ${e.message}"
            )
        } catch (e: CryptoException.UserNotAuthenticatedException) {
            // Key is valid, but requires local biometric prompt to authorize
            val info = keyStoreManager.getKeyInfo(alias)
            if (info != null) KeyStatus.Valid(info) else KeyStatus.NotEnrolled
        } catch (e: CryptoException) {
            KeyStatus.Error(e)
        } catch (e: Exception) {
            KeyStatus.Error(CryptoException.SignatureException("Unexpected failure checking key: ${e.message}", e))
        }
    }

    /**
     * Checks if re-enrollment is required due to missing or invalidated hardware key.
     */
    fun requiresReEnrollment(alias: String): Boolean {
        return when (checkKeyStatus(alias)) {
            is KeyStatus.NotEnrolled, is KeyStatus.Invalidated -> true
            else -> false
        }
    }

    /**
     * Revokes and removes the hardware key from Android Keystore.
     */
    fun revokeDeviceKey(alias: String): Boolean {
        return keyStoreManager.deleteKey(alias)
    }
}
