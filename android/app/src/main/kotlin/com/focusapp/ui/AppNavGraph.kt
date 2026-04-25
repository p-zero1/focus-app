package com.focusapp.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.focusapp.ui.analytics.AnalyticsScreen
import com.focusapp.ui.history.HistoryScreen
import com.focusapp.ui.history.SessionDetailScreen
import com.focusapp.ui.onboarding.OnboardingScreen
import com.focusapp.ui.profile.ProfileScreen
import com.focusapp.ui.timer.TimerScreen

// Route constants — single source of truth for navigation
object Routes {
    const val ONBOARDING = "onboarding"
    const val TIMER = "timer"
    const val ANALYTICS = "analytics"
    const val HISTORY = "history"
    const val PROFILE = "profile"
    const val SETTINGS = "settings"
    const val SESSION_DETAIL = "session/{id}"
    fun sessionDetail(id: Long) = "session/$id"
}

private data class BottomNavItem(
    val label: String,
    val route: String,
    val icon: @Composable () -> Unit,
)

private val bottomNavItems = listOf(
    BottomNavItem("Timer", Routes.TIMER, { Icon(Icons.Default.Timer, "Timer") }),
    BottomNavItem("Analytics", Routes.ANALYTICS, { Icon(Icons.Default.BarChart, "Analytics") }),
    BottomNavItem("History", Routes.HISTORY, { Icon(Icons.Default.History, "History") }),
    BottomNavItem("Profile", Routes.PROFILE, { Icon(Icons.Default.Person, "Profile") }),
)

// Screens where the bottom bar should NOT be shown
private val noBottomBarRoutes = setOf(Routes.ONBOARDING, Routes.SESSION_DETAIL, Routes.SETTINGS)

@Composable
fun AppNavGraph(startupViewModel: StartupViewModel = hiltViewModel()) {
    val startDestination by startupViewModel.startDestination.collectAsState()

    // Wait for DataStore to resolve the initial destination before rendering NavHost
    if (startDestination == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val currentRoute = currentDestination?.route

    val showBottomBar = currentRoute !in noBottomBarRoutes &&
        !currentRoute.orEmpty().startsWith("session/")

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomNavItems.forEach { item ->
                        NavigationBarItem(
                            icon = item.icon,
                            label = { Text(item.label) },
                            selected = currentDestination?.hierarchy?.any { it.route == item.route } == true,
                            onClick = {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
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
            composable(Routes.ONBOARDING) {
                OnboardingScreen(
                    onFinished = {
                        navController.navigate(Routes.TIMER) {
                            popUpTo(Routes.ONBOARDING) { inclusive = true }
                        }
                    }
                )
            }
            composable(Routes.TIMER) {
                TimerScreen(
                    onSessionCompleted = { sessionId ->
                        navController.navigate(Routes.sessionDetail(sessionId))
                    }
                )
            }
            composable(Routes.ANALYTICS) {
                AnalyticsScreen(
                    onNavigateToTimer = {
                        navController.navigate(Routes.TIMER) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
            composable(Routes.HISTORY) {
                HistoryScreen(
                    onSessionClick = { sessionId ->
                        navController.navigate(Routes.sessionDetail(sessionId))
                    },
                    onNavigateToTimer = {
                        navController.navigate(Routes.TIMER) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
            composable(Routes.PROFILE) {
                ProfileScreen()
            }
            composable(Routes.SETTINGS) {
                // Placeholder — implemented in Phase 8 (T078)
                Text("Settings")
            }
            composable(Routes.SESSION_DETAIL) { backStackEntry ->
                val id = backStackEntry.arguments?.getString("id")?.toLongOrNull() ?: return@composable
                SessionDetailScreen(
                    onBack = { navController.popBackStack() },
                )
            }
        }
    }
}
