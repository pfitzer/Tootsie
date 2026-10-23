package com.pfitzer.tootsie.presentation.screens.splash

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * Splash Screen - displays while checking authentication status.
 * Automatically navigates to Login or Home based on auth state.
 *
 * @param onNavigateToLogin Callback when user is not authenticated
 * @param onNavigateToHome Callback when user is authenticated
 * @param viewModel ViewModel for managing splash logic
 */
@Composable
fun SplashScreen(
    onNavigateToLogin: () -> Unit,
    onNavigateToHome: () -> Unit,
    viewModel: SplashViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Handle navigation based on authentication state
    LaunchedEffect(uiState) {
        when (uiState) {
            is SplashUiState.Authenticated -> onNavigateToHome()
            is SplashUiState.Unauthenticated -> onNavigateToLogin()
            is SplashUiState.Loading -> Unit // Show loading indicator
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}
