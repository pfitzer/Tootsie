package com.pfitzer.tootsie.presentation.navigation

import com.pfitzer.tootsie.presentation.components.status.StatusTranslations
import com.pfitzer.tootsie.presentation.components.status.LocalStatusTranslations
import com.pfitzer.tootsie.presentation.TranslationViewModel
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navArgument
import com.pfitzer.tootsie.presentation.MainViewModel
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
import com.pfitzer.tootsie.presentation.screens.lists.ListsScreen
import com.pfitzer.tootsie.presentation.screens.lists.ManageListScreen
import com.pfitzer.tootsie.presentation.screens.filters.FiltersScreen
import com.pfitzer.tootsie.presentation.screens.followrequests.FollowRequestsScreen
import com.pfitzer.tootsie.presentation.screens.filters.edit.FilterEditScreen
import com.pfitzer.tootsie.presentation.screens.conversations.ConversationsScreen
import com.pfitzer.tootsie.presentation.screens.editprofile.EditProfileScreen
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.CompositionLocalProvider
import com.pfitzer.tootsie.domain.model.Status
import com.pfitzer.tootsie.presentation.components.status.LocalActiveAccountId
import com.pfitzer.tootsie.presentation.components.status.LocalOnDeleteStatus
import com.pfitzer.tootsie.presentation.OwnStatusActionsViewModel
import com.pfitzer.tootsie.presentation.OwnStatusEvent
import android.widget.Toast
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import com.pfitzer.tootsie.presentation.components.status.LocalOnEditStatus
import com.pfitzer.tootsie.presentation.components.status.LocalOnQuoteStatus
import com.pfitzer.tootsie.presentation.components.status.LocalOnShowEditHistory

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
    onAuthCodeConsumed: () -> Unit = {},
    mainViewModel: MainViewModel = hiltViewModel()
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val activeAccount by mainViewModel.activeAccount.collectAsState()

    // Translations of statuses in every card
    val translationViewModel: TranslationViewModel = hiltViewModel()
    val translationStates by translationViewModel.translations.collectAsState()
    val isServerTranslationAvailable by translationViewModel.isServerTranslationAvailable.collectAsState()

    // Deleting own statuses from any card; a redraft opens the compose screen
    val ownStatusActions: OwnStatusActionsViewModel = hiltViewModel()
    val context = LocalContext.current
    LaunchedEffect(ownStatusActions) {
        ownStatusActions.events.collect { event ->
            when (event) {
                is OwnStatusEvent.Redraft ->
                    navController.navigate(TootsieDestinations.Compose.createRoute(draftId = event.draftId))
                is OwnStatusEvent.Error ->
                    Toast.makeText(context, event.message.asString(context), Toast.LENGTH_SHORT).show()
            }
        }
    }

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
        // The screens' own Scaffolds and top bars handle the system bars; here only the
        // bottom navigation's height is reserved (otherwise the status bar is added twice)
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        modifier = modifier
    ) { paddingValues ->
        // Lets every status card offer "Quote" without passing a callback through each screen
        CompositionLocalProvider(
            LocalOnQuoteStatus provides { status: Status ->
                navController.navigate(TootsieDestinations.Compose.createRoute(quoteId = status.id))
            },
            LocalActiveAccountId provides activeAccount?.id,
            LocalOnEditStatus provides { status: Status ->
                navController.navigate(TootsieDestinations.Compose.createRoute(editId = status.id))
            },
            LocalOnShowEditHistory provides { status: Status ->
                navController.navigate(TootsieDestinations.EditHistory.createRoute(status.id))
            },
            LocalOnDeleteStatus provides { status: Status, redraft: Boolean ->
                if (redraft) ownStatusActions.deleteAndRedraft(status) else ownStatusActions.delete(status)
            },
            LocalStatusTranslations provides StatusTranslations(
                states = translationStates,
                isServerTranslationAvailable = isServerTranslationAvailable,
                onToggle = translationViewModel::toggle
            )
        ) {
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
                onNavigateToFilters = {
                    navController.navigate(TootsieDestinations.Filters.route)
                },
                onNavigateToTrending = {
                    navController.navigate(TootsieDestinations.Trending.route)
                },
                onNavigateToAnnouncements = {
                    navController.navigate(TootsieDestinations.Announcements.route)
                },
                onNavigateToConversations = {
                    navController.navigate(TootsieDestinations.Conversations.route)
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
                },
                navArgument(TootsieDestinations.Compose.ARG_QUOTE_ID) {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
                navArgument(TootsieDestinations.Compose.ARG_SCHEDULED_ID) {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
                navArgument(TootsieDestinations.Compose.ARG_EDIT_ID) {
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
                },
                onNavigateToScheduledPosts = {
                    navController.navigate(TootsieDestinations.ScheduledPosts.route)
                }
            )
        }

        // Own profile screen (from bottom nav)
        composable(route = "profile") {
            ProfileScreen(
                onNavigateToProfile = { accountId ->
                    navController.navigate(TootsieDestinations.Profile.createRoute(accountId))
                },
                onNavigateToHashtag = { hashtag ->
                    navController.navigate(TootsieDestinations.HashtagTimeline.createRoute(hashtag))
                },
                onNavigateToImageViewer = { url, description ->
                    navController.navigate(TootsieDestinations.ImageViewer.createRoute(url, description))
                },
                onNavigateToVideoPlayer = { url, description ->
                    navController.navigate(TootsieDestinations.VideoPlayer.createRoute(url, description))
                },
                onNavigateToEditProfile = {
                    navController.navigate(TootsieDestinations.EditProfile.route)
                },
                onNavigateToFollowList = { followAccountId, type ->
                    navController.navigate(TootsieDestinations.FollowList.createRoute(followAccountId, type))
                }
            )
        }

        // Followers / following list of an account
        composable(
            route = TootsieDestinations.FollowList.route,
            arguments = listOf(
                navArgument(TootsieDestinations.FollowList.ARG_ID) { type = NavType.StringType },
                navArgument(TootsieDestinations.FollowList.ARG_TYPE) { type = NavType.StringType }
            )
        ) {
            com.pfitzer.tootsie.presentation.screens.followlist.FollowListScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToProfile = { accountId ->
                    navController.navigate(TootsieDestinations.Profile.createRoute(accountId))
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
                onNavigateToHashtag = { hashtag ->
                    navController.navigate(TootsieDestinations.HashtagTimeline.createRoute(hashtag))
                },
                onNavigateToImageViewer = { url, description ->
                    navController.navigate(TootsieDestinations.ImageViewer.createRoute(url, description))
                },
                onNavigateToVideoPlayer = { url, description ->
                    navController.navigate(TootsieDestinations.VideoPlayer.createRoute(url, description))
                },
                onNavigateToEditProfile = {
                    navController.navigate(TootsieDestinations.EditProfile.route)
                },
                onNavigateToFollowList = { followAccountId, type ->
                    navController.navigate(TootsieDestinations.FollowList.createRoute(followAccountId, type))
                }
            )
        }

        // Edit profile screen
        composable(route = TootsieDestinations.EditProfile.route) {
            EditProfileScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        // Conversations screen
        composable(route = TootsieDestinations.Conversations.route) {
            ConversationsScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToStatus = { statusId ->
                    navController.navigate(TootsieDestinations.StatusDetail.createRoute(statusId))
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
                onNavigateToAccountList = { statusId, type ->
                    navController.navigate(TootsieDestinations.FollowList.createRoute(statusId, type))
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
                onNavigateToHashtag = { hashtag ->
                    navController.navigate(TootsieDestinations.HashtagTimeline.createRoute(hashtag))
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
                onNavigateToHashtag = { hashtag ->
                    navController.navigate(TootsieDestinations.HashtagTimeline.createRoute(hashtag))
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
                onNavigateToHashtag = { tag ->
                    navController.navigate(TootsieDestinations.HashtagTimeline.createRoute(tag))
                },
                onNavigateToReply = { statusId ->
                    navController.navigate(TootsieDestinations.Compose.createRoute(replyToId = statusId))
                }
            )
        }

        // Edit history of a status
        composable(
            route = TootsieDestinations.EditHistory.route,
            arguments = listOf(
                navArgument(TootsieDestinations.EditHistory.ARG_STATUS_ID) { type = NavType.StringType }
            )
        ) {
            com.pfitzer.tootsie.presentation.screens.edithistory.EditHistoryScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        // Scheduled posts screen
        composable(route = TootsieDestinations.ScheduledPosts.route) {
            com.pfitzer.tootsie.presentation.screens.scheduled.ScheduledPostsScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onEditScheduledPost = { scheduledId ->
                    navController.navigate(TootsieDestinations.Compose.createRoute(scheduledId = scheduledId))
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
            SettingsScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToLogin = {
                    navController.navigate(TootsieDestinations.Login.route)
                },
                onNavigateToPushSettings = {
                    navController.navigate(TootsieDestinations.PushSettings.route)
                },
                onNavigateToFollowRequests = {
                    navController.navigate(TootsieDestinations.FollowRequests.route)
                },
                onNavigateToMutedAccounts = {
                    navController.navigate(TootsieDestinations.MutedAccounts.route)
                },
                onNavigateToBlockedAccounts = {
                    navController.navigate(TootsieDestinations.BlockedAccounts.route)
                },
                onNavigateToBlockedDomains = {
                    navController.navigate(TootsieDestinations.BlockedDomains.route)
                },
                onNavigateToEditProfile = {
                    navController.navigate(TootsieDestinations.EditProfile.route)
                },
                onNavigateToFilters = {
                    navController.navigate(TootsieDestinations.Filters.route)
                }
            )
        }

        // Push Settings screen - configure push notifications
        composable(route = TootsieDestinations.PushSettings.route) {
            com.pfitzer.tootsie.presentation.screens.settings.push.PushSettingsScreen(
                onNavigateBack = {
                    navController.popBackStack()
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
                onNavigateToHashtag = { hashtag ->
                    navController.navigate(TootsieDestinations.HashtagTimeline.createRoute(hashtag))
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
                onNavigateToHashtag = { hashtag ->
                    navController.navigate(TootsieDestinations.HashtagTimeline.createRoute(hashtag))
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
            ListsScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToList = { listId, listName ->
                    navController.navigate(TootsieDestinations.ListTimeline.createRoute(listId, listName))
                },
                onNavigateToManageList = { listId, listName ->
                    navController.navigate(TootsieDestinations.ManageList.createRoute(listId, listName))
                }
            )
        }

        // Manage List screen - manage members of a specific list
        composable(
            route = TootsieDestinations.ManageList.route,
            arguments = listOf(
                navArgument(TootsieDestinations.ManageList.ARG_LIST_ID) {
                    type = NavType.StringType
                },
                navArgument(TootsieDestinations.ManageList.ARG_LIST_NAME) {
                    type = NavType.StringType
                    nullable = true
                }
            )
        ) {
            ManageListScreen(
                onNavigateBack = {
                    navController.popBackStack()
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

        // Filters screen - view and manage keyword filters
        composable(route = TootsieDestinations.Filters.route) {
            FiltersScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToFilter = { filterId ->
                    navController.navigate(TootsieDestinations.FilterEdit.createRoute(filterId))
                }
            )
        }

        // Follow Requests screen - manage pending follow requests
        composable(route = TootsieDestinations.FollowRequests.route) {
            FollowRequestsScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToProfile = { accountId ->
                    navController.navigate(TootsieDestinations.Profile.createRoute(accountId))
                }
            )
        }

        // Filter editor - create or edit a filter
        composable(
            route = TootsieDestinations.FilterEdit.route,
            arguments = listOf(
                navArgument(TootsieDestinations.FilterEdit.ARG_FILTER_ID) {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) {
            FilterEditScreen(
                onNavigateBack = {
                    navController.popBackStack()
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
                onNavigateToHashtag = { hashtag ->
                    navController.navigate(TootsieDestinations.HashtagTimeline.createRoute(hashtag))
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

        // Trending screen - view trending posts, hashtags and links
        composable(route = TootsieDestinations.Trending.route) {
            com.pfitzer.tootsie.presentation.screens.trending.TrendingScreen(
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
                onNavigateToHashtag = { query ->
                    // Remove the # if present
                    val hashtag = query.removePrefix("#")
                    navController.navigate(TootsieDestinations.HashtagTimeline.createRoute(hashtag))
                },
                onNavigateToImageViewer = { url, description ->
                    navController.navigate(TootsieDestinations.ImageViewer.createRoute(url, description))
                },
                onNavigateToVideoPlayer = { url, description ->
                    navController.navigate(TootsieDestinations.VideoPlayer.createRoute(url, description))
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

        // Announcements screen - view server announcements
        composable(route = TootsieDestinations.Announcements.route) {
            com.pfitzer.tootsie.presentation.screens.announcements.AnnouncementsScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToProfile = { accountId ->
                    navController.navigate(TootsieDestinations.Profile.createRoute(accountId))
                },
                onNavigateToHashtag = { hashtag ->
                    navController.navigate(TootsieDestinations.HashtagTimeline.createRoute(hashtag))
                }
            )
        }

        // Muted Accounts screen - view and manage muted accounts
        composable(route = TootsieDestinations.MutedAccounts.route) {
            com.pfitzer.tootsie.presentation.screens.mutedaccounts.MutedAccountsScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToProfile = { accountId ->
                    navController.navigate(TootsieDestinations.Profile.createRoute(accountId))
                }
            )
        }

        // Blocked Domains screen - view and manage blocked domains
        composable(route = TootsieDestinations.BlockedDomains.route) {
            com.pfitzer.tootsie.presentation.screens.blockeddomains.BlockedDomainsScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        // Blocked Accounts screen - view and manage blocked accounts
        composable(route = TootsieDestinations.BlockedAccounts.route) {
            com.pfitzer.tootsie.presentation.screens.blockedaccounts.BlockedAccountsScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToProfile = { accountId ->
                    navController.navigate(TootsieDestinations.Profile.createRoute(accountId))
                }
            )
        }
        }
        }
    }
}
