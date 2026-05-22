package com.android.security.samples.playintegrityapi.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import com.android.security.samples.playintegrityapi.ui.HomeRoute

/**
 * Route for the Home screen.
 */
const val HOME_ROUTE = "home_route"

/**
 * Navigates the user to the Home screen.
 */
fun NavController.navigateToHome(navOptions: NavOptions? = null) {
    this.navigate(HOME_ROUTE, navOptions)
}

/**
 * Registers the Home screen into the main application navigation graph.
 */
fun NavGraphBuilder.homeScreen(
    onNavigateToBank: () -> Unit,
    onNavigateToStreaming: () -> Unit,
    onNavigateToGame: () -> Unit
) {
    composable(route = HOME_ROUTE) {
        HomeRoute(
            onNavigateToBank = onNavigateToBank,
            onNavigateToStreaming = onNavigateToStreaming,
            onNavigateToGame = onNavigateToGame
        )
    }
}