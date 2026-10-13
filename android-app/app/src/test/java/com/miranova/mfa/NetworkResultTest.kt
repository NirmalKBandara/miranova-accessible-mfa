package com.miranova.mfa

import com.miranova.mfa.data.network.NetworkResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class NetworkResultTest {

    @Test
    fun testSuccessResult() {
        val result: NetworkResult<String> = NetworkResult.Success("OK")
        assertTrue(result is NetworkResult.Success)
        assertEquals("OK", (result as NetworkResult.Success).data)
    }

    @Test
    fun testHttpErrorResult() {
        val result: NetworkResult<String> = NetworkResult.HttpError(401, "Unauthorized challenge signature")
        assertTrue(result is NetworkResult.HttpError)
        val httpError = result as NetworkResult.HttpError
        assertEquals(401, httpError.code)
        assertEquals("Unauthorized challenge signature", httpError.message)
    }

    @Test
    fun testNetworkErrorResult() {
        val exception = IOException("Connection refused by peer")
        val result: NetworkResult<String> = NetworkResult.NetworkError(exception)
        assertTrue(result is NetworkResult.NetworkError)
        assertEquals("Connection refused by peer", (result as NetworkResult.NetworkError).exception.message)
    }

    @Test
    fun testTimeoutResult() {
        val result: NetworkResult<String> = NetworkResult.Timeout
        assertTrue(result is NetworkResult.Timeout)
    }
}
