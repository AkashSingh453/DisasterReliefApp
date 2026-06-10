package com.disasterrelief.app.presentation.onboarding

import android.content.Context
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.UUID
import javax.inject.Inject

data class OnboardingState(
    val name: String = ""
)

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _state = MutableStateFlow(OnboardingState())
    val state: StateFlow<OnboardingState> = _state.asStateFlow()

    fun updateName(name: String) {
        _state.update { it.copy(name = name) }
    }

    fun completeOnboarding(): Boolean {
        val currentState = _state.value
        if (currentState.name.isBlank()) return false

        val prefs = context.getSharedPreferences("disaster_relief_prefs", Context.MODE_PRIVATE)
        prefs.edit().apply {
            putString("node_id", UUID.randomUUID().toString())
            putString("node_name", currentState.name.trim())
            putString("node_role", "VOLUNTEER") // Default standard node role
            apply()
        }
        return true
    }
}
