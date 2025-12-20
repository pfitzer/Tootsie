package com.pfitzer.tootsie.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.pfitzer.tootsie.presentation.screens.auth.LoginScreen
import com.pfitzer.tootsie.presentation.screens.splash.SplashScreen

/**
 * Main navigation host for the Tootsie app.
 * Manages navigation between all screens.
 *
 * @param navController Navigation controller for managing screen transitions
 * @param modifier Optional modifier for the NavHost
 * @param startDestination Initial screen to display
 * @param authorizationCode OAuth authorization code from deep link (if any)
 * @param onAuthCodeConsumed Callback when authorization code is processed
 */
@Composable
fun TootsieNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    startDestination: String = TootsieDestinations.Splash.route,
    authorizationCode: String? = null,
    onAuthCodeConsumed: () -> Unit = {}
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        // Splash screen - initial screen that checks auth status
        composable(route = TootsieDestinations.Splash.route) {
            SplashScreen(
                onNavigateToLogin = {
                    navController.navigate(TootsieDestinations.Login.route) {
                        popUpTo(TootsieDestinations.Splash.route) {
                            inclusive = true
                        }
                    }
                },
                onNavigateToHome = {
                    navController.navigate(TootsieDestinations.Home.route) {
                        popUpTo(TootsieDestinations.Splash.route) {
                            inclusive = true
                        }
                    }
                }
            )
        }

        // Login screen - OAuth authentication
        composable(route = TootsieDestinations.Login.route) {
            LoginScreen(
                authorizationCode = authorizationCode,
                onAuthCodeConsumed = onAuthCodeConsumed,
                onLoginSuccess = {
                    navController.navigate(TootsieDestinations.Home.route) {
                        popUpTo(TootsieDestinations.Login.route) {
                            inclusive = true
                        }
                    }
                }
            )
        }

        // OAuth callback screen - handles authorization code
        composable(
            route = TootsieDestinations.OAuthCallback.route,
            arguments = listOf(
                navArgument(TootsieDestinations.OAuthCallback.ARG_CODE) {
                    type = NavType.StringType
                }
            )
        ) { backStackEntry ->
            val code = backStackEntry.arguments?.getString(
                TootsieDestinations.OAuthCallback.ARG_CODE
            ) ?: return@composable

            // OAuth callback handling will be implemented later
            // For now, navigate back to login
            navController.popBackStack()
        }

        // Home screen - main timeline
        composable(route = TootsieDestinations.Home.route) {
            // HomeScreen will be implemented in Phase 3
            // Placeholder for now
        }

        // Compose screen - create new post
        composable(route = TootsieDestinations.Compose.route) {
            // ComposeScreen will be implemented in Phase 3
        }

        // Profile screen
        composable(
            route = TootsieDestinations.Profile.route,
            arguments = listOf(
                navArgument(TootsieDestinations.Profile.ARG_ACCOUNT_ID) {
                    type = NavType.StringType
                }
            )
        ) { backStackEntry ->
            val accountId = backStackEntry.arguments?.getString(
                TootsieDestinations.Profile.ARG_ACCOUNT_ID
            ) ?: return@composable

            // ProfileScreen will be implemented in Phase 3
        }

        // Status detail screen
        composable(
            route = TootsieDestinations.StatusDetail.route,
            arguments = listOf(
                navArgument(TootsieDestinations.StatusDetail.ARG_STATUS_ID) {
                    type = NavType.StringType
                }
            )
        ) { backStackEntry ->
            val statusId = backStackEntry.arguments?.getString(
                TootsieDestinations.StatusDetail.ARG_STATUS_ID
            ) ?: return@composable

            // StatusDetailScreen will be implemented in Phase 3
        }

        // Search screen
        composable(route = TootsieDestinations.Search.route) {
            // SearchScreen will be implemented in Phase 3
        }

        // Notifications screen
        composable(route = TootsieDestinations.Notifications.route) {
            // NotificationsScreen will be implemented in Phase 3
        }

        // Settings screen
        composable(route = TootsieDestinations.Settings.route) {
            // SettingsScreen will be implemented in Phase 3
        }
    }
}
