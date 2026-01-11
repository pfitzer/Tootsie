package com.pfitzer.tootsie.presentation.navigation

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
import com.pfitzer.tootsie.presentation.screens.settings.SettingsScreen
import com.pfitzer.tootsie.presentation.screens.splash.SplashScreen
import com.pfitzer.tootsie.presentation.screens.bookmarks.BookmarksScreen
import com.pfitzer.tootsie.presentation.screens.favorites.FavoritesScreen

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
                onNavigateToHashtag = { hashtag ->
                    navController.navigate(TootsieDestinations.HashtagTimeline.createRoute(hashtag))
                },
                onNavigateToSettings = {
                    navController.navigate(TootsieDestinations.Settings.route)
                },
                onNavigateToBookmarks = {
                    navController.navigate(TootsieDestinations.Bookmarks.route)
                },
                onNavigateToFavorites = {
                    navController.navigate(TootsieDestinations.Favorites.route)
                },
                onNavigateToLists = {
                    navController.navigate(TootsieDestinations.Lists.route)
                },
                onNavigateToFollowedTags = {
                    navController.navigate(TootsieDestinations.FollowedTags.route)
                },
                onNavigateToTrendingHashtags = {
                    navController.navigate(TootsieDestinations.TrendingHashtags.route)
                },
                onNavigateToTrendingPosts = {
                    navController.navigate(TootsieDestinations.TrendingPosts.route)
                },
                onNavigateToTrendingLinks = {
                    navController.navigate(TootsieDestinations.TrendingLinks.route)
                },
                onNavigateToImageViewer = { url, description ->
                    navController.navigate(TootsieDestinations.ImageViewer.createRoute(url, description))
                },
                onNavigateToVideoPlayer = { url, description ->
                    navController.navigate(TootsieDestinations.VideoPlayer.createRoute(url, description))
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
                },
                onNavigateToImageViewer = { url, description ->
                    navController.navigate(TootsieDestinations.ImageViewer.createRoute(url, description))
                },
                onNavigateToVideoPlayer = { url, description ->
                    navController.navigate(TootsieDestinations.VideoPlayer.createRoute(url, description))
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
                },
                onNavigateToImageViewer = { url, description ->
                    navController.navigate(TootsieDestinations.ImageViewer.createRoute(url, description))
                },
                onNavigateToVideoPlayer = { url, description ->
                    navController.navigate(TootsieDestinations.VideoPlayer.createRoute(url, description))
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
                },
                onNavigateToSearch = { hashtagQuery ->
                    navController.navigate(TootsieDestinations.Search.createRoute(query = hashtagQuery))
                },
                onNavigateToImageViewer = { url, description ->
                    navController.navigate(TootsieDestinations.ImageViewer.createRoute(url, description))
                },
                onNavigateToVideoPlayer = { url, description ->
                    navController.navigate(TootsieDestinations.VideoPlayer.createRoute(url, description))
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

            SearchScreen(
                initialQuery = initialQuery,
                onNavigateToProfile = { accountId ->
                    navController.navigate(TootsieDestinations.Profile.createRoute(accountId))
                },
                onNavigateToStatusDetail = { statusId ->
                    navController.navigate(TootsieDestinations.StatusDetail.createRoute(statusId))
                },
                onNavigateToImageViewer = { url, description ->
                    navController.navigate(TootsieDestinations.ImageViewer.createRoute(url, description))
                },
                onNavigateToVideoPlayer = { url, description ->
                    navController.navigate(TootsieDestinations.VideoPlayer.createRoute(url, description))
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
                },
                onNavigateToSearch = { tag ->
                    navController.navigate(TootsieDestinations.Search.createRoute("#$tag"))
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

        // Settings screen - account management
        composable(route = TootsieDestinations.Settings.route) {
            SettingsScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToLogin = {
                    navController.navigate(TootsieDestinations.Login.route)
                }
            )
        }

        // Bookmarks screen - view bookmarked statuses
        composable(route = TootsieDestinations.Bookmarks.route) {
            BookmarksScreen(
                onNavigateBack = {
                    navController.popBackStack()
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
                onNavigateToSearch = { query ->
                    navController.navigate(TootsieDestinations.Search.createRoute(query))
                },
                onNavigateToImageViewer = { url, description ->
                    navController.navigate(TootsieDestinations.ImageViewer.createRoute(url, description))
                },
                onNavigateToVideoPlayer = { url, description ->
                    navController.navigate(TootsieDestinations.VideoPlayer.createRoute(url, description))
                }
            )
        }

        // Favorites screen - view favorited statuses
        composable(route = TootsieDestinations.Favorites.route) {
            FavoritesScreen(
                onNavigateBack = {
                    navController.popBackStack()
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
                onNavigateToSearch = { query ->
                    navController.navigate(TootsieDestinations.Search.createRoute(query))
                },
                onNavigateToImageViewer = { url, description ->
                    navController.navigate(TootsieDestinations.ImageViewer.createRoute(url, description))
                },
                onNavigateToVideoPlayer = { url, description ->
                    navController.navigate(TootsieDestinations.VideoPlayer.createRoute(url, description))
                }
            )
        }

        // Lists screen - view and manage lists
        composable(route = TootsieDestinations.Lists.route) {
            com.pfitzer.tootsie.presentation.screens.lists.ListsScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToList = { listId, listName ->
                    navController.navigate(TootsieDestinations.ListTimeline.createRoute(listId, listName))
                }
            )
        }

        // Followed Tags screen - view and manage followed hashtags
        composable(route = TootsieDestinations.FollowedTags.route) {
            com.pfitzer.tootsie.presentation.screens.followedtags.FollowedTagsScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToHashtag = { hashtag ->
                    navController.navigate(TootsieDestinations.HashtagTimeline.createRoute(hashtag))
                }
            )
        }

        // List Timeline screen - view statuses from a specific list
        composable(
            route = TootsieDestinations.ListTimeline.route,
            arguments = listOf(
                navArgument(TootsieDestinations.ListTimeline.ARG_LIST_ID) {
                    type = NavType.StringType
                },
                navArgument(TootsieDestinations.ListTimeline.ARG_LIST_NAME) {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = "List"
                }
            )
        ) { backStackEntry ->
            val listName = backStackEntry.arguments?.getString(TootsieDestinations.ListTimeline.ARG_LIST_NAME) ?: "List"
            com.pfitzer.tootsie.presentation.screens.listtimeline.ListTimelineScreen(
                listName = listName,
                onNavigateBack = {
                    navController.popBackStack()
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
                onNavigateToSearch = { query ->
                    navController.navigate(TootsieDestinations.Search.createRoute(query))
                },
                onNavigateToImageViewer = { url, description ->
                    navController.navigate(TootsieDestinations.ImageViewer.createRoute(url, description))
                },
                onNavigateToVideoPlayer = { url, description ->
                    navController.navigate(TootsieDestinations.VideoPlayer.createRoute(url, description))
                }
            )
        }

        // Hashtag Timeline screen - view statuses containing a specific hashtag
        composable(
            route = TootsieDestinations.HashtagTimeline.route,
            arguments = listOf(
                navArgument(TootsieDestinations.HashtagTimeline.ARG_HASHTAG) {
                    type = NavType.StringType
                }
            )
        ) { backStackEntry ->
            val hashtag = backStackEntry.arguments?.getString(TootsieDestinations.HashtagTimeline.ARG_HASHTAG) ?: ""
            com.pfitzer.tootsie.presentation.screens.hashtagtimeline.HashtagTimelineScreen(
                hashtag = hashtag,
                onNavigateBack = {
                    navController.popBackStack()
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
                onNavigateToHashtag = { newHashtag ->
                    navController.navigate(TootsieDestinations.HashtagTimeline.createRoute(newHashtag))
                },
                onNavigateToImageViewer = { url, description ->
                    navController.navigate(TootsieDestinations.ImageViewer.createRoute(url, description))
                },
                onNavigateToVideoPlayer = { url, description ->
                    navController.navigate(TootsieDestinations.VideoPlayer.createRoute(url, description))
                }
            )
        }

        // Trending Hashtags screen - view trending hashtags
        composable(route = TootsieDestinations.TrendingHashtags.route) {
            com.pfitzer.tootsie.presentation.screens.trending.TrendingHashtagsScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToSearch = { query ->
                    // Remove the # if present
                    val hashtag = query.removePrefix("#")
                    navController.navigate(TootsieDestinations.HashtagTimeline.createRoute(hashtag))
                }
            )
        }

        // Trending Posts screen - view trending posts/statuses
        composable(route = TootsieDestinations.TrendingPosts.route) {
            com.pfitzer.tootsie.presentation.screens.trendingposts.TrendingPostsScreen(
                onNavigateBack = {
                    navController.popBackStack()
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
                onNavigateToSearch = { query ->
                    navController.navigate(TootsieDestinations.Search.createRoute(query))
                },
                onNavigateToImageViewer = { url, description ->
                    navController.navigate(TootsieDestinations.ImageViewer.createRoute(url, description))
                },
                onNavigateToVideoPlayer = { url, description ->
                    navController.navigate(TootsieDestinations.VideoPlayer.createRoute(url, description))
                }
            )
        }

        // Trending Links screen - view trending links/articles
        composable(route = TootsieDestinations.TrendingLinks.route) {
            com.pfitzer.tootsie.presentation.screens.trendinglinks.TrendingLinksScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        // Image Viewer screen - full screen image viewer with zoom
        composable(
            route = TootsieDestinations.ImageViewer.route,
            arguments = listOf(
                navArgument(TootsieDestinations.ImageViewer.ARG_URL) {
                    type = NavType.StringType
                },
                navArgument(TootsieDestinations.ImageViewer.ARG_DESCRIPTION) {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val url = backStackEntry.arguments?.getString(TootsieDestinations.ImageViewer.ARG_URL)
                ?.let { java.net.URLDecoder.decode(it, "UTF-8") } ?: return@composable
            val description = backStackEntry.arguments?.getString(TootsieDestinations.ImageViewer.ARG_DESCRIPTION)
                ?.let { java.net.URLDecoder.decode(it, "UTF-8") }

            com.pfitzer.tootsie.presentation.screens.imageviewer.ImageViewerScreen(
                imageUrl = url,
                description = description,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        // Video Player screen - full screen video player with controls
        composable(
            route = TootsieDestinations.VideoPlayer.route,
            arguments = listOf(
                navArgument(TootsieDestinations.VideoPlayer.ARG_URL) {
                    type = NavType.StringType
                },
                navArgument(TootsieDestinations.VideoPlayer.ARG_DESCRIPTION) {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val url = backStackEntry.arguments?.getString(TootsieDestinations.VideoPlayer.ARG_URL)
                ?.let { java.net.URLDecoder.decode(it, "UTF-8") } ?: return@composable
            val description = backStackEntry.arguments?.getString(TootsieDestinations.VideoPlayer.ARG_DESCRIPTION)
                ?.let { java.net.URLDecoder.decode(it, "UTF-8") }

            com.pfitzer.tootsie.presentation.screens.videoplayer.VideoPlayerScreen(
                videoUrl = url,
                description = description,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
        }
    }
}
