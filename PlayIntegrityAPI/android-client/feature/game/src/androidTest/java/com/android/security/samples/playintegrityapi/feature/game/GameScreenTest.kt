package com.android.security.samples.playintegrityapi.feature.game

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.android.security.samples.playintegrityapi.core.ui.theme.PiaSampleTheme
import com.android.security.samples.playintegrityapi.feature.game.ui.GameScreen
import com.android.security.samples.playintegrityapi.feature.game.ui.GameState
import com.android.security.samples.playintegrityapi.feature.game.ui.GameUiState
import com.android.security.samples.playintegrityapi.feature.game.ui.ScoreTier
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class GameScreenTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private fun getString(id: Int) = context.getString(id)

    /**
     * Helper function to render the GameScreen while safely handling infinite animations.
     * It pauses the test clock to prevent ComposeNotIdleExceptions and fast-forwards
     * 500ms to allow AnimatedContent entry transitions to complete.
     */
    private fun setupGameScreen(
        gameState: GameState,
        onBackClick: () -> Unit = {},
        onInitializeClick: () -> Unit = {},
        onStartClick: () -> Unit = {},
        onStopClick: () -> Unit = {},
        onResetClick: () -> Unit = {},
        onRemediateClick: () -> Unit = {}
    ) {
        // Disable auto-advancing to prevent infinite loops from stalling the test
        composeTestRule.mainClock.autoAdvance = false

        composeTestRule.setContent {
            PiaSampleTheme {
                GameScreen(
                    uiState = GameUiState(gameState = gameState),
                    onBackClick = onBackClick,
                    onInitializeClick = onInitializeClick,
                    onStartClick = onStartClick,
                    onStopClick = onStopClick,
                    onResetClick = onResetClick,
                    onRemediateClick = onRemediateClick
                )
            }
        }

        // Fast-forward through the AnimatedContent entry crossfades (which take 250ms)
        composeTestRule.mainClock.advanceTimeBy(500L)
    }

    @Test
    fun gameScreen_displaysAllExpectedElements_inIdleState() {
        setupGameScreen(gameState = GameState.Idle)

        composeTestRule.onNodeWithText(getString(R.string.game_top_bar_title)).assertIsDisplayed()
        composeTestRule.onNodeWithText(getString(R.string.game_header_title)).assertIsDisplayed()
        composeTestRule.onNodeWithText(getString(R.string.game_header_subtitle)).assertIsDisplayed()
        composeTestRule.onNodeWithText(getString(R.string.game_btn_start_secure_session))
            .assertIsDisplayed()
    }

    @Test
    fun gameScreen_clickingBackButton_triggersOnBackClick() {
        var backClicked = false
        setupGameScreen(
            gameState = GameState.Idle,
            onBackClick = { backClicked = true }
        )

        composeTestRule.onNodeWithContentDescription("Back").performClick()
        assertTrue(backClicked)
    }

    @Test
    fun gameScreen_idleState_clickingInitializeButton_triggersOnInitializeClick() {
        var initializeClicked = false
        setupGameScreen(
            gameState = GameState.Idle,
            onInitializeClick = { initializeClicked = true }
        )

        composeTestRule.onNodeWithText(getString(R.string.game_btn_start_secure_session))
            .assertIsDisplayed()
            .performClick()

        assertTrue(initializeClicked)
    }

    @Test
    fun gameScreen_readyState_displaysStartButton_andTriggersOnStartClick() {
        var startClicked = false
        setupGameScreen(
            gameState = GameState.Ready(targetTime = 5.0),
            onStartClick = { startClicked = true }
        )

        composeTestRule.onNodeWithText(getString(R.string.game_btn_start))
            .assertIsDisplayed()
            .performClick()

        assertTrue(startClicked)
    }

    @Test
    fun gameScreen_playingState_displaysStopButton_andTriggersOnStopClick() {
        var stopClicked = false
        setupGameScreen(
            gameState = GameState.Playing(targetTime = 5.0),
            onStopClick = { stopClicked = true }
        )

        composeTestRule.onNodeWithText(getString(R.string.game_btn_stop))
            .assertIsDisplayed()
            .performClick()

        assertTrue(stopClicked)
    }

    @Test
    fun gameScreen_resultState_displaysPlayAgainButton_andTriggersOnResetClick() {
        var resetClicked = false
        setupGameScreen(
            gameState = GameState.Result(
                targetTime = 5.0,
                actualTime = 5.0,
                difference = 0.0,
                tier = ScoreTier.PERFECT,
                isVerified = true
            ),
            onResetClick = { resetClicked = true }
        )

        composeTestRule.onNodeWithText(getString(R.string.game_btn_play_again))
            .assertIsDisplayed()
            .performClick()

        assertTrue(resetClicked)
    }

    @Test
    fun gameScreen_errorState_showsErrorMessage_andTryAgainButton() {
        val testErrorMessage = "Cheat toggling detected: Environment compromised."
        var initializeClicked = false

        setupGameScreen(
            gameState = GameState.InitError(testErrorMessage),
            onInitializeClick = { initializeClicked = true }
        )

        composeTestRule.onNodeWithText(testErrorMessage).assertIsDisplayed()
        composeTestRule.onNodeWithText(getString(R.string.game_btn_try_again), ignoreCase = true)
            .assertIsDisplayed()
            .performClick()

        assertTrue(initializeClicked)
    }
}