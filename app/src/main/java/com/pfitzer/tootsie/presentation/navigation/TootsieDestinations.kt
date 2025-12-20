package com.pfitzer.tootsie.presentation.navigation

/**
 * Navigation destinations for Tootsie app.
 * Each destination represents a screen or feature in the app.
 */
sealed class TootsieDestinations(val route: String) {

    /**
     * Splash screen - checks auth status and routes to appropriate screen.
     */
    data object Splash : TootsieDestinations("splash")

    /**
     * Login screen - OAuth authentication flow.
     */
    data object Login : TootsieDestinations("login")

    /**
     * OAuth callback screen - handles the OAuth redirect.
     * Requires authorization code as parameter.
     */
    data object OAuthCallback : TootsieDestinations("oauth_callback/{code}") {
        fun createRoute(code: String): String = "oauth_callback/$code"
        const val ARG_CODE = "code"
    }

    /**
     * Home screen - main timeline and navigation hub.
     */
    data object Home : TootsieDestinations("home")

    /**
     * Compose screen - create new status/post.
     */
    data object Compose : TootsieDestinations("compose")

    /**
     * Profile screen - user profile.
     * Can view own profile or another user's profile.
     */
    data object Profile : TootsieDestinations("profile/{accountId}") {
        fun createRoute(accountId: String): String = "profile/$accountId"
        const val ARG_ACCOUNT_ID = "accountId"
    }

    /**
     * Status detail screen - view a single status with replies.
     */
    data object StatusDetail : TootsieDestinations("status/{statusId}") {
        fun createRoute(statusId: String): String = "status/$statusId"
        const val ARG_STATUS_ID = "statusId"
    }

    /**
     * Search screen - search for users, posts, and hashtags.
     */
    data object Search : TootsieDestinations("search")

    /**
     * Notifications screen - user notifications.
     */
    data object Notifications : TootsieDestinations("notifications")

    /**
     * Settings screen - app settings and preferences.
     */
    data object Settings : TootsieDestinations("settings")
}
