package com.miranova.mfa.data.network

/**
 * Standard sealed result interface for network operations.
 */
sealed interface NetworkResult<out T> {
    data class Success<T>(val data: T) : NetworkResult<T>
    data class HttpError(val code: Int, val message: String) : NetworkResult<Nothing>
    data class NetworkError(val exception: Throwable) : NetworkResult<Nothing>
    data object Timeout : NetworkResult<Nothing>
}
