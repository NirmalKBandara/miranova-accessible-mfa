package com.miranova.mfa.data.store

import android.content.Context

/**
 * Metadata record for a successfully enrolled device bound to an identity.
 * Private key is never stored here; only the non-exportable hardware key alias is referenced.
 */
data class EnrollmentRecord(
    val userId: String,
    val username: String,
    val deviceId: String,
    val registeredAt: String,
    val keyAlias: String,
    val isBiometricProtected: Boolean = false,
    val isInvalidated: Boolean = false
)

/**
 * Storage contract for managing local device enrollment state.
 */
interface EnrollmentStore {
    fun saveEnrollment(record: EnrollmentRecord)
    fun getEnrollment(): EnrollmentRecord?
    fun isEnrolled(): Boolean
    fun markInvalidated()
    fun clearEnrollment()
}

/**
 * Thread-safe in-memory storage implementation for enrollment state.
 */
class MemoryEnrollmentStore : EnrollmentStore {
    private var currentRecord: EnrollmentRecord? = null

    override fun saveEnrollment(record: EnrollmentRecord) {
        currentRecord = record
    }

    override fun getEnrollment(): EnrollmentRecord? {
        return currentRecord
    }

    override fun isEnrolled(): Boolean {
        return currentRecord != null && !(currentRecord?.isInvalidated ?: false)
    }

    override fun markInvalidated() {
        currentRecord = currentRecord?.copy(isInvalidated = true)
    }

    override fun clearEnrollment() {
        currentRecord = null
    }
}

/**
 * Persistent SharedPreferences-backed enrollment store.
 */
class SharedPreferencesEnrollmentStore(context: Context) : EnrollmentStore {
    private val prefs = context.getSharedPreferences("miranova_enrollment_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_USER_ID = "enr_user_id"
        private const val KEY_USERNAME = "enr_username"
        private const val KEY_DEVICE_ID = "enr_device_id"
        private const val KEY_REGISTERED_AT = "enr_registered_at"
        private const val KEY_KEY_ALIAS = "enr_key_alias"
        private const val KEY_BIOMETRIC = "enr_biometric"
        private const val KEY_INVALIDATED = "enr_invalidated"
    }

    override fun saveEnrollment(record: EnrollmentRecord) {
        prefs.edit()
            .putString(KEY_USER_ID, record.userId)
            .putString(KEY_USERNAME, record.username)
            .putString(KEY_DEVICE_ID, record.deviceId)
            .putString(KEY_REGISTERED_AT, record.registeredAt)
            .putString(KEY_KEY_ALIAS, record.keyAlias)
            .putBoolean(KEY_BIOMETRIC, record.isBiometricProtected)
            .putBoolean(KEY_INVALIDATED, record.isInvalidated)
            .apply()
    }

    override fun getEnrollment(): EnrollmentRecord? {
        val username = prefs.getString(KEY_USERNAME, null) ?: return null
        val userId = prefs.getString(KEY_USER_ID, "") ?: ""
        val deviceId = prefs.getString(KEY_DEVICE_ID, "") ?: ""
        val registeredAt = prefs.getString(KEY_REGISTERED_AT, "") ?: ""
        val keyAlias = prefs.getString(KEY_KEY_ALIAS, "") ?: ""
        val isBiometric = prefs.getBoolean(KEY_BIOMETRIC, false)
        val isInvalidated = prefs.getBoolean(KEY_INVALIDATED, false)

        return EnrollmentRecord(
            userId = userId,
            username = username,
            deviceId = deviceId,
            registeredAt = registeredAt,
            keyAlias = keyAlias,
            isBiometricProtected = isBiometric,
            isInvalidated = isInvalidated
        )
    }

    override fun isEnrolled(): Boolean {
        val record = getEnrollment()
        return record != null && !record.isInvalidated
    }

    override fun markInvalidated() {
        prefs.edit().putBoolean(KEY_INVALIDATED, true).apply()
    }

    override fun clearEnrollment() {
        prefs.edit().clear().apply()
    }
}
