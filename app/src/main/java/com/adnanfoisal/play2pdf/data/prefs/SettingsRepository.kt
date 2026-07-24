package com.adnanfoisal.play2pdf.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.adnanfoisal.play2pdf.domain.model.PdfTheme
import com.adnanfoisal.play2pdf.domain.model.UserSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * DataStore-backed user settings.
 *
 * DataStore is the official AndroidX preference store for new apps
 * (replaces SharedPreferences). It's coroutine-based, type-safe, and
 * handles lifecycle correctly.
 *
 * One file: `play2pdf_settings.preferences_pb`. All keys are private —
 * callers go through [observe] / [update] / individual setters.
 */

private val Context.prefsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "play2pdf_settings"
)

@Singleton
class SettingsRepository @Inject constructor(
    private val context: Context
) {
    private object Keys {
        val YoutubeApiKey = stringPreferencesKey("youtube_api_key")
        val GeminiApiKey = stringPreferencesKey("gemini_api_key")
        val BackendUrl = stringPreferencesKey("backend_url")
        val UserName = stringPreferencesKey("user_name")
        val OnboardingComplete = booleanPreferencesKey("onboarding_complete")
        val SelectedTheme = stringPreferencesKey("selected_theme")
        val SoundEnabled = booleanPreferencesKey("sound_enabled")
        val HapticsEnabled = booleanPreferencesKey("haptics_enabled")
    }

    /** Default backend URL — matches the FastAPI app on HuggingFace Space. */
    val defaultBackendUrl: String = "https://adnanfoisal-play2pdf.hf.space"

    /** Reactive stream of the user's full settings object. */
    val settings: Flow<UserSettings> = context.prefsDataStore.data.map { p ->
        UserSettings(
            youtubeApiKey = p[Keys.YoutubeApiKey] ?: "",
            geminiApiKey = p[Keys.GeminiApiKey] ?: "",
            backendUrl = p[Keys.BackendUrl] ?: defaultBackendUrl,
            userName = p[Keys.UserName] ?: "",
            onboardingComplete = p[Keys.OnboardingComplete] ?: false,
            selectedTheme = PdfTheme.fromApiName(p[Keys.SelectedTheme] ?: PdfTheme.TufteScholar.apiName),
            soundEnabled = p[Keys.SoundEnabled] ?: true,
            hapticsEnabled = p[Keys.HapticsEnabled] ?: true
        )
    }

    suspend fun setYoutubeApiKey(value: String) =
        context.prefsDataStore.edit { it[Keys.YoutubeApiKey] = value }

    suspend fun setGeminiApiKey(value: String) =
        context.prefsDataStore.edit { it[Keys.GeminiApiKey] = value }

    suspend fun setBackendUrl(value: String) =
        context.prefsDataStore.edit { it[Keys.BackendUrl] = value }

    suspend fun setUserName(value: String) =
        context.prefsDataStore.edit { it[Keys.UserName] = value }

    suspend fun setOnboardingComplete(value: Boolean) =
        context.prefsDataStore.edit { it[Keys.OnboardingComplete] = value }

    suspend fun setSelectedTheme(value: PdfTheme) =
        context.prefsDataStore.edit { it[Keys.SelectedTheme] = value.apiName }

    suspend fun setSoundEnabled(value: Boolean) =
        context.prefsDataStore.edit { it[Keys.SoundEnabled] = value }

    suspend fun setHapticsEnabled(value: Boolean) =
        context.prefsDataStore.edit { it[Keys.HapticsEnabled] = value }

    /** Convenience: patch multiple fields atomically in one transaction. */
    suspend fun update(block: (UserSettings) -> UserSettings) {
        context.prefsDataStore.edit { p ->
            val current = UserSettings(
                youtubeApiKey = p[Keys.YoutubeApiKey] ?: "",
                geminiApiKey = p[Keys.GeminiApiKey] ?: "",
                backendUrl = p[Keys.BackendUrl] ?: defaultBackendUrl,
                userName = p[Keys.UserName] ?: "",
                onboardingComplete = p[Keys.OnboardingComplete] ?: false,
                selectedTheme = PdfTheme.fromApiName(p[Keys.SelectedTheme] ?: PdfTheme.TufteScholar.apiName),
                soundEnabled = p[Keys.SoundEnabled] ?: true,
                hapticsEnabled = p[Keys.HapticsEnabled] ?: true
            )
            val updated = block(current)
            p[Keys.YoutubeApiKey] = updated.youtubeApiKey
            p[Keys.GeminiApiKey] = updated.geminiApiKey
            p[Keys.BackendUrl] = updated.backendUrl
            p[Keys.UserName] = updated.userName
            p[Keys.OnboardingComplete] = updated.onboardingComplete
            p[Keys.SelectedTheme] = updated.selectedTheme.apiName
            p[Keys.SoundEnabled] = updated.soundEnabled
            p[Keys.HapticsEnabled] = updated.hapticsEnabled
        }
    }
}
