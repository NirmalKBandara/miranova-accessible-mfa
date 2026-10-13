package com.miranova.mfa

import com.miranova.mfa.ui.accessibility.AccessibilityPreferences
import com.miranova.mfa.ui.accessibility.MemoryAccessibilityPreferencesStore
import com.miranova.mfa.ui.accessibility.SystemAudioInspector
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests validating AccessibilityPreferences model, memory store mutations,
 * and system audio environment detection.
 */
class AccessibilityPreferencesTest {

    private lateinit var store: MemoryAccessibilityPreferencesStore

    @Before
    fun setUp() {
        store = MemoryAccessibilityPreferencesStore()
    }

    @Test
    fun `default preferences enable all complementary accessibility channels`() {
        val defaults = store.getPreferences()
        assertTrue(defaults.isHapticEnabled)
        assertTrue(defaults.isAudioEarconsEnabled)
        assertTrue(defaults.isTtsGuidanceEnabled)
        assertEquals(1.0f, defaults.speechRate, 0.001f)
    }

    @Test
    fun `preference store updates individual channel toggles correctly`() {
        store.updateHapticEnabled(false)
        assertFalse(store.getPreferences().isHapticEnabled)
        assertTrue(store.getPreferences().isAudioEarconsEnabled)

        store.updateAudioEarconsEnabled(false)
        assertFalse(store.getPreferences().isAudioEarconsEnabled)

        store.updateTtsGuidanceEnabled(false)
        assertFalse(store.getPreferences().isTtsGuidanceEnabled)

        store.updateSpeechRate(1.5f)
        assertEquals(1.5f, store.getPreferences().speechRate, 0.001f)
    }

    @Test
    fun `resetToDefaults restores initial state`() {
        store.updateHapticEnabled(false)
        store.updateAudioEarconsEnabled(false)
        store.updateTtsGuidanceEnabled(false)
        store.updateSpeechRate(2.0f)

        store.resetToDefaults()

        val restored = store.getPreferences()
        assertTrue(restored.isHapticEnabled)
        assertTrue(restored.isAudioEarconsEnabled)
        assertTrue(restored.isTtsGuidanceEnabled)
        assertEquals(1.0f, restored.speechRate, 0.001f)
    }

    @Test
    fun `system audio inspector gracefully handles null context without exception`() {
        val inspector = SystemAudioInspector(null)
        assertFalse(inspector.isSystemSilentOrVibrate())
        assertFalse(inspector.isNotificationVolumeZero())
    }
}
