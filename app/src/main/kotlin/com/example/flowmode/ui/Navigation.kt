package com.example.flowmode.ui

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.flowmode.ui.auth.AuthScreen
import com.example.flowmode.ui.auth.AuthViewModel
import com.example.flowmode.ui.canvas.CanvasViewModel
import com.example.flowmode.ui.canvas.FlowCanvas
import com.example.flowmode.ui.marketplace.MarketplaceScreen
import com.example.flowmode.ui.marketplace.MarketplaceViewModel

sealed class Screen(val route: String) {
    object Auth : Screen("auth")
    object FlowList : Screen("flow_list")
    object FlowEditor : Screen("flow_editor")
    object Marketplace : Screen("marketplace")
    object Settings : Screen("settings")
}

@Composable
fun FlowNavigation(
    navController: NavHostController,
    authViewModel: AuthViewModel,
    canvasViewModel: CanvasViewModel,
    marketplaceViewModel: MarketplaceViewModel
) {
    val startDestination = if (authViewModel.user.value == null) Screen.Auth.route else Screen.FlowList.route

    NavHost(navController = navController, startDestination = startDestination) {
        composable(Screen.Auth.route) {
            AuthScreen(authViewModel)
        }
        composable(Screen.FlowList.route) {
            FlowListScreen()
        }
        composable(Screen.FlowEditor.route) {
            FlowCanvas(canvasViewModel)
        }
        composable(Screen.Marketplace.route) {
            MarketplaceScreen(marketplaceViewModel)
        }
        composable(Screen.Settings.route) {
            SettingsScreen(authViewModel)
        }
    }
}
