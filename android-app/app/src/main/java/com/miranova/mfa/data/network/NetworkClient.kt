package com.miranova.mfa.data.network

import com.miranova.mfa.data.config.AppConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.net.SocketTimeoutException
import java.util.concurrent.TimeUnit

/**
 * Abstract network caller interface for testability and mock injection.
 */
interface NetworkCaller {
    suspend fun get(url: String): NetworkResult<String>
    suspend fun post(url: String, jsonBody: String): NetworkResult<String>
}

/**
 * Robust OkHttp network client for backend API communication.
 */
object NetworkClient : NetworkCaller {

    private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

    val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(AppConfig.CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .readTimeout(AppConfig.READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .writeTimeout(AppConfig.WRITE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    override suspend fun get(url: String): NetworkResult<String> = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(url)
            .get()
            .addHeader("Accept", "application/json")
            .build()

        executeRequest(request)
    }

    override suspend fun post(url: String, jsonBody: String): NetworkResult<String> = withContext(Dispatchers.IO) {
        val body: RequestBody = jsonBody.toRequestBody(JSON_MEDIA_TYPE)
        val request = Request.Builder()
            .url(url)
            .post(body)
            .addHeader("Accept", "application/json")
            .addHeader("Content-Type", "application/json")
            .build()

        executeRequest(request)
    }

    private fun executeRequest(request: Request): NetworkResult<String> {
        return try {
            val response = okHttpClient.newCall(request).execute()
            response.use { resp ->
                val bodyString = resp.body?.string().orEmpty()
                if (resp.isSuccessful) {
                    NetworkResult.Success(bodyString)
                } else {
                    NetworkResult.HttpError(resp.code, bodyString.ifBlank { resp.message })
                }
            }
        } catch (e: SocketTimeoutException) {
            NetworkResult.Timeout
        } catch (e: IOException) {
            NetworkResult.NetworkError(e)
        } catch (e: Exception) {
            NetworkResult.NetworkError(e)
        }
    }
}
