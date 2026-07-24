package com.adnanfoisal.play2pdf.core.haptics

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.adnanfoisal.play2pdf.data.prefs.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Haptics manager — wraps the platform [Vibrator] / [VibratorManager] and
 * exposes 5 named patterns per the v2.0 §12.3 haptic patterns table.
 *
 * All calls respect the user's "Haptic feedback" setting (gated via
 * [SettingsRepository.hapticsEnabled]).
 *
 * On Android < 12 we use the deprecated [Vibrator.vibrate] API with a
 * fixed duration; on Android 12+ we use [VibrationEffect] with predefined
 * effects so the system can do the right thing per device.
 *
 * Usage:
 *   class MyViewModel @Inject constructor(private val haptics: HapticsManager) {
 *       fun onButtonPressed() {
 *           viewModelScope.launch { haptics.light() }
 *       }
 *   }
 */
@Singleton
class HapticsManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settings: SettingsRepository
) {
    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val mgr = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
        mgr?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    suspend fun light() = perform(HapticPattern.Light)
    suspend fun medium() = perform(HapticPattern.Medium)
    suspend fun heavy() = perform(HapticPattern.Heavy)
    suspend fun success() = perform(HapticPattern.Success)
    suspend fun error() = perform(HapticPattern.Error)

    private suspend fun perform(pattern: HapticPattern) {
        if (!settings.settings.first().hapticsEnabled) return
        val v = vibrator ?: return
        if (!v.hasVibrator()) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val effect = when (pattern) {
                HapticPattern.Light ->
                    VibrationEffect.createOneShot(20, VibrationEffect.DEFAULT_AMPLITUDE)
                HapticPattern.Medium ->
                    VibrationEffect.createOneShot(40, VibrationEffect.DEFAULT_AMPLITUDE)
                HapticPattern.Heavy ->
                    VibrationEffect.createOneShot(80, VibrationEffect.DEFAULT_AMPLITUDE)
                HapticPattern.Success ->
                    VibrationEffect.createWaveform(longArrayOf(0, 30, 80, 30), -1)
                HapticPattern.Error ->
                    VibrationEffect.createWaveform(longArrayOf(0, 80, 60, 80), -1)
            }
            v.vibrate(effect)
        } else {
            @Suppress("DEPRECATION")
            v.vibrate(when (pattern) {
                HapticPattern.Light -> 20
                HapticPattern.Medium -> 40
                HapticPattern.Heavy -> 80
                HapticPattern.Success -> 110
                HapticPattern.Error -> 140
            })
        }
    }
}

enum class HapticPattern { Light, Medium, Heavy, Success, Error }
