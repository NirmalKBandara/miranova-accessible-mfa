package com.miranova.mfa

import com.miranova.mfa.crypto.CanonicalChallenge
import com.miranova.mfa.crypto.ChallengeSigner
import com.miranova.mfa.crypto.CryptoException
import com.miranova.mfa.crypto.KeyInfoModel
import com.miranova.mfa.crypto.KeyLifecycleManager
import com.miranova.mfa.crypto.KeyStatus
import com.miranova.mfa.crypto.KeyStoreManager
import com.miranova.mfa.crypto.SecurityLevel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.PublicKey
import java.security.Signature
import java.util.Base64

/**
 * Unit test validating KeyStoreManager contract, ChallengeSigner, and KeyLifecycleManager.
 */
class KeyStoreManagerTest {

    private class MockJvmKeyStoreManager : KeyStoreManager {
        val keys = mutableMapOf<String, KeyPair>()
        var shouldThrowInvalidated = false

        override fun generateKeyPair(alias: String, requireUserAuth: Boolean): KeyInfoModel {
            val kpg = KeyPairGenerator.getInstance("EC")
            kpg.initialize(256)
            val kp = kpg.generateKeyPair()
            keys[alias] = kp

            val pubBase64 = Base64.getEncoder().encodeToString(kp.public.encoded)
            return KeyInfoModel(
                alias = alias,
                algorithm = "EC",
                keySize = 256,
                securityLevel = SecurityLevel.SOFTWARE,
                isInsideSecureHardware = false,
                userAuthenticationRequired = requireUserAuth,
                publicKeyBase64 = pubBase64
            )
        }

        override fun getPublicKey(alias: String): PublicKey? = keys[alias]?.public

        override fun getPublicKeyBase64(alias: String): String? {
            val pub = getPublicKey(alias) ?: return null
            return Base64.getEncoder().encodeToString(pub.encoded)
        }

        override fun sign(alias: String, data: ByteArray): ByteArray {
            if (shouldThrowInvalidated) {
                throw CryptoException.KeyPermanentlyInvalidatedException("Mock key invalidated by lock screen")
            }
            val kp = keys[alias] ?: throw CryptoException.KeyNotFoundException(alias)
            val sig = Signature.getInstance("SHA256withECDSA")
            sig.initSign(kp.private)
            sig.update(data)
            return sig.sign()
        }

        override fun verify(alias: String, data: ByteArray, signature: ByteArray): Boolean {
            val pub = getPublicKey(alias) ?: return false
            return try {
                val sig = Signature.getInstance("SHA256withECDSA")
                sig.initVerify(pub)
                sig.update(data)
                sig.verify(signature)
            } catch (e: Exception) {
                false
            }
        }

        override fun hasKey(alias: String): Boolean = keys.containsKey(alias)

        override fun deleteKey(alias: String): Boolean = keys.remove(alias) != null

        override fun getKeyInfo(alias: String): KeyInfoModel? {
            val pubBase64 = getPublicKeyBase64(alias) ?: return null
            return KeyInfoModel(
                alias = alias,
                algorithm = "EC",
                keySize = 256,
                securityLevel = SecurityLevel.SOFTWARE,
                isInsideSecureHardware = false,
                publicKeyBase64 = pubBase64
            )
        }
    }

    private lateinit var mockManager: MockJvmKeyStoreManager
    private lateinit var signer: ChallengeSigner
    private lateinit var lifecycle: KeyLifecycleManager

    @Before
    fun setUp() {
        mockManager = MockJvmKeyStoreManager()
        signer = ChallengeSigner(mockManager)
        lifecycle = KeyLifecycleManager(mockManager)
    }

    @Test
    fun testGenerateAndSignChallengeSuccess() {
        val alias = "test_device_key"
        val info = mockManager.generateKeyPair(alias)

        assertEquals("test_device_key", info.alias)
        assertEquals("EC", info.algorithm)
        assertTrue(mockManager.hasKey(alias))

        // Create and sign canonical challenge
        val challenge = CanonicalChallenge.create("nonce-998877", 1700000000000L)
        val response = signer.signChallenge(alias, challenge)

        assertEquals("test_device_key", response.alias)
        assertEquals("nonce-998877", response.challengeNonce)
        assertNotNull(response.signatureBase64)
        assertTrue(response.signatureBase64.isNotBlank())

        // Verify signature with registered public key
        val isValid = signer.verifyChallenge(alias, challenge, response.signatureBase64)
        assertTrue("Signature must verify successfully against original challenge", isValid)
    }

    @Test
    fun testTamperedChallengeSignatureFails() {
        val alias = "test_tamper_key"
        mockManager.generateKeyPair(alias)

        val challenge = CanonicalChallenge.create("nonce-original", 1700000000000L)
        val response = signer.signChallenge(alias, challenge)

        // Verify with tampered challenge
        val tamperedChallenge = CanonicalChallenge.create("nonce-tampered", 1700000000000L)
        val isTamperedValid = signer.verifyChallenge(alias, tamperedChallenge, response.signatureBase64)
        assertFalse("Tampered challenge must fail verification", isTamperedValid)
    }

    @Test
    fun testKeyPermanentlyInvalidatedTriggersRecovery() {
        val alias = "test_invalidated_key"
        mockManager.generateKeyPair(alias)

        // Initially valid
        val initialStatus = lifecycle.checkKeyStatus(alias)
        assertTrue(initialStatus is KeyStatus.Valid)
        assertFalse(lifecycle.requiresReEnrollment(alias))

        // Simulate lock screen reset causing permanent invalidation
        mockManager.shouldThrowInvalidated = true

        val status = lifecycle.checkKeyStatus(alias)
        assertTrue("Status must be Invalidated", status is KeyStatus.Invalidated)
        assertTrue("Re-enrollment must be required when key is invalidated", lifecycle.requiresReEnrollment(alias))
    }

    @Test
    fun testRevokeKeyRemovesFromStore() {
        val alias = "test_revoke_key"
        mockManager.generateKeyPair(alias)
        assertTrue(mockManager.hasKey(alias))

        val revoked = lifecycle.revokeDeviceKey(alias)
        assertTrue(revoked)
        assertFalse(mockManager.hasKey(alias))
        assertEquals(KeyStatus.NotEnrolled, lifecycle.checkKeyStatus(alias))
    }
}
