package com.android.security.samples.playintegrityapi.feature.streaming.ui.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import com.android.security.samples.playintegrityapi.feature.streaming.ui.StreamingRoute

/**
 * Internal route for the Streaming feature.
 * Kept internal so other modules are forced to use [navigateToStreaming].
 */
internal const val STREAMING_ROUTE = "streaming_route"

/**
 * Navigates the user to the Streaming micro-app.
 *
 * @param navOptions Optional [NavOptions] to configure the navigation backstack behavior.
 */
fun NavController.navigateToStreaming(navOptions: NavOptions? = null) {
    this.navigate(STREAMING_ROUTE, navOptions)
}

/**
 * Registers the Streaming feature into the main application navigation graph.
 *
 * @param onBackClick Callback triggered when the user presses the top app bar back arrow
 * or the system back button.
 */
fun NavGraphBuilder.streamingScreen(
    onBackClick: () -> Unit
) {
    composable(route = STREAMING_ROUTE) {
        StreamingRoute(
            onBackClick = onBackClick
        )
    }
}