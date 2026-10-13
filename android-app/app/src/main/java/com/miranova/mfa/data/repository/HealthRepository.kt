package com.miranova.mfa.data.repository

import com.miranova.mfa.data.config.AppConfig
import com.miranova.mfa.data.network.NetworkClient
import com.miranova.mfa.data.network.NetworkResult
import com.miranova.mfa.data.network.model.HealthResponse
import org.json.JSONObject

/**
 * Repository to verify backend connectivity and health endpoint.
 */
class HealthRepository(
    private val client: NetworkClient = NetworkClient
) {
    suspend fun checkHealth(): NetworkResult<HealthResponse> {
        val url = AppConfig.getHealthUrl()
        return when (val result = client.get(url)) {
            is NetworkResult.Success -> {
                try {
                    val json = JSONObject(result.data)
                    val status = json.optString("status", "ok")
                    val service = json.optString("service", "miranova-backend")
                    val timestamp = json.optString("timestamp", "")
                    val version = json.optString("version", "1.0.0")
                    NetworkResult.Success(HealthResponse(status, service, timestamp, version))
                } catch (e: Exception) {
                    // Fallback if plain text or unexpected format
                    NetworkResult.Success(HealthResponse(status = "ok", service = "miranova-backend"))
                }
            }
            is NetworkResult.HttpError -> result
            is NetworkResult.NetworkError -> result
            is NetworkResult.Timeout -> result
        }
    }
}
