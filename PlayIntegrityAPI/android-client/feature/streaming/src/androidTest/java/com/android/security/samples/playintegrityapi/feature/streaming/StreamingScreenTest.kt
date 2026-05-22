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

package com.android.security.samples.playintegrityapi.feature.streaming

import androidx.activity.ComponentActivity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.android.security.samples.playintegrityapi.core.ui.theme.PiaSampleTheme
import com.android.security.samples.playintegrityapi.feature.streaming.ui.StreamingScreen
import com.android.security.samples.playintegrityapi.feature.streaming.ui.StreamingUiState
import com.android.security.samples.playintegrityapi.feature.streaming.ui.VideoPlayerUiState
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StreamingScreenTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private fun getString(id: Int) = context.getString(id)

    @Test
    fun streamingScreen_displaysAllExpectedElements() {
        composeTestRule.setContent {
            PiaSampleTheme {
                StreamingScreen(
                    onBackClick = {},
                    onFetchManifestClick = {},
                    exoPlayer = null,
                    uiState = StreamingUiState(),
                    onLifecycleStop = {},
                    onLifecycleStart = {}
                )
            }
        }


        composeTestRule.onNodeWithText(getString(R.string.streaming_top_bar_title))
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(getString(R.string.streaming_server_response))
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(getString(R.string.streaming_tier_premium_title))
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(getString(R.string.streaming_tier_restricted_title))
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(getString(R.string.streaming_btn_fetch_manifest))
            .assertIsDisplayed()
    }

    @Test
    fun streamingScreen_clickingBackButton_triggersOnBackClick() {
        var backClicked = false
        composeTestRule.setContent {
            PiaSampleTheme {
                StreamingScreen(
                    onBackClick = { backClicked = true },
                    onFetchManifestClick = {},
                    exoPlayer = null,
                    uiState = StreamingUiState(),
                    onLifecycleStop = {},
                    onLifecycleStart = {}
                )
            }
        }

        composeTestRule.onNodeWithContentDescription(getString(com.android.security.samples.playintegrityapi.core.ui.R.string.navigate_back_content_desc))
            .performClick()

        assertTrue(backClicked)
    }

    @Test
    fun streamingScreen_clickingFetchManifestButton_triggersOnClick() {
        var fetchClicked = false

        composeTestRule.setContent {
            PiaSampleTheme {
                StreamingScreen(
                    onBackClick = {},
                    onFetchManifestClick = { fetchClicked = true },
                    exoPlayer = null,
                    uiState = StreamingUiState(
                        isRefreshing = false
                    ),
                    onLifecycleStop = {},
                    onLifecycleStart = {}
                )
            }
        }

        composeTestRule.onNodeWithText(getString(R.string.streaming_btn_fetch_manifest))
            .assertIsDisplayed()
            .performClick()

        assertTrue(fetchClicked)
    }

    @Test
    fun streamingScreen_playerErrorState_showsErrorMessageOnVideoOverlay() {
        val testErrorMessage = "Unable to verify Play Integrity token."
        composeTestRule.setContent {
            PiaSampleTheme {
                StreamingScreen(
                    onBackClick = {},
                    onFetchManifestClick = {},
                    exoPlayer = null,
                    uiState = StreamingUiState(
                        playerState = VideoPlayerUiState(
                            isLoading = false,
                            isError = true,
                            errorMessage = testErrorMessage
                        )
                    ),
                    onLifecycleStop = {},
                    onLifecycleStart = {}
                )
            }
        }

        composeTestRule.onNodeWithText(testErrorMessage).assertIsDisplayed()
    }

    @Test
    fun streamingScreen_restrictedTier_displaysWarningIcon() {
        composeTestRule.setContent {
            PiaSampleTheme {
                StreamingScreen(
                    onBackClick = {},
                    onFetchManifestClick = {},
                    exoPlayer = null,
                    uiState = StreamingUiState(
                        activeTierIndex = 4 // Restricted tier
                    ),
                    onLifecycleStop = {},
                    onLifecycleStart = {}
                )
            }
        }

        composeTestRule.onNodeWithContentDescription("Warning").assertIsDisplayed()
    }
}