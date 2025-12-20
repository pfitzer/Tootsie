package com.pfitzer.tootsie.presentation.screens.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pfitzer.tootsie.domain.model.AppCredentials
import com.pfitzer.tootsie.domain.usecase.auth.*
import com.pfitzer.tootsie.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for Login Screen.
 * Manages the complete OAuth2 authentication flow using MVI pattern.
 */
@HiltViewModel
class LoginViewModel @Inject constructor(
    private val registerAppUseCase: RegisterAppUseCase,
    private val getAuthorizationUrlUseCase: GetAuthorizationUrlUseCase,
    private val exchangeCodeForTokenUseCase: ExchangeCodeForTokenUseCase,
    private val verifyCredentialsUseCase: VerifyCredentialsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    /**
     * Handle user actions.
     */
    fun onAction(action: LoginAction) {
        when (action) {
            is LoginAction.UpdateInstanceUrl -> updateInstanceUrl(action.url)
            is LoginAction.Login -> startLogin()
            is LoginAction.HandleAuthorizationCode -> handleAuthorizationCode(action.code)
            is LoginAction.DismissError -> dismissError()
        }
    }

    /**
     * Update instance URL input.
     */
    private fun updateInstanceUrl(url: String) {
        _uiState.update { it.copy(instanceUrl = url, errorMessage = null) }
    }

    /**
     * Start the login flow - register app and open browser for OAuth.
     */
    private fun startLogin() {
        if (_uiState.value.instanceUrl.isBlank()) {
            _uiState.update {
                it.copy(errorMessage = "Please enter a Mastodon instance URL")
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            // Step 1: Register app with instance
            when (val result = registerAppUseCase(_uiState.value.instanceUrl)) {
                is Result.Success -> {
                    val appCredentials = result.data
                    _uiState.update { it.copy(appCredentials = appCredentials) }

                    // Step 2: Build authorization URL
                    val authUrl = getAuthorizationUrlUseCase(
                        instanceUrl = _uiState.value.instanceUrl,
                        appCredentials = appCredentials
                    )

                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            authorizationUrl = authUrl,
                            loginStep = LoginStep.AWAITING_AUTHORIZATION
                        )
                    }
                }

                is Result.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = "Failed to connect to instance: ${result.message}"
                        )
                    }
                }

                is Result.Loading -> Unit
            }
        }
    }

    /**
     * Handle authorization code from OAuth callback.
     * Exchange code for access token and verify credentials.
     */
    private fun handleAuthorizationCode(code: String) {
        val appCredentials = _uiState.value.appCredentials
        if (appCredentials == null) {
            _uiState.update {
                it.copy(errorMessage = "App credentials not found. Please try logging in again.")
            }
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(isLoading = true, loginStep = LoginStep.EXCHANGING_TOKEN)
            }

            // Step 3: Exchange authorization code for access token
            when (val tokenResult = exchangeCodeForTokenUseCase(
                instanceUrl = _uiState.value.instanceUrl,
                appCredentials = appCredentials,
                authorizationCode = code
            )) {
                is Result.Success -> {
                    // Step 4: Verify credentials and get user account
                    when (val credentialsResult = verifyCredentialsUseCase()) {
                        is Result.Success -> {
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    loginStep = LoginStep.COMPLETED,
                                    isAuthenticated = true
                                )
                            }
                        }

                        is Result.Error -> {
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    errorMessage = "Failed to verify credentials: ${credentialsResult.message}"
                                )
                            }
                        }

                        is Result.Loading -> Unit
                    }
                }

                is Result.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = "Failed to exchange authorization code: ${tokenResult.message}"
                        )
                    }
                }

                is Result.Loading -> Unit
            }
        }
    }

    /**
     * Dismiss error message.
     */
    private fun dismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}

/**
 * UI State for Login Screen (MVI pattern).
 */
data class LoginUiState(
    val instanceUrl: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val appCredentials: AppCredentials? = null,
    val authorizationUrl: String? = null,
    val loginStep: LoginStep = LoginStep.INITIAL,
    val isAuthenticated: Boolean = false
)

/**
 * Login flow steps.
 */
enum class LoginStep {
    INITIAL,                // User enters instance URL
    REGISTERING_APP,        // Registering app with instance
    AWAITING_AUTHORIZATION, // Waiting for user to authorize in browser
    EXCHANGING_TOKEN,       // Exchanging code for token
    VERIFYING_CREDENTIALS,  // Verifying user credentials
    COMPLETED               // Login successful
}

/**
 * User actions for Login Screen (MVI pattern).
 */
sealed class LoginAction {
    data class UpdateInstanceUrl(val url: String) : LoginAction()
    data object Login : LoginAction()
    data class HandleAuthorizationCode(val code: String) : LoginAction()
    data object DismissError : LoginAction()
}
