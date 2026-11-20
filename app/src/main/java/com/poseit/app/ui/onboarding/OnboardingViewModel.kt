package com.poseit.app.ui.onboarding

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.poseit.app.data.SettingsRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Persists "intro seen" in the ViewModel scope, so leaving the screen can't cancel the write half-way.
 * [done] flips to true once it is stored; the screen then calls its onDone callback (only while still shown).
 */
class OnboardingViewModel(private val settingsRepository: SettingsRepository) : ViewModel() {

    private val _finishing = MutableStateFlow(false)
    val finishing: StateFlow<Boolean> = _finishing.asStateFlow()

    private val _done = MutableStateFlow(false)
    val done: StateFlow<Boolean> = _done.asStateFlow()

    fun finish() {
        if (_finishing.value) return
        _finishing.value = true
        viewModelScope.launch {
            try {
                settingsRepository.update { it.copy(onboardingDone = true) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // Still let the user into the app; the intro would just show again next time.
                Log.w("Onboarding", "Could not store onboarding state", e)
            }
            _done.value = true
        }
    }
}
