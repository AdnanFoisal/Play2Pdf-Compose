package com.adnanfoisal.play2pdf.core.sound

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import com.adnanfoisal.play2pdf.R
import com.adnanfoisal.play2pdf.data.prefs.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Sound manager — wraps [SoundPool] with 6 sound effects per Asset K.
 *
 * Per §3.2 fallback policy: if the WAV files are NOT yet delivered by
 * the Design Agent, [play] silently no-ops (because [loadSound] returns
 * 0 when the resource doesn't exist). The app is silent but functional.
 *
 * All calls respect the user's "Sound effects" setting.
 *
 * Usage:
 *   class MyViewModel @Inject constructor(private val sounds: SoundManager) {
 *       fun onCompileSuccess() {
 *           viewModelScope.launch { sounds.play(SoundEffect.Success) }
 *       }
 *   }
 */
@Singleton
class SoundManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settings: SettingsRepository
) {
    private val pool: SoundPool = SoundPool.Builder()
        .setMaxStreams(4)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        )
        .build()

    private val soundIds: Map<SoundEffect, Int> = SoundEffect.entries.associateWith { effect ->
        loadSound(effect)
    }

    private fun loadSound(effect: SoundEffect): Int {
        val resId = when (effect) {
            SoundEffect.Tap -> R.raw.sfx_tap
            SoundEffect.ChipAdd -> R.raw.sfx_chip_add
            SoundEffect.ChipRemove -> R.raw.sfx_chip_remove
            SoundEffect.Success -> R.raw.sfx_success
            SoundEffect.Error -> R.raw.sfx_error
            SoundEffect.Nav -> R.raw.sfx_nav
        }
        // If the resource doesn't exist (Design Agent hasn't delivered Asset K),
        // R.raw.sfx_* will be 0 — load() returns 0, which play() no-ops on.
        return if (resId != 0) pool.load(context, resId, 1) else 0
    }

    suspend fun play(effect: SoundEffect, volume: Float = 0.3f) {
        if (!settings.settings.first().soundEnabled) return
        val id = soundIds[effect] ?: return
        if (id == 0) return
        pool.play(id, volume, volume, 1, 0, 1f)
    }
}

enum class SoundEffect { Tap, ChipAdd, ChipRemove, Success, Error, Nav }
