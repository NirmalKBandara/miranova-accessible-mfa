package com.miranova.mfa

import com.miranova.mfa.crypto.CryptoException
import com.miranova.mfa.crypto.KeyInfoModel
import com.miranova.mfa.crypto.KeyLifecycleManager
import com.miranova.mfa.crypto.KeyStatus
import com.miranova.mfa.crypto.KeyStoreManager
import com.miranova.mfa.crypto.SecurityLevel
import com.miranova.mfa.data.model.EnrollmentState
import com.miranova.mfa.data.network.NetworkCaller
import com.miranova.mfa.data.network.NetworkResult
import com.miranova.mfa.data.repository.EnrollmentRepository
import com.miranova.mfa.data.store.MemoryEnrollmentStore
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.PublicKey
import java.util.Base64

/**
 * Unit tests verifying EnrollmentRepository atomic registration, Keystore key generation,
 * server failure rollback (anti-orphaned-key guarantee), and re-enrollment lifecycle.
 */
class EnrollmentRepositoryTest {

    private class MockKeyStoreManager : KeyStoreManager {
        val keys = mutableMapOf<String, KeyPair>()
        var keyInvalidated = false

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
            if (keyInvalidated) {
                throw CryptoException.KeyPermanentlyInvalidatedException("Key invalidated by OS")
            }
            if (!keys.containsKey(alias)) throw CryptoException.KeyNotFoundException(alias)
            return ByteArray(64) { 0x01 }
        }

        override fun verify(alias: String, data: ByteArray, signature: ByteArray): Boolean = keys.containsKey(alias)

        override fun hasKey(alias: String): Boolean = keys.containsKey(alias)

        override fun deleteKey(alias: String): Boolean = keys.remove(alias) != null

        override fun getKeyInfo(alias: String): KeyInfoModel? {
            val pub = getPublicKeyBase64(alias) ?: return null
            return KeyInfoModel(
                alias = alias,
                algorithm = "EC",
                keySize = 256,
                securityLevel = SecurityLevel.SOFTWARE,
                isInsideSecureHardware = false,
                publicKeyBase64 = pub
            )
        }
    }

    private class MockNetworkCaller : NetworkCaller {
        var postHandler: (suspend (url: String, json: String) -> NetworkResult<String>)? = null

        override suspend fun get(url: String): NetworkResult<String> = NetworkResult.Success("{}")

        override suspend fun post(url: String, jsonBody: String): NetworkResult<String> {
            return postHandler?.invoke(url, jsonBody)
                ?: NetworkResult.Success("""{"userId":"user_alice_123","deviceId":"dev_android_99","registeredAt":"2026-10-13T10:00:00Z"}""")
        }
    }

    private lateinit var mockKeyStore: MockKeyStoreManager
    private lateinit var mockNetwork: MockNetworkCaller
    private lateinit var store: MemoryEnrollmentStore
    private lateinit var lifecycle: KeyLifecycleManager
    private lateinit var repository: EnrollmentRepository

    @Before
    fun setUp() {
        mockKeyStore = MockKeyStoreManager()
        mockNetwork = MockNetworkCaller()
        store = MemoryEnrollmentStore()
        lifecycle = KeyLifecycleManager(mockKeyStore)
        repository = EnrollmentRepository(
            keyStoreManager = mockKeyStore,
            networkClient = mockNetwork,
            enrollmentStore = store,
            lifecycleManager = lifecycle
        )
    }

    @Test
    fun `initial state is Unenrolled when store is empty`() {
        assertTrue(repository.enrollmentState.value is EnrollmentState.Unenrolled)
        assertFalse(repository.isEnrolled())
    }

    @Test
    fun `successful registration creates key, persists record, and transitions to Enrolled`() = runBlocking {
        mockNetwork.postHandler = { _, _ ->
            NetworkResult.Success("""{"userId":"usr_alice_42","deviceId":"dev_pixel_7","registeredAt":"2026-10-13T12:00:00Z"}""")
        }

        val result = repository.registerDevice(
            username = "alice",
            requireBiometric = true,
            keyAlias = "test_alias"
        )

        assertTrue(result is EnrollmentState.Enrolled)
        val enrolled = result as EnrollmentState.Enrolled
        assertEquals("alice", enrolled.username)
        assertEquals("usr_alice_42", enrolled.userId)
        assertEquals("dev_pixel_7", enrolled.deviceId)
        assertEquals("test_alias", enrolled.keyAlias)
        assertTrue(enrolled.isBiometricProtected)

        // Hardware key exists in keystore
        assertTrue(mockKeyStore.hasKey("test_alias"))

        // Store contains record
        assertTrue(store.isEnrolled())
        assertEquals("alice", store.getEnrollment()?.username)
        assertTrue(repository.isEnrolled())
    }

    @Test
    fun `registration failure triggers atomic rollback and deletes orphaned keystore key`() = runBlocking {
        mockNetwork.postHandler = { _, _ ->
            NetworkResult.HttpError(400, "Device registration rejected by backend")
        }

        val result = repository.registerDevice(
            username = "bob",
            keyAlias = "bob_key"
        )

        assertTrue(result is EnrollmentState.EnrollmentFailed)
        val failed = result as EnrollmentState.EnrollmentFailed
        assertTrue(failed.isRollbackCompleted)

        // Consistency check: Keystore key was rolled back (deleted)
        assertFalse("Orphaned key must be deleted upon registration failure", mockKeyStore.hasKey("bob_key"))
        assertFalse("Store must remain unenrolled", store.isEnrolled())
        assertFalse(repository.isEnrolled())
    }

    @Test
    fun `network timeout triggers atomic rollback`() = runBlocking {
        mockNetwork.postHandler = { _, _ ->
            NetworkResult.Timeout
        }

        val result = repository.registerDevice(username = "charlie", keyAlias = "charlie_key")

        assertTrue(result is EnrollmentState.EnrollmentFailed)
        assertFalse(mockKeyStore.hasKey("charlie_key"))
        assertFalse(repository.isEnrolled())
    }

    @Test
    fun `cancelEnrollment deletes in-flight key and reverts to Unenrolled`() {
        mockKeyStore.generateKeyPair("cancelled_key")
        assertTrue(mockKeyStore.hasKey("cancelled_key"))

        repository.cancelEnrollment("cancelled_key")

        assertFalse(mockKeyStore.hasKey("cancelled_key"))
        assertTrue(repository.enrollmentState.value is EnrollmentState.Unenrolled)
    }

    @Test
    fun `checkEnrollmentHealth detects invalidated key and transitions to ReEnrollmentRequired`() {
        mockKeyStore.generateKeyPair("health_key")
        mockKeyStore.keyInvalidated = true

        val status = repository.checkEnrollmentHealth("health_key")

        assertTrue(status is KeyStatus.Invalidated)
        assertTrue(repository.enrollmentState.value is EnrollmentState.ReEnrollmentRequired)
        val req = repository.enrollmentState.value as EnrollmentState.ReEnrollmentRequired
        assertEquals("health_key", req.previousKeyAlias)
    }

    @Test
    fun `reEnrollDevice revokes old key material and completes fresh enrollment`() = runBlocking {
        // Setup initial enrollment
        mockKeyStore.generateKeyPair("my_key")
        val initialResult = repository.registerDevice("david", false, "my_key")
        assertTrue(initialResult is EnrollmentState.Enrolled)

        // Now execute re-enrollment with new policy
        mockNetwork.postHandler = { _, _ ->
            NetworkResult.Success("""{"userId":"usr_david_new","deviceId":"dev_david_new","registeredAt":"2026-10-13T14:00:00Z"}""")
        }

        val reEnrollResult = repository.reEnrollDevice("david", requireBiometric = true, keyAlias = "my_key")

        assertTrue(reEnrollResult is EnrollmentState.Enrolled)
        val enrolled = reEnrollResult as EnrollmentState.Enrolled
        assertEquals("usr_david_new", enrolled.userId)
        assertTrue(enrolled.isBiometricProtected)
        assertTrue(mockKeyStore.hasKey("my_key"))
    }

    @Test
    fun `revokeCurrentEnrollment cleans up key and resets repository to Unenrolled`() {
        mockKeyStore.generateKeyPair("revoked_key")
        store.saveEnrollment(
            com.miranova.mfa.data.store.EnrollmentRecord(
                userId = "u1",
                username = "eve",
                deviceId = "d1",
                registeredAt = "2026-10-13",
                keyAlias = "revoked_key",
                isBiometricProtected = false
            )
        )
        assertTrue(store.isEnrolled())

        repository.revokeCurrentEnrollment("revoked_key")

        assertFalse(mockKeyStore.hasKey("revoked_key"))
        assertFalse(store.isEnrolled())
        assertTrue(repository.enrollmentState.value is EnrollmentState.Unenrolled)
    }
}
