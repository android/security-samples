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

package com.android.security.samples.playintegrityapi

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.android.security.samples.playintegrityapi.ui.HomeScreen
import com.android.security.samples.playintegrityapi.core.ui.theme.PiaSampleTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HomeScreenTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private fun getString(id: Int) = context.getString(id)

    @Test
    fun homeScreen_displaysAllExpectedElements() {
        composeTestRule.setContent {
            PiaSampleTheme {
                HomeScreen(
                    onNavigateToBank = {},
                    onNavigateToStreaming = {},
                    onNavigateToGame = {}
                )
            }
        }

        // Verify Top Bar
        composeTestRule.onNodeWithText(getString(R.string.home_top_bar_title)).assertIsDisplayed()

        // Verify Cards
        composeTestRule.onNodeWithText(getString(R.string.use_case_bank_title)).assertIsDisplayed()
        composeTestRule.onNodeWithText(getString(R.string.use_case_streaming_title)).assertIsDisplayed()
        composeTestRule.onNodeWithText(getString(R.string.use_case_game_title)).assertIsDisplayed()

        // Verify Status Pill
        composeTestRule.onNodeWithText(getString(R.string.home_status_integrity_secure)).assertIsDisplayed()
    }

    @Test
    fun homeScreen_clickingBankCard_triggersNavigation() {
        var bankNavigated = false

        composeTestRule.setContent {
            PiaSampleTheme {
                HomeScreen(
                    onNavigateToBank = { bankNavigated = true },
                    onNavigateToStreaming = {},
                    onNavigateToGame = {}
                )
            }
        }

        composeTestRule.onNodeWithText(getString(R.string.use_case_bank_title)).performClick()
        assertTrue(bankNavigated)
    }

    @Test
    fun homeScreen_clickingStreamingCard_triggersNavigation() {
        var streamingNavigated = false

        composeTestRule.setContent {
            PiaSampleTheme {
                HomeScreen(
                    onNavigateToBank = {},
                    onNavigateToStreaming = { streamingNavigated = true },
                    onNavigateToGame = {}
                )
            }
        }

        composeTestRule.onNodeWithText(getString(R.string.use_case_streaming_title)).performClick()
        assertTrue(streamingNavigated)
    }

    @Test
    fun homeScreen_clickingGameCard_triggersNavigation() {
        var gameNavigated = false

        composeTestRule.setContent {
            PiaSampleTheme {
                HomeScreen(
                    onNavigateToBank = {},
                    onNavigateToStreaming = {},
                    onNavigateToGame = { gameNavigated = true }
                )
            }
        }

        composeTestRule.onNodeWithText(getString(R.string.use_case_game_title)).performClick()
        assertTrue(gameNavigated)
    }
}