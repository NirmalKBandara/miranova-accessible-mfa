package com.miranova.mfa.crypto

/**
 * Model describing a generated Android Keystore key pair and its security properties.
 */
data class KeyInfoModel(
    val alias: String,
    val algorithm: String = "EC",
    val keySize: Int = 256,
    val securityLevel: SecurityLevel = SecurityLevel.UNKNOWN,
    val isInsideSecureHardware: Boolean = false,
    val userAuthenticationRequired: Boolean = false,
    val publicKeyBase64: String = ""
)
