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