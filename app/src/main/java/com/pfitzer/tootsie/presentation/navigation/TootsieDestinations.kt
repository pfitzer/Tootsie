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
    data object Compose : TootsieDestinations("compose?replyToId={replyToId}&draftId={draftId}&quoteId={quoteId}") {
        fun createRoute(replyToId: String? = null, draftId: Long? = null, quoteId: String? = null): String {
            val params = buildList {
                if (replyToId != null) add("replyToId=$replyToId")
                if (draftId != null) add("draftId=$draftId")
                if (quoteId != null) add("quoteId=$quoteId")
            }
            return if (params.isEmpty()) {
                "compose"
            } else {
                "compose?${params.joinToString("&")}"
            }
        }
        const val ARG_REPLY_TO_ID = "replyToId"
        const val ARG_DRAFT_ID = "draftId"
        const val ARG_QUOTE_ID = "quoteId"
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
                // URL encode the query to handle special characters like #
                val encodedQuery = java.net.URLEncoder.encode(query, "UTF-8")
                "search?query=$encodedQuery"
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

    /**
     * Bookmarks screen - view bookmarked statuses.
     */
    data object Bookmarks : TootsieDestinations("bookmarks")

    /**
     * Favorites screen - view favorited statuses.
     */
    data object Favorites : TootsieDestinations("favorites")

    /**
     * Lists screen - view and manage user's lists.
     */
    data object Lists : TootsieDestinations("lists")

    /**
     * Manage List screen - manage members of a specific list.
     * Requires listId and listName parameters.
     */
    data object ManageList : TootsieDestinations("manage_list/{listId}?listName={listName}") {
        fun createRoute(listId: String, listName: String): String {
            val encodedName = java.net.URLEncoder.encode(listName, "UTF-8")
            return "manage_list/$listId?listName=$encodedName"
        }
        const val ARG_LIST_ID = "listId"
        const val ARG_LIST_NAME = "listName"
    }

    /**
     * Followed Tags screen - view and manage followed hashtags.
     */
    data object FollowedTags : TootsieDestinations("followed_tags")

    /**
     * Filters screen - view and manage keyword filters.
     */
    data object Filters : TootsieDestinations("filters")

    /**
     * Follow Requests screen - manage pending follow requests.
     */
    data object FollowRequests : TootsieDestinations("follow_requests")

    /**
     * Filter editor - create a filter (without filterId) or edit one.
     */
    data object FilterEdit : TootsieDestinations("filters/edit?filterId={filterId}") {
        fun createRoute(filterId: String?): String =
            if (filterId == null) "filters/edit" else "filters/edit?filterId=$filterId"
        const val ARG_FILTER_ID = "filterId"
    }

    /**
     * List Timeline screen - view statuses from a specific list.
     * Requires listId and listName parameters.
     */
    data object ListTimeline : TootsieDestinations("list_timeline/{listId}?listName={listName}") {
        fun createRoute(listId: String, listName: String): String {
            val encodedName = java.net.URLEncoder.encode(listName, "UTF-8")
            return "list_timeline/$listId?listName=$encodedName"
        }
        const val ARG_LIST_ID = "listId"
        const val ARG_LIST_NAME = "listName"
    }

    /**
     * Hashtag Timeline screen - view statuses containing a specific hashtag.
     * Requires hashtag parameter.
     */
    data object HashtagTimeline : TootsieDestinations("hashtag_timeline/{hashtag}") {
        fun createRoute(hashtag: String): String {
            val encodedHashtag = java.net.URLEncoder.encode(hashtag, "UTF-8")
            return "hashtag_timeline/$encodedHashtag"
        }
        const val ARG_HASHTAG = "hashtag"
    }

    /**
     * Trending screen - view trending posts, hashtags and links.
     */
    data object Trending : TootsieDestinations("trending")

    /**
     * Image Viewer screen - view image in full screen with zoom.
     * Requires imageUrl parameter and optional description.
     */
    data object ImageViewer : TootsieDestinations("image_viewer?url={url}&description={description}") {
        fun createRoute(url: String, description: String? = null): String {
            val encodedUrl = java.net.URLEncoder.encode(url, "UTF-8")
            val encodedDesc = description?.let { java.net.URLEncoder.encode(it, "UTF-8") } ?: ""
            return if (description != null) {
                "image_viewer?url=$encodedUrl&description=$encodedDesc"
            } else {
                "image_viewer?url=$encodedUrl"
            }
        }
        const val ARG_URL = "url"
        const val ARG_DESCRIPTION = "description"
    }

    /**
     * Video Player screen - play video in full screen with controls.
     * Requires videoUrl parameter and optional description.
     */
    data object VideoPlayer : TootsieDestinations("video_player?url={url}&description={description}") {
        fun createRoute(url: String, description: String? = null): String {
            val encodedUrl = java.net.URLEncoder.encode(url, "UTF-8")
            val encodedDesc = description?.let { java.net.URLEncoder.encode(it, "UTF-8") } ?: ""
            return if (description != null) {
                "video_player?url=$encodedUrl&description=$encodedDesc"
            } else {
                "video_player?url=$encodedUrl"
            }
        }
        const val ARG_URL = "url"
        const val ARG_DESCRIPTION = "description"
    }

    /**
     * Push Settings screen - configure push notification preferences.
     */
    data object PushSettings : TootsieDestinations("push_settings")

    data object Conversations : TootsieDestinations("conversations")

    /**
     * Edit Profile screen - edit authenticated user's profile.
     */
    data object EditProfile : TootsieDestinations("edit_profile")

    /**
     * Announcements screen - view server announcements.
     */
    data object Announcements : TootsieDestinations("announcements")

    /**
     * Muted Accounts screen - view and manage muted accounts.
     */
    data object MutedAccounts : TootsieDestinations("muted_accounts")

    /**
     * Blocked Accounts screen - view and manage blocked accounts.
     */
    data object BlockedAccounts : TootsieDestinations("blocked_accounts")
}
