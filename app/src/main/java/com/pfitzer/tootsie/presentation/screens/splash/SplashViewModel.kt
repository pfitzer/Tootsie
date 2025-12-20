package com.pfitzer.tootsie.presentation.screens.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pfitzer.tootsie.domain.usecase.auth.CheckLoginStatusUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for Splash Screen.
 * Checks authentication status and determines initial navigation.
 */
@HiltViewModel
class SplashViewModel @Inject constructor(
    private val checkLoginStatusUseCase: CheckLoginStatusUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<SplashUiState>(SplashUiState.Loading)
    val uiState: StateFlow<SplashUiState> = _uiState.asStateFlow()

    init {
        checkAuthStatus()
    }

    /**
     * Check if user is authenticated.
     */
    private fun checkAuthStatus() {
        viewModelScope.launch {
            checkLoginStatusUseCase().collect { isLoggedIn ->
                _uiState.value = if (isLoggedIn) {
                    SplashUiState.Authenticated
                } else {
                    SplashUiState.Unauthenticated
                }
            }
        }
    }
}

/**
 * UI state for Splash Screen.
 */
sealed class SplashUiState {
    data object Loading : SplashUiState()
    data object Authenticated : SplashUiState()
    data object Unauthenticated : SplashUiState()
}
