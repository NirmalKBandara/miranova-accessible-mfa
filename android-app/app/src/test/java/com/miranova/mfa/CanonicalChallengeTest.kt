package com.miranova.mfa

import com.miranova.mfa.crypto.CanonicalChallenge
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.charset.StandardCharsets

class CanonicalChallengeTest {

    @Test
    fun testCanonicalStringFormat() {
        val challenge = CanonicalChallenge(
            nonce = "random-nonce-12345",
            timestamp = 1700000000000L,
            origin = "https://mfa.miranova.internal",
            action = "authenticate"
        )

        val expected = "miranova:auth:v1:random-nonce-12345:1700000000000:https://mfa.miranova.internal:authenticate"
        assertEquals(expected, challenge.toCanonicalString())
    }

    @Test
    fun testCanonicalBytesMatchesUtf8() {
        val challenge = CanonicalChallenge.create(
            nonce = "test-nonce",
            timestamp = 1600000000L,
            origin = "https://example.com"
        )

        val expectedBytes = challenge.toCanonicalString().toByteArray(StandardCharsets.UTF_8)
        assertTrue(expectedBytes.contentEquals(challenge.toCanonicalBytes()))
    }

    @Test
    fun testTamperingAltersCanonicalBytes() {
        val original = CanonicalChallenge.create("nonce-abc", 1000L)
        val tamperedNonce = CanonicalChallenge.create("nonce-xyz", 1000L)
        val tamperedTimestamp = CanonicalChallenge.create("nonce-abc", 1001L)

        assertFalse(original.toCanonicalBytes().contentEquals(tamperedNonce.toCanonicalBytes()))
        assertFalse(original.toCanonicalBytes().contentEquals(tamperedTimestamp.toCanonicalBytes()))
    }
}
