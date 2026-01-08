package com.pfitzer.tootsie.presentation.screens.auth

import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.pfitzer.tootsie.R
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pfitzer.tootsie.presentation.components.common.ErrorMessage
import com.pfitzer.tootsie.presentation.components.common.LoadingButton

private const val TAG = "LoginScreen"

/**
 * Login Screen - OAuth2 authentication flow.
 * Allows user to enter their Mastodon instance URL and authenticate.
 *
 * @param onLoginSuccess Callback when login completes successfully
 * @param authorizationCode OAuth authorization code from deep link (if any)
 * @param onAuthCodeConsumed Callback when authorization code is processed
 * @param viewModel ViewModel managing login logic
 */
@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    authorizationCode: String? = null,
    onAuthCodeConsumed: () -> Unit = {},
    viewModel: LoginViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Handle authorization code from OAuth callback
    LaunchedEffect(authorizationCode) {
        authorizationCode?.let { code ->
            viewModel.onAction(LoginAction.HandleAuthorizationCode(code))
            onAuthCodeConsumed()
        }
    }

    // Handle successful authentication
    LaunchedEffect(uiState.isAuthenticated) {
        if (uiState.isAuthenticated) {
            onLoginSuccess()
        }
    }

    // Open browser for OAuth when authorization URL is ready
    LaunchedEffect(uiState.authorizationUrl) {
        uiState.authorizationUrl?.let { url ->
            Log.d(TAG, "Opening browser with URL: $url")
            try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                Log.d(TAG, "Browser opened successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to open browser", e)
                viewModel.onAction(LoginAction.DismissError)
            }
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // App Title
            Text(
                text = stringResource(R.string.login_title),
                style = MaterialTheme.typography.displayMedium,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(R.string.login_subtitle),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Instance URL Input
            OutlinedTextField(
                value = uiState.instanceUrl,
                onValueChange = { viewModel.onAction(LoginAction.UpdateInstanceUrl(it)) },
                label = { Text(stringResource(R.string.instance_url)) },
                placeholder = { Text(stringResource(R.string.instance_url_hint)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                enabled = !uiState.isLoading,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Uri,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = { viewModel.onAction(LoginAction.Login) }
                ),
                supportingText = {
                    Text(stringResource(R.string.login_helper_text))
                }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Login Button
            LoadingButton(
                text = stringResource(R.string.login),
                onClick = { viewModel.onAction(LoginAction.Login) },
                isLoading = uiState.isLoading,
                enabled = uiState.instanceUrl.isNotBlank()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Login Step Indicator
            if (uiState.loginStep != LoginStep.INITIAL) {
                LoginStepIndicator(step = uiState.loginStep)
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Manual browser open button (if waiting for authorization)
            if (uiState.loginStep == LoginStep.AWAITING_AUTHORIZATION && uiState.authorizationUrl != null) {
                Button(
                    onClick = {
                        try {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uiState.authorizationUrl))
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            // Ignore
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.login_button))
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Error Message
            uiState.errorMessage?.let { error ->
                ErrorMessage(
                    message = error,
                    modifier = Modifier.padding(top = 16.dp)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Help Text
            Text(
                text = stringResource(R.string.login_help_text),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * Displays the current step in the login flow.
 */
@Composable
private fun LoginStepIndicator(
    step: LoginStep,
    modifier: Modifier = Modifier
) {
    val stepText = when (step) {
        LoginStep.INITIAL -> ""
        LoginStep.REGISTERING_APP -> "Registering with instance..."
        LoginStep.AWAITING_AUTHORIZATION -> "Waiting for authorization in browser..."
        LoginStep.EXCHANGING_TOKEN -> "Exchanging authorization code..."
        LoginStep.VERIFYING_CREDENTIALS -> "Verifying credentials..."
        LoginStep.COMPLETED -> "Login successful!"
    }

    if (stepText.isNotEmpty()) {
        Card(
            modifier = modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (step != LoginStep.COMPLETED) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                }
                Text(
                    text = stepText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }
    }
}
