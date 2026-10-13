package com.miranova.mfa.crypto

import java.security.PublicKey

/**
 * Interface contract for device-bound hardware keystore management.
 */
interface KeyStoreManager {

    /**
     * Generates an asymmetric non-exportable ECDSA P-256 key pair in Android Keystore.
     */
    @Throws(CryptoException::class)
    fun generateKeyPair(alias: String, requireUserAuth: Boolean = false): KeyInfoModel

    /**
     * Retrieves the public key for the specified alias.
     */
    fun getPublicKey(alias: String): PublicKey?

    /**
     * Retrieves the standard X.509 Base64-encoded public key for server registration.
     */
    fun getPublicKeyBase64(alias: String): String?

    /**
     * Signs arbitrary data with the private key anchored in hardware.
     */
    @Throws(CryptoException::class)
    fun sign(alias: String, data: ByteArray): ByteArray

    /**
     * Verifies a signature against the public key.
     */
    fun verify(alias: String, data: ByteArray, signature: ByteArray): Boolean

    /**
     * Checks if a key pair exists under the alias.
     */
    fun hasKey(alias: String): Boolean

    /**
     * Deletes the key pair from Keystore.
     */
    fun deleteKey(alias: String): Boolean

    /**
     * Retrieves key information and security isolation status.
     */
    fun getKeyInfo(alias: String): KeyInfoModel?
}
