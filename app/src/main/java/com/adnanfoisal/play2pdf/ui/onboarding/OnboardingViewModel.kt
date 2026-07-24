package com.adnanfoisal.play2pdf.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.adnanfoisal.play2pdf.data.prefs.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the onboarding flow.
 *
 * Persists [SettingsRepository.setOnboardingComplete] when the user
 * taps "Get Started" or "Skip" so the onboarding only shows once.
 */
@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val settings: SettingsRepository
) : ViewModel() {

    fun markOnboardingComplete() {
        viewModelScope.launch {
            settings.setOnboardingComplete(true)
        }
    }
}
