package com.pfitzer.tootsie.presentation.components.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import com.pfitzer.tootsie.R

/**
 * Bottom navigation bar for main app screens.
 * Displays navigation items for Home, Search, Notifications, and Profile.
 */
@Composable
fun BottomNavigationBar(
    currentRoute: String?,
    onNavigateToHome: () -> Unit,
    onNavigateToSearch: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    onNavigateToProfile: () -> Unit
) {
    NavigationBar {
        BottomNavItem.entries.forEach { item ->
            val isSelected = currentRoute?.startsWith(item.route) == true

            NavigationBarItem(
                icon = {
                    Icon(
                        imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                        contentDescription = stringResource(item.labelRes)
                    )
                },
                selected = isSelected,
                onClick = {
                    when (item) {
                        BottomNavItem.HOME -> onNavigateToHome()
                        BottomNavItem.SEARCH -> onNavigateToSearch()
                        BottomNavItem.NOTIFICATIONS -> onNavigateToNotifications()
                        BottomNavItem.PROFILE -> onNavigateToProfile()
                    }
                }
            )
        }
    }
}

/**
 * Bottom navigation items.
 */
enum class BottomNavItem(
    val route: String,
    @StringRes val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    HOME(
        route = "home",
        labelRes = R.string.home_timeline,
        selectedIcon = Icons.Filled.Home,
        unselectedIcon = Icons.Outlined.Home
    ),
    SEARCH(
        route = "search",
        labelRes = R.string.search,
        selectedIcon = Icons.Filled.Search,
        unselectedIcon = Icons.Outlined.Search
    ),
    NOTIFICATIONS(
        route = "notifications",
        labelRes = R.string.notifications,
        selectedIcon = Icons.Filled.Notifications,
        unselectedIcon = Icons.Outlined.Notifications
    ),
    PROFILE(
        route = "profile",
        labelRes = R.string.profile,
        selectedIcon = Icons.Filled.Person,
        unselectedIcon = Icons.Outlined.Person
    )
}
