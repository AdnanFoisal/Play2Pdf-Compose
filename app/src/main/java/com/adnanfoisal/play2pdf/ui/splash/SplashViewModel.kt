package com.adnanfoisal.play2pdf.ui.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.adnanfoisal.play2pdf.data.prefs.SettingsRepository
import com.adnanfoisal.play2pdf.tokens.Motion
import com.adnanfoisal.play2pdf.ui.navigation.Routes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val settings: SettingsRepository
) : ViewModel() {

    /**
     * Decide where to go after the splash hold (2.5s).
     *
     *  - If onboarding has been completed → main
     *  - Otherwise → onboarding
     *
     * The splash ALWAYS holds for the full [Motion.Durations.Splash]
     * duration so the user sees the animation, even if the DataStore
     * read is instant.
     */
    fun boot(onNavigate: (String) -> Unit) {
        viewModelScope.launch {
            val onboardingComplete = settings.settings.first().onboardingComplete
            // Hold for the full splash duration regardless.
            delay(Motion.Durations.Splash.toLong())
            onNavigate(if (onboardingComplete) Routes.Main else Routes.Onboarding)
        }
    }
}
