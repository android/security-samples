// Copyright 2026 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     https://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

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