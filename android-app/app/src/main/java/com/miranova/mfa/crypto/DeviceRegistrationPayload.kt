package com.miranova.mfa.crypto

import org.json.JSONObject

/**
 * Public-key registration payload submitted to the FastAPI backend during device enrollment.
 * Conforms strictly to the principle that only public key material and non-sensitive hardware
 * metadata leave the device.
 */
data class DeviceRegistrationPayload(
    val username: String,
    val deviceAlias: String,
    val algorithm: String = "ES256",
    val publicKeyBase64: String,
    val securityLevel: String,
    val isInsideSecureHardware: Boolean,
    val enrollmentTimestamp: Long = System.currentTimeMillis()
) {
    companion object {
        fun fromKeyInfo(username: String, keyInfo: KeyInfoModel): DeviceRegistrationPayload {
            return DeviceRegistrationPayload(
                username = username.trim(),
                deviceAlias = keyInfo.alias,
                algorithm = "ES256", // Standard FIDO2 / ECDSA with SHA-256
                publicKeyBase64 = keyInfo.publicKeyBase64,
                securityLevel = keyInfo.securityLevel.name,
                isInsideSecureHardware = keyInfo.isInsideSecureHardware,
                enrollmentTimestamp = System.currentTimeMillis()
            )
        }
    }

    /**
     * Serializes payload to standard JSON string for HTTP POST /api/auth/register.
     */
    fun toJsonString(): String {
        return JSONObject().apply {
            put("username", username)
            put("device_alias", deviceAlias)
            put("algorithm", algorithm)
            put("public_key", publicKeyBase64)
            put("security_level", securityLevel)
            put("hardware_backed", isInsideSecureHardware)
            put("timestamp", enrollmentTimestamp)
        }.toString()
    }
}
