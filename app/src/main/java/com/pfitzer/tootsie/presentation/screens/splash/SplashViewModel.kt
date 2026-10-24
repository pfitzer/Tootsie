package com.pfitzer.tootsie.presentation.screens.splash

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pfitzer.tootsie.data.repository.AuthRepositoryImpl
import com.pfitzer.tootsie.domain.usecase.auth.CheckLoginStatusUseCase
import com.pfitzer.tootsie.domain.usecase.timeline.CleanupDatabaseUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

private const val TAG = "SplashViewModel"

/**
 * ViewModel for Splash Screen.
 * Checks authentication status and determines initial navigation.
 */
@HiltViewModel
class SplashViewModel @Inject constructor(
    private val checkLoginStatusUseCase: CheckLoginStatusUseCase,
    private val cleanupDatabaseUseCase: CleanupDatabaseUseCase,
    private val authRepository: AuthRepositoryImpl
) : ViewModel() {

    private val _uiState = MutableStateFlow<SplashUiState>(SplashUiState.Loading)
    val uiState: StateFlow<SplashUiState> = _uiState.asStateFlow()

    init {
        checkAuthStatus()
    }

    /**
     * Check if user is authenticated.
     * Also initializes providers with stored credentials.
     */
    private fun checkAuthStatus() {
        viewModelScope.launch {
            Log.d(TAG, "checkAuthStatus() - Initializing providers...")

            // Initialize providers with stored credentials before checking auth status
            // Use IO dispatcher to avoid DataStore deadlock on main thread
            withContext(Dispatchers.IO) {
                authRepository.ensureInitialized()
                Log.d(TAG, "checkAuthStatus() - Cleaning up database...")
                cleanupDatabaseUseCase()
            }

            Log.d(TAG, "checkAuthStatus() - Checking login status...")
            val isLoggedIn = checkLoginStatusUseCase().first()

            Log.d(TAG, "checkAuthStatus() - isLoggedIn: $isLoggedIn")
            _uiState.value = if (isLoggedIn) {
                SplashUiState.Authenticated
            } else {
                SplashUiState.Unauthenticated
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
