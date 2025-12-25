package com.pfitzer.tootsie.presentation.navigation

import android.util.Log
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navArgument
import com.pfitzer.tootsie.presentation.components.navigation.BottomNavigationBar
import com.pfitzer.tootsie.presentation.screens.auth.LoginScreen
import com.pfitzer.tootsie.presentation.screens.compose.ComposeScreen
import com.pfitzer.tootsie.presentation.screens.drafts.DraftsScreen
import com.pfitzer.tootsie.presentation.screens.home.HomeScreen
import com.pfitzer.tootsie.presentation.screens.notifications.NotificationsScreen
import com.pfitzer.tootsie.presentation.screens.profile.ProfileScreen
import com.pfitzer.tootsie.presentation.screens.search.SearchScreen
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
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Determine if we should show bottom navigation
    val showBottomBar = when {
        currentRoute == null -> false
        currentRoute.startsWith("home") -> true
        currentRoute.startsWith("search") -> true
        currentRoute.startsWith("notifications") -> true
        // Show bottom bar for own profile (route without accountId param)
        currentRoute == "profile" -> true
        else -> false
    }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                BottomNavigationBar(
                    currentRoute = currentRoute,
                    onNavigateToHome = {
                        navController.navigate(TootsieDestinations.Home.route) {
                            popUpTo(TootsieDestinations.Home.route) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onNavigateToSearch = {
                        navController.navigate(TootsieDestinations.Search.createRoute()) {
                            popUpTo(TootsieDestinations.Home.route) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onNavigateToNotifications = {
                        navController.navigate(TootsieDestinations.Notifications.route) {
                            popUpTo(TootsieDestinations.Home.route) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onNavigateToProfile = {
                        // Navigate to own profile - we'll use a special "me" identifier
                        navController.navigate("profile") {
                            popUpTo(TootsieDestinations.Home.route) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        },
        modifier = modifier
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(paddingValues)
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
            HomeScreen(
                onNavigateToCompose = {
                    navController.navigate(TootsieDestinations.Compose.createRoute())
                },
                onNavigateToReply = { statusId ->
                    navController.navigate(TootsieDestinations.Compose.createRoute(replyToId = statusId))
                },
                onNavigateToStatus = { statusId ->
                    navController.navigate(TootsieDestinations.StatusDetail.createRoute(statusId))
                },
                onNavigateToProfile = { accountId ->
                    navController.navigate(TootsieDestinations.Profile.createRoute(accountId))
                },
                onNavigateToSearch = { hashtagQuery ->
                    val route = TootsieDestinations.Search.createRoute(query = hashtagQuery)
                    Log.d("TootsieNavHost", "Navigating to search with query: '$hashtagQuery', route: '$route'")
                    navController.navigate(route)
                }
            )
        }

        // Compose screen - create new post or reply
        composable(
            route = TootsieDestinations.Compose.route,
            arguments = listOf(
                navArgument(TootsieDestinations.Compose.ARG_REPLY_TO_ID) {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
                navArgument(TootsieDestinations.Compose.ARG_DRAFT_ID) {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) {
            ComposeScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToDrafts = {
                    navController.navigate(TootsieDestinations.Drafts.route)
                }
            )
        }

        // Own profile screen (from bottom nav)
        composable(route = "profile") {
            ProfileScreen(
                onNavigateToProfile = { accountId ->
                    navController.navigate(TootsieDestinations.Profile.createRoute(accountId))
                },
                onNavigateToSearch = { hashtagQuery ->
                    navController.navigate(TootsieDestinations.Search.createRoute(query = hashtagQuery))
                }
            )
        }

        // Other user's profile screen (with account ID)
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

            ProfileScreen(
                accountId = accountId,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToProfile = { profileAccountId ->
                    navController.navigate(TootsieDestinations.Profile.createRoute(profileAccountId))
                },
                onNavigateToSearch = { hashtagQuery ->
                    navController.navigate(TootsieDestinations.Search.createRoute(query = hashtagQuery))
                }
            )
        }

        // Status detail screen
        composable(
            route = TootsieDestinations.StatusDetail.route,
            arguments = listOf(
                navArgument(TootsieDestinations.StatusDetail.ARG_STATUS_ID) {
                    type = NavType.StringType
                }
            )
        ) {
            com.pfitzer.tootsie.presentation.screens.statusdetail.StatusDetailScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToStatus = { statusId ->
                    navController.navigate(TootsieDestinations.StatusDetail.createRoute(statusId))
                },
                onNavigateToProfile = { accountId ->
                    navController.navigate(TootsieDestinations.Profile.createRoute(accountId))
                },
                onNavigateToCompose = { statusId ->
                    navController.navigate(TootsieDestinations.Compose.createRoute(replyToId = statusId))
                }
            )
        }

        // Search screen
        composable(
            route = TootsieDestinations.Search.route,
            arguments = listOf(
                navArgument(TootsieDestinations.Search.ARG_QUERY) {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val initialQuery = backStackEntry.arguments?.getString(
                TootsieDestinations.Search.ARG_QUERY
            )

            Log.d("TootsieNavHost", "Search composable invoked with initialQuery: '$initialQuery'")

            SearchScreen(
                initialQuery = initialQuery,
                onNavigateToProfile = { accountId ->
                    navController.navigate(TootsieDestinations.Profile.createRoute(accountId))
                },
                onNavigateToStatusDetail = { statusId ->
                    navController.navigate(TootsieDestinations.StatusDetail.createRoute(statusId))
                }
            )
        }

        // Notifications screen
        composable(route = TootsieDestinations.Notifications.route) {
            NotificationsScreen(
                onNavigateToStatus = { statusId ->
                    navController.navigate(TootsieDestinations.StatusDetail.createRoute(statusId))
                },
                onNavigateToProfile = { accountId ->
                    navController.navigate(TootsieDestinations.Profile.createRoute(accountId))
                }
            )
        }

        // Drafts screen
        composable(route = TootsieDestinations.Drafts.route) {
            DraftsScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onResumeDraft = { draftId ->
                    navController.navigate(TootsieDestinations.Compose.createRoute(draftId = draftId))
                }
            )
        }

        // Settings screen
        composable(route = TootsieDestinations.Settings.route) {
            // SettingsScreen will be implemented in Phase 3
        }
        }
    }
}
