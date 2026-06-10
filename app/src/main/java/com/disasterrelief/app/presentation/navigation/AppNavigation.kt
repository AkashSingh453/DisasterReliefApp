package com.disasterrelief.app.presentation.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Sos
import androidx.compose.material.icons.outlined.Chat
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Sos
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import android.content.Context
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.disasterrelief.app.presentation.chat.ChatScreen
import com.disasterrelief.app.presentation.dashboard.DashboardScreen
import com.disasterrelief.app.presentation.map.MapScreen
import com.disasterrelief.app.presentation.onboarding.OnboardingScreen
import com.disasterrelief.app.presentation.sos.SOSScreen
import kotlinx.serialization.Serializable

// ══════════════════════════════════════════════════════════════════════
//  TYPE-SAFE NAVIGATION ROUTES
// ══════════════════════════════════════════════════════════════════════

@Serializable
data object OnboardingRoute

@Serializable
data object DashboardRoute

@Serializable
data object SOSRoute

@Serializable
data object ChatRoute

@Serializable
data object MapRoute

@Serializable
data object AiAnalysisRoute

@Serializable
data class DirectChatRoute(val peerId: String, val peerName: String)

// ══════════════════════════════════════════════════════════════════════
//  BOTTOM NAVIGATION ITEMS
// ══════════════════════════════════════════════════════════════════════

data class BottomNavItem<T : Any>(
    val label: String,
    val route: T,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

val bottomNavItems = listOf(
    BottomNavItem(
        label = "Dashboard",
        route = DashboardRoute,
        selectedIcon = Icons.Filled.Dashboard,
        unselectedIcon = Icons.Outlined.Dashboard
    ),
    BottomNavItem(
        label = "SOS",
        route = SOSRoute,
        selectedIcon = Icons.Filled.Sos,
        unselectedIcon = Icons.Outlined.Sos
    ),
    BottomNavItem(
        label = "Chat",
        route = ChatRoute,
        selectedIcon = Icons.Filled.Chat,
        unselectedIcon = Icons.Outlined.Chat
    ),
    BottomNavItem(
        label = "Map",
        route = MapRoute,
        selectedIcon = Icons.Filled.Map,
        unselectedIcon = Icons.Outlined.Map
    )
)

// ══════════════════════════════════════════════════════════════════════
//  APP NAVIGATION
// ══════════════════════════════════════════════════════════════════════

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val context = LocalContext.current

    var startDestination by remember { mutableStateOf<Any?>(null) }

    LaunchedEffect(Unit) {
        val prefs = context.getSharedPreferences("disaster_relief_prefs", Context.MODE_PRIVATE)
        val name = prefs.getString("node_name", "") ?: ""
        if (name.isBlank()) {
            startDestination = OnboardingRoute
        } else {
            startDestination = DashboardRoute
        }
    }

    if (startDestination == null) {
        return // Loading state
    }

    val isOnboarding = currentDestination?.hasRoute(OnboardingRoute::class) == true

    Scaffold(
        bottomBar = {
            if (!isOnboarding) {
                NavigationBar {
                bottomNavItems.forEach { item ->
                    val selected = currentDestination?.hierarchy?.any {
                        it.hasRoute(item.route::class)
                    } == true

                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            navController.navigate(item.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                contentDescription = item.label
                            )
                        },
                        label = { Text(item.label) }
                    )
                }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startDestination!!,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable<OnboardingRoute> {
                OnboardingScreen(
                    onComplete = {
                        navController.navigate(DashboardRoute) {
                            popUpTo(OnboardingRoute) { inclusive = true }
                        }
                    }
                )
            }
            composable<DashboardRoute> {
                DashboardScreen(
                    onNavigateToSOS = {
                        navController.navigate(SOSRoute)
                    },
                    onNavigateToAiAnalysis = {
                        navController.navigate(AiAnalysisRoute)
                    }
                )
            }
            composable<SOSRoute> {
                SOSScreen()
            }
            composable<ChatRoute> {
                ChatScreen(
                    onNavigateToDirectChat = { peerId, peerName ->
                        navController.navigate(DirectChatRoute(peerId, peerName))
                    }
                )
            }
            composable<MapRoute> {
                MapScreen(
                    onNavigateToDirectChat = { peerId, peerName ->
                        navController.navigate(DirectChatRoute(peerId, peerName))
                    }
                )
            }
            composable<DirectChatRoute> {
                com.disasterrelief.app.presentation.chat.DirectChatScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable<AiAnalysisRoute> {
                com.disasterrelief.app.presentation.ai.AiAnalysisScreen()
            }
        }
    }
}
