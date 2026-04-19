package com.axionch.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.axionch.app.domain.model.NavRoute
import com.axionch.app.ui.screens.accounts.AccountsScreen
import com.axionch.app.ui.screens.composer.ComposerScreen
import com.axionch.app.ui.screens.capture.CaptureStudioScreen
import com.axionch.app.ui.screens.dashboard.DashboardScreen
import com.axionch.app.ui.screens.dryrun.DryRunHistoryScreen
import com.axionch.app.ui.screens.results.ResultsScreen
import com.axionch.app.ui.screens.video.VideoFiltersScreen
import com.axionch.app.ui.screens.vault.VaultScreen

@Composable
fun AxionNavHost(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = NavRoute.Dashboard.route
    ) {
        composable(NavRoute.Dashboard.route) {
            DashboardScreen(navController)
        }
        composable(NavRoute.Accounts.route) {
            AccountsScreen(navController)
        }
        composable(NavRoute.Composer.route) {
            ComposerScreen(navController)
        }
        composable(NavRoute.Results.route) {
            ResultsScreen(navController)
        }
        composable(NavRoute.DryRunHistory.route) {
            DryRunHistoryScreen(navController)
        }
        composable(NavRoute.Vault.route) {
            VaultScreen(navController)
        }
        composable(NavRoute.VideoFilters.route) {
            VideoFiltersScreen(navController)
        }
        composable(NavRoute.CaptureStudio.route) {
            CaptureStudioScreen(navController)
        }
    }
}
