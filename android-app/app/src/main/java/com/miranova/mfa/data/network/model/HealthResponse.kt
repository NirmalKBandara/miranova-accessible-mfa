package com.miranova.mfa.data.network.model

/**
 * Health check response model matching FastAPI backend /api/health endpoint.
 */
data class HealthResponse(
    val status: String,
    val service: String = "miranova-backend",
    val timestamp: String = "",
    val version: String = "1.0.0"
)
