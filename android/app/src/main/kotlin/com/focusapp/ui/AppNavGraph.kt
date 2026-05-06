package com.focusapp.ui

import androidx.compose.foundation.background
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
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
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
    const val ONBOARDING     = "onboarding"
    const val TIMER          = "timer"
    const val ANALYTICS      = "analytics"
    const val HISTORY        = "history"
    const val PROFILE        = "profile"
    const val SETTINGS       = "settings"
    const val SESSION_DETAIL = "session/{id}"
    fun sessionDetail(id: Long) = "session/$id"
}

private data class BottomNavItem(
    val label: String,
    val route: String,
    val icon: @Composable () -> Unit,
)

private val bottomNavItems = listOf(
    BottomNavItem("Timer",     Routes.TIMER,     { Icon(Icons.Default.Timer,   "Timer")     }),
    BottomNavItem("Analytics", Routes.ANALYTICS, { Icon(Icons.Default.BarChart, "Analytics") }),
    BottomNavItem("History",   Routes.HISTORY,   { Icon(Icons.Default.History,  "History")   }),
    BottomNavItem("Profile",   Routes.PROFILE,   { Icon(Icons.Default.Person,   "Profile")   }),
)

private val noBottomBarRoutes = setOf(Routes.ONBOARDING, Routes.SESSION_DETAIL, Routes.SETTINGS)

private val navBarContainerColor = Color(0xEB1A1A2E) // rgba(26,26,46,0.92)
private val navBarBorderColor    = Color(0x0FFFFFFF) // rgba(255,255,255,0.06)
private val navActiveColor       = Color(0xFF6C63FF)
private val navInactiveColor     = Color(0xFF9E9E9E)
private val navIndicatorColor    = Color(0x1A6C63FF)

@Composable
fun AppNavGraph(startupViewModel: StartupViewModel = hiltViewModel()) {
    val startDestination by startupViewModel.startDestination.collectAsState()

    if (startDestination == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0F0F1A)),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator()
        }
        return
    }

    val navController       = rememberNavController()
    val navBackStackEntry   by navController.currentBackStackEntryAsState()
    val currentDestination  = navBackStackEntry?.destination
    val currentRoute        = currentDestination?.route

    val showBottomBar = currentRoute !in noBottomBarRoutes &&
        !currentRoute.orEmpty().startsWith("session/")

    // Ambient gradient background — applied as the root surface
    val ambientBg = Brush.verticalGradient(
        colors = listOf(Color(0xFF0F0F1A), Color(0xFF08081A)),
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ambientBg),
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            bottomBar = {
                if (showBottomBar) {
                    NavigationBar(
                        containerColor = navBarContainerColor,
                        modifier = Modifier.drawBehind {
                            drawLine(
                                color       = navBarBorderColor,
                                start       = Offset(0f, 0f),
                                end         = Offset(size.width, 0f),
                                strokeWidth = 1.dp.toPx(),
                            )
                        },
                    ) {
                        bottomNavItems.forEach { item ->
                            val selected = currentDestination
                                ?.hierarchy
                                ?.any { it.route == item.route } == true
                            NavigationBarItem(
                                icon     = item.icon,
                                label    = { Text(item.label) },
                                selected = selected,
                                colors   = NavigationBarItemDefaults.colors(
                                    selectedIconColor   = navActiveColor,
                                    selectedTextColor   = navActiveColor,
                                    indicatorColor      = navIndicatorColor,
                                    unselectedIconColor = navInactiveColor,
                                    unselectedTextColor = navInactiveColor,
                                ),
                                onClick = {
                                    navController.navigate(item.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState    = true
                                    }
                                },
                            )
                        }
                    }
                }
            },
        ) { innerPadding ->
            NavHost(
                navController    = navController,
                startDestination = startDestination!!,
                modifier         = Modifier.padding(innerPadding),
            ) {
                composable(Routes.ONBOARDING) {
                    OnboardingScreen(
                        onFinished = {
                            navController.navigate(Routes.TIMER) {
                                popUpTo(Routes.ONBOARDING) { inclusive = true }
                            }
                        },
                    )
                }
                composable(Routes.TIMER) {
                    TimerScreen(
                        onSessionCompleted = { sessionId ->
                            navController.navigate(Routes.sessionDetail(sessionId))
                        },
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
                                restoreState    = true
                            }
                        },
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
                                restoreState    = true
                            }
                        },
                    )
                }
                composable(Routes.PROFILE) { ProfileScreen() }
                composable(Routes.SETTINGS) { Text("Settings") }
                composable(Routes.SESSION_DETAIL) { backStackEntry ->
                    val id = backStackEntry.arguments?.getString("id")?.toLongOrNull()
                        ?: return@composable
                    SessionDetailScreen(onBack = { navController.popBackStack() })
                }
            }
        }
    }
}
