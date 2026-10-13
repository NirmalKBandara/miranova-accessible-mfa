package com.miranova.mfa

import com.miranova.mfa.crypto.DeviceRegistrationPayload
import com.miranova.mfa.crypto.KeyInfoModel
import com.miranova.mfa.crypto.SecurityLevel
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DeviceRegistrationPayloadTest {

    @Test
    fun testPayloadSerializationDoesNotContainPrivateKey() {
        val keyInfo = KeyInfoModel(
            alias = "miranova_user_key",
            algorithm = "EC",
            keySize = 256,
            securityLevel = SecurityLevel.TRUSTED_ENVIRONMENT,
            isInsideSecureHardware = true,
            userAuthenticationRequired = true,
            publicKeyBase64 = "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAE1234567890abcdef"
        )

        val payload = DeviceRegistrationPayload.fromKeyInfo("suren_user", keyInfo)
        val jsonStr = payload.toJsonString()

        // Verify JSON keys
        val json = JSONObject(jsonStr)
        assertEquals("suren_user", json.getString("username"))
        assertEquals("miranova_user_key", json.getString("device_alias"))
        assertEquals("ES256", json.getString("algorithm"))
        assertEquals("MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAE1234567890abcdef", json.getString("public_key"))
        assertEquals("TRUSTED_ENVIRONMENT", json.getString("security_level"))
        assertTrue(json.getBoolean("hardware_backed"))

        // Critical Security Assertion: No private key material in JSON output
        assertFalse("JSON must not contain private_key", json.has("private_key"))
        assertFalse("JSON must not contain secret", json.has("secret"))
        assertFalse("JSON must not contain seed", json.has("seed"))
    }
}
