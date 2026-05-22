package com.android.security.samples.playintegrityapi.feature.bank.ui.navigation


import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import com.android.security.samples.playintegrityapi.feature.bank.ui.BankRoute

/**
 * Internal route for the Bank feature.
 * Kept internal so other modules are forced to use [navigateToBank].
 */
internal const val BANK_ROUTE = "bank_route"

/**
 * Navigates the user to the Bank micro-app.
 *
 * @param navOptions Optional [NavOptions] to configure the navigation backstack behavior
 * (e.g., popping previous screens or launching as a single top instance).
 */
fun NavController.navigateToBank(navOptions: NavOptions? = null) {
    this.navigate(BANK_ROUTE, navOptions)
}

/**
 * Registers the Bank feature into the main application navigation graph.
 *
 * @param onBackClick Callback triggered when the user presses the top app bar back arrow
 * or the system back button.
 */
fun NavGraphBuilder.bankScreen(
    onBackClick: () -> Unit
) {
    composable(route = BANK_ROUTE) {
        BankRoute(
            onBackClick = onBackClick
        )
    }
}