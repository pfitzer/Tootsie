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
     * Optionally takes replyToId parameter for replying to a status.
     * Optionally takes draftId parameter for resuming a draft.
     */
    data object Compose : TootsieDestinations("compose?replyToId={replyToId}&draftId={draftId}") {
        fun createRoute(replyToId: String? = null, draftId: Long? = null): String {
            val params = buildList {
                if (replyToId != null) add("replyToId=$replyToId")
                if (draftId != null) add("draftId=$draftId")
            }
            return if (params.isEmpty()) {
                "compose"
            } else {
                "compose?${params.joinToString("&")}"
            }
        }
        const val ARG_REPLY_TO_ID = "replyToId"
        const val ARG_DRAFT_ID = "draftId"
    }

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
     * Optionally takes initialQuery parameter to pre-fill search.
     */
    data object Search : TootsieDestinations("search?query={query}") {
        fun createRoute(query: String? = null): String {
            return if (query != null) {
                "search?query=$query"
            } else {
                "search"
            }
        }
        const val ARG_QUERY = "query"
    }

    /**
     * Notifications screen - user notifications.
     */
    data object Notifications : TootsieDestinations("notifications")

    /**
     * Drafts screen - view and manage saved drafts.
     */
    data object Drafts : TootsieDestinations("drafts")

    /**
     * Settings screen - app settings and preferences.
     */
    data object Settings : TootsieDestinations("settings")
}
