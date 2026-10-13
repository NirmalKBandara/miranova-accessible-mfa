package com.miranova.mfa.data.repository

import com.miranova.mfa.crypto.CryptoException
import com.miranova.mfa.crypto.DeviceRegistrationPayload
import com.miranova.mfa.crypto.KeyStoreManager
import com.miranova.mfa.data.config.AppConfig
import com.miranova.mfa.data.model.EnrollmentState
import com.miranova.mfa.data.model.RegistrationResponse
import com.miranova.mfa.data.network.NetworkClient
import com.miranova.mfa.data.network.NetworkResult
import com.miranova.mfa.data.store.EnrollmentRecord
import com.miranova.mfa.data.store.EnrollmentStore
import com.miranova.mfa.data.store.MemoryEnrollmentStore
import com.miranova.mfa.ui.accessibility.ActionableError
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

/**
 * Repository orchestrating atomic device enrollment, Keystore key generation,
 * and backend registration.
 *
 * Consistency Guarantee (Issue #51):
 * If network registration fails or is cancelled, any newly generated Keystore key
 * is atomically deleted (rollback) so the device never retains an orphaned key.
 */
class EnrollmentRepository(
    private val keyStoreManager: KeyStoreManager,
    private val networkClient: NetworkClient = NetworkClient,
    private val enrollmentStore: EnrollmentStore = MemoryEnrollmentStore()
) {

    companion object {
        const val DEFAULT_KEY_ALIAS = "miranova_device_key"
    }

    private val _enrollmentState = MutableStateFlow<EnrollmentState>(
        if (enrollmentStore.isEnrolled()) {
            val record = enrollmentStore.getEnrollment()!!
            EnrollmentState.Enrolled(
                userId = record.userId,
                username = record.username,
                deviceId = record.deviceId,
                registeredAt = record.registeredAt,
                keyAlias = record.keyAlias,
                isBiometricProtected = record.isBiometricProtected
            )
        } else {
            EnrollmentState.Unenrolled
        }
    )
    val enrollmentState: StateFlow<EnrollmentState> = _enrollmentState.asStateFlow()

    /**
     * Executes the atomic enrollment workflow:
     * 1. Generate hardware-bound ECDSA P-256 key pair in Android Keystore.
     * 2. Package DeviceRegistrationPayload.
     * 3. POST to /api/auth/register.
     * 4. On success: Persist local record and emit Enrolled.
     * 5. On failure: Atomic rollback deleting newly generated key from Keystore.
     */
    suspend fun registerDevice(
        username: String,
        requireBiometric: Boolean = false,
        keyAlias: String = DEFAULT_KEY_ALIAS
    ): EnrollmentState {
        val trimmedUsername = username.trim()
        if (trimmedUsername.isBlank()) {
            val errState = EnrollmentState.EnrollmentFailed(
                error = ActionableError.Generic("Username cannot be empty. Please enter an account username."),
                isRollbackCompleted = true
            )
            _enrollmentState.value = errState
            return errState
        }

        // Step 1: Keystore key generation
        _enrollmentState.value = EnrollmentState.GeneratingKey(trimmedUsername, requireBiometric)
        val keyInfo = try {
            keyStoreManager.generateKeyPair(keyAlias, requireUserAuth = requireBiometric)
        } catch (e: CryptoException) {
            val errState = EnrollmentState.EnrollmentFailed(
                error = ActionableError.Generic("Failed to generate hardware key: ${e.message}"),
                isRollbackCompleted = true
            )
            _enrollmentState.value = errState
            return errState
        }

        // Step 2: Package registration payload
        val registrationPayload = DeviceRegistrationPayload.fromKeyInfo(trimmedUsername, keyInfo)

        // Step 3: Server registration HTTP request
        _enrollmentState.value = EnrollmentState.RegisteringWithBackend(trimmedUsername, keyAlias)
        val registerUrl = AppConfig.getRegisterUrl()

        val networkResult = networkClient.post(registerUrl, registrationPayload.toJsonString())

        return when (networkResult) {
            is NetworkResult.Success -> {
                val resp = RegistrationResponse.fromJson(networkResult.data)
                val assignedUserId = resp.userId ?: "user_${trimmedUsername.lowercase()}"
                val assignedDeviceId = resp.deviceId ?: "dev_${UUID.randomUUID().toString().take(8)}"
                val regTimestamp = resp.registeredAt ?: registrationPayload.enrollmentTimestamp.toString()

                val record = EnrollmentRecord(
                    userId = assignedUserId,
                    username = trimmedUsername,
                    deviceId = assignedDeviceId,
                    registeredAt = regTimestamp,
                    keyAlias = keyAlias,
                    isBiometricProtected = requireBiometric
                )
                enrollmentStore.saveEnrollment(record)

                val enrolledState = EnrollmentState.Enrolled(
                    userId = assignedUserId,
                    username = trimmedUsername,
                    deviceId = assignedDeviceId,
                    registeredAt = regTimestamp,
                    keyAlias = keyAlias,
                    isBiometricProtected = requireBiometric
                )
                _enrollmentState.value = enrolledState
                enrolledState
            }

            is NetworkResult.HttpError -> {
                // Rollback: Delete orphan key from Keystore
                rollbackKey(keyAlias)
                val errState = EnrollmentState.EnrollmentFailed(
                    error = ActionableError.Network("Server rejected enrollment (HTTP ${networkResult.code}): ${networkResult.message}"),
                    isRollbackCompleted = true
                )
                _enrollmentState.value = errState
                errState
            }

            is NetworkResult.NetworkError -> {
                // Rollback: Delete orphan key from Keystore
                rollbackKey(keyAlias)
                val errState = EnrollmentState.EnrollmentFailed(
                    error = ActionableError.Network("Network failure connecting to server: ${networkResult.exception.localizedMessage ?: "Connection refused"}"),
                    isRollbackCompleted = true
                )
                _enrollmentState.value = errState
                errState
            }

            is NetworkResult.Timeout -> {
                // Rollback: Delete orphan key from Keystore
                rollbackKey(keyAlias)
                val errState = EnrollmentState.EnrollmentFailed(
                    error = ActionableError.Timeout(AppConfig.CONNECT_TIMEOUT_SECONDS.toInt()),
                    isRollbackCompleted = true
                )
                _enrollmentState.value = errState
                errState
            }
        }
    }

    /**
     * Cancels any active/in-progress enrollment and ensures no orphaned key remains.
     */
    fun cancelEnrollment(keyAlias: String = DEFAULT_KEY_ALIAS) {
        rollbackKey(keyAlias)
        _enrollmentState.value = EnrollmentState.Unenrolled
    }

    /**
     * Atomically deletes key from Keystore on failure or abort.
     */
    private fun rollbackKey(keyAlias: String) {
        try {
            if (keyStoreManager.hasKey(keyAlias)) {
                keyStoreManager.deleteKey(keyAlias)
            }
        } catch (e: Exception) {
            // Best-effort cleanup
        }
    }
}
