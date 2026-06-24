package com.company.warehousevio.ui

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.company.warehousevio.ui.screen.MonitorScreen
import com.company.warehousevio.ui.screen.RoleSelectionScreen
import com.company.warehousevio.ui.screen.SessionHistoryScreen
import com.company.warehousevio.ui.screen.TrackerScreen

object Routes {
    const val ROLE_SELECTION = "role_selection"
    const val TRACKER = "tracker"
    const val MONITOR = "monitor"
    const val HISTORY = "history"
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Routes.ROLE_SELECTION) {
        composable(Routes.ROLE_SELECTION) {
            RoleSelectionScreen(
                onTrackerSelected = { navController.navigate(Routes.TRACKER) },
                onMonitorSelected = { navController.navigate(Routes.MONITOR) },
            )
        }
        composable(Routes.TRACKER) {
            TrackerScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.MONITOR) {
            MonitorScreen(
                onBack = { navController.popBackStack() },
                onHistory = { navController.navigate(Routes.HISTORY) },
            )
        }
        composable(Routes.HISTORY) {
            SessionHistoryScreen(onBack = { navController.popBackStack() })
        }
    }
}
