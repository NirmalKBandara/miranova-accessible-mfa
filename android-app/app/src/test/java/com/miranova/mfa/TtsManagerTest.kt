package com.miranova.mfa

import com.miranova.mfa.ui.accessibility.TextToSpeechEngine
import com.miranova.mfa.ui.accessibility.TtsManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests validating TtsManager secrecy guard protection, token scrubbing,
 * TalkBack non-interference, and mock speech dispatch.
 */
class TtsManagerTest {

    private class MockTtsEngine : TextToSpeechEngine {
        val spokenUtterances = mutableListOf<String>()
        var isEngineReady = true

        override fun speak(text: String, queueMode: Int): Boolean {
            if (!isEngineReady) return false
            spokenUtterances.add(text)
            return true
        }

        override fun stop() {
            spokenUtterances.clear()
        }

        override fun shutdown() {
            isEngineReady = false
        }

        override fun isReady(): Boolean = isEngineReady
    }

    private lateinit var mockEngine: MockTtsEngine
    private lateinit var ttsManager: TtsManager

    @Before
    fun setUp() {
        mockEngine = MockTtsEngine()
        ttsManager = TtsManager(externalEngine = mockEngine)
    }

    @Test
    fun `containsSecret accurately flags sensitive cryptographic material`() {
        // Hex nonces
        assertTrue(ttsManager.containsSecret("Challenge token 0123456789abcdef0123456789abcdef"))

        // Base64 secret key representations
        assertTrue(ttsManager.containsSecret("Key material MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCg=="))

        // Canonical challenge messages
        assertTrue(ttsManager.containsSecret("Payload miranova:auth:v1:nonce1234567890123456:1710000000:https://auth.miranova.io:approve"))

        // Numeric OTP codes
        assertTrue(ttsManager.containsSecret("Your confirmation PIN is 123456"))

        // Explicit secret keywords
        assertTrue(ttsManager.containsSecret("Exporting private key to buffer"))
        assertTrue(ttsManager.containsSecret("Displaying secret key"))

        // Safe operational guidance
        assertFalse(ttsManager.containsSecret("Double tap Approve to confirm this authentication challenge."))
        assertFalse(ttsManager.containsSecret("Device enrollment complete. Registered key bound to alice."))
        assertFalse(ttsManager.containsSecret("Step 1 of 3: Account Identity"))
    }

    @Test
    fun `sanitizeUtterance scrubs nonces and secrets replacing with protected tags`() {
        val raw = "Challenge miranova:auth:v1:abcdef1234567890abcdef1234567890:1710000000:example.com:auth"
        val sanitized = ttsManager.sanitizeUtterance(raw)

        assertFalse(sanitized.contains("miranova:auth:v1"))
        assertTrue(sanitized.contains("[protected challenge]"))
    }

    @Test
    fun `speak automatically scrubs secret and speaks safe version`() {
        val secretMessage = "Your authorization code is 654321 for verification"
        val success = ttsManager.speak(secretMessage, bypassMuteCheck = true)

        assertTrue(success)
        assertEquals(1, mockEngine.spokenUtterances.size)
        val spoken = mockEngine.spokenUtterances[0]
        assertFalse("Spoken audio must not contain raw numeric OTP code", spoken.contains("654321"))
        assertTrue(spoken.contains("[protected code]"))
    }

    @Test
    fun `speak allows clean non-secret guidance without modification`() {
        val cleanMessage = "Authentication request from partner portal. Double tap approve to sign."
        val success = ttsManager.speak(cleanMessage, bypassMuteCheck = true)

        assertTrue(success)
        assertEquals(listOf(cleanMessage), mockEngine.spokenUtterances)
    }

    @Test
    fun `graceful degradation when speech engine is unready`() {
        mockEngine.isEngineReady = false

        val success = ttsManager.speak("Welcome to Miranova", bypassMuteCheck = true)
        assertFalse("Speech must fail gracefully when engine is unready", success)
        assertTrue(mockEngine.spokenUtterances.isEmpty())
    }

    @Test
    fun `stop and shutdown release resources properly`() {
        ttsManager.speak("First utterance", bypassMuteCheck = true)
        assertEquals(1, mockEngine.spokenUtterances.size)

        ttsManager.stop()
        assertTrue(mockEngine.spokenUtterances.isEmpty())

        ttsManager.shutdown()
        assertFalse(ttsManager.isReady())
    }
}
