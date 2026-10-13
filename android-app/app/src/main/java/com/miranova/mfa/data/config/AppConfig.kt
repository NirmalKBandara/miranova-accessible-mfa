package com.miranova.mfa.data.config

/**
 * Global application network and environment configuration.
 * Configured by default for Android emulator host loopback (10.0.2.2:8000).
 */
object AppConfig {
    // Default base URL for Android emulator communicating with local FastAPI backend
    const val DEFAULT_EMULATOR_BASE_URL = "http://10.0.2.2:8000"
    const val DEFAULT_DEVICE_BASE_URL = "http://192.168.1.100:8000"

    @Volatile
    var baseUrl: String = DEFAULT_EMULATOR_BASE_URL

    // Standardized API Endpoint paths
    const val ENDPOINT_HEALTH = "/api/health"
    const val ENDPOINT_REGISTER_DEVICE = "/api/auth/register"
    const val ENDPOINT_GET_CHALLENGE = "/api/auth/challenge"
    const val ENDPOINT_VERIFY_CHALLENGE = "/api/auth/verify"
    const val ENDPOINT_REVOKE_SESSION = "/api/auth/logout"

    // Timeouts
    const val CONNECT_TIMEOUT_SECONDS = 10L
    const val READ_TIMEOUT_SECONDS = 15L
    const val WRITE_TIMEOUT_SECONDS = 15L

    fun getHealthUrl(): String = "${baseUrl.trimEnd('/')}$ENDPOINT_HEALTH"
    fun getRegisterUrl(): String = "${baseUrl.trimEnd('/')}$ENDPOINT_REGISTER_DEVICE"
    fun getChallengeUrl(): String = "${baseUrl.trimEnd('/')}$ENDPOINT_GET_CHALLENGE"
    fun getVerifyUrl(): String = "${baseUrl.trimEnd('/')}$ENDPOINT_VERIFY_CHALLENGE"
    fun getLogoutUrl(): String = "${baseUrl.trimEnd('/')}$ENDPOINT_REVOKE_SESSION"

    fun updateBaseUrl(newUrl: String) {
        val trimmed = newUrl.trim()
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            baseUrl = trimmed
        }
    }
}
