package com.miranova.mfa

import com.miranova.mfa.data.config.AppConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AppConfigTest {

    @Before
    fun setUp() {
        AppConfig.baseUrl = AppConfig.DEFAULT_EMULATOR_BASE_URL
    }

    @Test
    fun testDefaultEmulatorUrl() {
        assertEquals("http://10.0.2.2:8000", AppConfig.baseUrl)
        assertEquals("http://10.0.2.2:8000/api/health", AppConfig.getHealthUrl())
        assertEquals("http://10.0.2.2:8000/api/auth/register", AppConfig.getRegisterUrl())
        assertEquals("http://10.0.2.2:8000/api/auth/challenge", AppConfig.getChallengeUrl())
        assertEquals("http://10.0.2.2:8000/api/auth/verify", AppConfig.getVerifyUrl())
        assertEquals("http://10.0.2.2:8000/api/auth/logout", AppConfig.getLogoutUrl())
    }

    @Test
    fun testUpdateBaseUrl() {
        AppConfig.updateBaseUrl("http://192.168.1.50:8000")
        assertEquals("http://192.168.1.50:8000", AppConfig.baseUrl)
        assertEquals("http://192.168.1.50:8000/api/health", AppConfig.getHealthUrl())
    }

    @Test
    fun testUpdateBaseUrlIgnoresInvalidScheme() {
        val original = AppConfig.baseUrl
        AppConfig.updateBaseUrl("ftp://invalid.host")
        assertEquals(original, AppConfig.baseUrl)
    }

    @Test
    fun testTimeoutConstantsAreSane() {
        assertTrue(AppConfig.CONNECT_TIMEOUT_SECONDS >= 5)
        assertTrue(AppConfig.READ_TIMEOUT_SECONDS >= 10)
    }
}
