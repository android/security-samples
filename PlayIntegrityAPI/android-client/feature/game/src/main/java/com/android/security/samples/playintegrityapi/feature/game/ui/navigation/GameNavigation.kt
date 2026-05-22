package com.android.security.samples.playintegrityapi.feature.game.ui.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import com.android.security.samples.playintegrityapi.feature.game.ui.GameRoute

/**
 * Internal route for the Game feature.
 * Kept internal so other modules are forced to use [navigateToGame].
 */
internal const val GAME_ROUTE = "game_route"

/**
 * Navigates the user to the Game micro-app.
 *
 * @param navOptions Optional [NavOptions] to configure the navigation backstack behavior
 * (e.g., popping previous screens or launching as a single top instance).
 */
fun NavController.navigateToGame(navOptions: NavOptions? = null) {
    this.navigate(GAME_ROUTE, navOptions)
}

/**
 * Registers the Game feature into the main application navigation graph.
 *
 * @param onBackClick Callback triggered when the user presses the top app bar back arrow
 * or the system back button.
 */
fun NavGraphBuilder.gameScreen(
    onBackClick: () -> Unit
) {
    composable(route = GAME_ROUTE) {
        GameRoute(
            onBackClick = onBackClick
        )
    }
}