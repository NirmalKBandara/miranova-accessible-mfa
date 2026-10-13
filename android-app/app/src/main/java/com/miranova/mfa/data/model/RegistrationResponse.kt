package com.miranova.mfa.data.model

import org.json.JSONObject

/**
 * Backend response model for HTTP POST /api/auth/register.
 */
data class RegistrationResponse(
    val success: Boolean,
    val message: String,
    val userId: String? = null,
    val deviceId: String? = null,
    val registeredAt: String? = null
) {
    companion object {
        /**
         * Parses JSON string returned from backend registration endpoint.
         */
        fun fromJson(jsonStr: String): RegistrationResponse {
            return try {
                val obj = JSONObject(jsonStr)
                RegistrationResponse(
                    success = obj.optBoolean("success", true),
                    message = obj.optString("message", "Device registered successfully"),
                    userId = obj.optString("userId", obj.optString("user_id", "")).takeIf { it.isNotBlank() },
                    deviceId = obj.optString("deviceId", obj.optString("device_id", "")).takeIf { it.isNotBlank() },
                    registeredAt = obj.optString("registeredAt", obj.optString("registered_at", "")).takeIf { it.isNotBlank() }
                )
            } catch (e: Exception) {
                RegistrationResponse(
                    success = false,
                    message = "Malformed registration response: ${e.message}"
                )
            }
        }
    }
}
