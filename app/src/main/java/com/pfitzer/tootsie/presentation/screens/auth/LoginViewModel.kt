package com.pfitzer.tootsie.presentation.screens.auth

import com.pfitzer.tootsie.R
import com.pfitzer.tootsie.presentation.util.UiText
import android.util.Log
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

private const val TAG = "LoginViewModel"

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
        Log.d(TAG, "startLogin() called with instance: ${_uiState.value.instanceUrl}")

        if (_uiState.value.instanceUrl.isBlank()) {
            Log.w(TAG, "Instance URL is blank")
            _uiState.update {
                it.copy(errorMessage = UiText.of(R.string.error_enter_instance))
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, loginStep = LoginStep.REGISTERING_APP) }
            Log.d(TAG, "Starting app registration")

            // Step 1: Register app with instance
            when (val result = registerAppUseCase(_uiState.value.instanceUrl)) {
                is Result.Success -> {
                    val appCredentials = result.data
                    Log.d(TAG, "App registration successful: clientId=${appCredentials.clientId}")
                    _uiState.update { it.copy(appCredentials = appCredentials) }

                    // Step 2: Build authorization URL
                    val authUrl = getAuthorizationUrlUseCase(
                        instanceUrl = _uiState.value.instanceUrl,
                        appCredentials = appCredentials
                    )
                    Log.d(TAG, "Authorization URL generated: $authUrl")

                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            authorizationUrl = authUrl,
                            loginStep = LoginStep.AWAITING_AUTHORIZATION
                        )
                    }
                    Log.d(TAG, "UI state updated with authorization URL")
                }

                is Result.Error -> {
                    Log.e(TAG, "App registration failed: ${result.message}", result.exception)
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = UiText.of(R.string.error_connect_instance, result.message.orEmpty()),
                            loginStep = LoginStep.INITIAL
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
        Log.d(TAG, "handleAuthorizationCode() called with code: ${code.take(10)}...")

        val appCredentials = _uiState.value.appCredentials
        if (appCredentials == null) {
            Log.e(TAG, "App credentials not found when handling authorization code")
            _uiState.update {
                it.copy(errorMessage = UiText.of(R.string.error_app_credentials_missing))
            }
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(isLoading = true, loginStep = LoginStep.EXCHANGING_TOKEN)
            }
            Log.d(TAG, "Exchanging authorization code for token")

            // Step 3: Exchange authorization code for access token
            when (val tokenResult = exchangeCodeForTokenUseCase(
                instanceUrl = _uiState.value.instanceUrl,
                appCredentials = appCredentials,
                authorizationCode = code
            )) {
                is Result.Success -> {
                    Log.d(TAG, "Token exchange successful, verifying credentials")
                    _uiState.update { it.copy(loginStep = LoginStep.VERIFYING_CREDENTIALS) }

                    // Step 4: Verify credentials and get user account
                    when (val credentialsResult = verifyCredentialsUseCase()) {
                        is Result.Success -> {
                            Log.d(TAG, "Credentials verified successfully, login complete")
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    loginStep = LoginStep.COMPLETED,
                                    isAuthenticated = true
                                )
                            }
                        }

                        is Result.Error -> {
                            Log.e(TAG, "Credential verification failed: ${credentialsResult.message}", credentialsResult.exception)
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    errorMessage = UiText.of(R.string.error_verify_credentials, credentialsResult.message.orEmpty()),
                                    loginStep = LoginStep.INITIAL
                                )
                            }
                        }

                        is Result.Loading -> Unit
                    }
                }

                is Result.Error -> {
                    Log.e(TAG, "Token exchange failed: ${tokenResult.message}", tokenResult.exception)
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = UiText.of(R.string.error_exchange_code, tokenResult.message.orEmpty()),
                            loginStep = LoginStep.INITIAL
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
    val errorMessage: UiText? = null,
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
