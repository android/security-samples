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

package com.android.security.samples.playintegrityapi.feature.game.ui

import android.app.Activity
import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.security.samples.playintegrityapi.core.integrity.IntegrityRepository
import com.android.security.samples.playintegrityapi.feature.game.data.remote.GameInitiateResponse
import com.android.security.samples.playintegrityapi.feature.game.data.remote.IntervalTokenDto
import com.android.security.samples.playintegrityapi.feature.game.data.remote.SecurityChecklistDto
import com.android.security.samples.playintegrityapi.feature.game.data.remote.GameStopRequest
import com.android.security.samples.playintegrityapi.feature.game.data.remote.GameStopResponse
import com.android.security.samples.playintegrityapi.feature.game.domain.*
import com.google.android.play.core.integrity.StandardIntegrityManager.StandardIntegrityToken
import com.google.android.play.core.integrity.model.IntegrityDialogTypeCode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.abs

/**
 * Represents the performance tier of the player based on the accuracy of their timing.
 */
enum class ScoreTier {
    /** Perfect timing (exact match with the target). */
    PERFECT,

    /** Very close to the target time (error <= 20%). */
    NEAR_PERFECT,

    /** Moderately close to the target time (error <= 50%). */
    NEAR_MISS,

    /** Far from the target time (error > 50%). */
    MISSED
}

/**
 * Represents the various stages of the game's lifecycle.
 */
sealed interface GameState {
    /** The initial state before the game session is requested. */
    object Idle : GameState

    /** The game session is being initialized by contacting the backend and fetching intervals. */
    object Initializing : GameState

    /**
     * The game session is initialized and ready to begin.
     *
     * @property targetTime The time in seconds the player needs to match.
     */
    data class Ready(val targetTime: Double) : GameState

    /**
     * The game is actively running (pulse animation active) and the player is timing their stop.
     *
     * @property targetTime The time in seconds the player is trying to match.
     */
    data class Playing(val targetTime: Double) : GameState

    /**
     * The player has stopped the game, and the result is being submitted to the backend for verification.
     *
     * @property targetTime The original target time in seconds.
     * @property actualTime The actual time in seconds the player waited before stopping.
     */
    data class Submitting(val targetTime: Double, val actualTime: Double) : GameState

    /**
     * The final outcome of the game after server verification.
     *
     * @property targetTime The original target time in seconds.
     * @property actualTime The actual time in seconds the player achieved.
     * @property difference The absolute difference between [targetTime] and [actualTime].
     * @property tier The performance tier achieved by the player.
     * @property isVerified True if the backend verified the Play Integrity tokens successfully.
     * @property rejectReason A message detailing why the score was rejected, if [isVerified] is false.
     * @property remediationToken An optional token used to trigger a Play Integrity remediation dialog.
     */
    data class Result(
        val targetTime: Double,
        val actualTime: Double,
        val difference: Double,
        val tier: ScoreTier,
        val isVerified: Boolean,
        val rejectReason: String? = null,
        val remediationToken: StandardIntegrityToken? = null
    ) : GameState

    /**
     * Represents a failure during the initial game setup.
     *
     * @property message The error message detailing why initialization failed.
     */
    data class InitError(val message: String) : GameState
}

/**
 * The overall UI state for the game screen, combining the game lifecycle and the security checklist.
 *
 * @property gameState The current phase of the core game loop (e.g., Idle, Playing, Result).
 * @property checkScreenCapture True if the environment is safe from unauthorized screen capture.
 * @property checkAccessibility True if the environment is safe from risky accessibility apps.
 * @property checkPlayProtect True if Google Play Protect is enabled and scanning the device.
 */
data class GameUiState(
    val gameState: GameState = GameState.Idle,
    val checkScreenCapture: Boolean = true,
    val checkAccessibility: Boolean = true,
    val checkPlayProtect: Boolean = true
)

@HiltViewModel
class GameViewModel @Inject constructor(
    private val initiateGameUseCase: InitiateGameUseCase,
    private val getGameStatusUseCase: GetGameStatusUseCase,
    private val generateIntervalTokenUseCase: GenerateIntervalTokenUseCase,
    private val submitGameScoreUseCase: SubmitGameScoreUseCase,
    private val integrityRepository: IntegrityRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(GameUiState())
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    private var sessionId: String? = null
    private var startTimeMillis: Long = 0
    private var intervals: List<Double> = emptyList()
    private val intervalTokens = mutableListOf<IntervalTokenDto>()

    fun initializeSession() {
        viewModelScope.launch {
            setInitializingState()

            when (val result = initiateGameUseCase()) {
                is GameResult.Success -> handleInitializationSuccess(result.data)
                is GameResult.Failure -> handleInitializationFailure(result.message)
            }
        }
    }

    fun startGame() {
        val currentState = _uiState.value.gameState
        if (currentState is GameState.Ready) {
            prepareGameForStart(currentState.targetTime)
            scheduleAllIntervalTokens()
        }
    }

    fun stopGame() {
        val currentState = _uiState.value.gameState
        if (currentState is GameState.Playing) {
            val stopTimeMillis = SystemClock.elapsedRealtime()
            val actualTime = calculateActualTime(startTimeMillis, stopTimeMillis)

            setSubmittingState(currentState.targetTime, actualTime)

            viewModelScope.launch {
                processGameStop(currentState.targetTime, actualTime)
            }
        }
    }

    fun resetGame() {
        viewModelScope.launch {
            setInitializingState()
            delay(1500) // Artificial delay for smooth UX
            clearSessionMemory()
            initializeSession()
        }
    }

    fun validateEnvironmentOnResume() {
        val currentState = _uiState.value.gameState
        if (currentState is GameState.Playing) {
            viewModelScope.launch {
                when (val result = getGameStatusUseCase()) {
                    is GameResult.Success -> updateChecklistUi(result.data.checklist)
                    is GameResult.Failure -> { /* Silently retain previous state on network failure */
                    }
                }
            }
        }
    }

    fun triggerRemediationDialog(activity: Activity) {
        val state = _uiState.value
        val gameState = state.gameState

        val typeCode = when {
            !state.checkScreenCapture || !state.checkAccessibility -> IntegrityDialogTypeCode.CLOSE_ALL_ACCESS_RISK
            !state.checkPlayProtect -> IntegrityDialogTypeCode.GET_STRONG_INTEGRITY
            else -> return
        }

        if (gameState is GameState.Result && gameState.remediationToken != null) {
            viewModelScope.launch {
                // TODO: Handle dialog result
                integrityRepository.showDialog(activity, typeCode, gameState.remediationToken)
            }
        }
    }

    private fun setInitializingState() {
        _uiState.update { it.copy(gameState = GameState.Initializing) }
    }

    private fun handleInitializationSuccess(response: GameInitiateResponse) {
        sessionId = response.sessionId
        intervals = response.intervals
        updateChecklistUi(response.checklist)
        // NOTE: The game transitions to Ready and allows the player to start even if the initial
        // checklist contains violations. The server tracks these violations internally and enforces
        // score rejection only at the end of the session, preventing instant feedback to potential cheaters.
        _uiState.update { it.copy(gameState = GameState.Ready(response.targetTime)) }
    }

    private fun handleInitializationFailure(message: String) {
        _uiState.update { it.copy(gameState = GameState.InitError(message)) }
    }

    private fun prepareGameForStart(targetTime: Double) {
        startTimeMillis = SystemClock.elapsedRealtime()
        intervalTokens.clear()
        _uiState.update { it.copy(gameState = GameState.Playing(targetTime)) }
    }

    /**
     * TOCTOU Defence: Schedule background token generation for every random interval.
     * While the game is running, the client silently requests a new Play Integrity token
     * at each required interval. The requestHash for these intermediate tokens binds the
     * sessionId and the current interval time.
     */
    private fun scheduleAllIntervalTokens() {
        intervals.forEach { interval ->
            scheduleSingleIntervalToken(interval)
        }
    }

    private fun scheduleSingleIntervalToken(interval: Double) {
        viewModelScope.launch {
            delay((interval * 1000).toLong())

            // The session could have ended early, abort generation if we aren't playing
            if (_uiState.value.gameState !is GameState.Playing) return@launch

            val tokenResult = generateIntervalTokenUseCase(
                sessionId = sessionId ?: return@launch,
                clientStartTime = startTimeMillis,
                interval = interval
            )

            // If generation fails mid-game, we silently ignore it. The server will
            // catch the missing token at the end and reject the payload.
            tokenResult.getOrNull()?.let { token ->
                intervalTokens.add(IntervalTokenDto(interval, token.token()))
            }
        }
    }

    private fun calculateActualTime(startMillis: Long, stopMillis: Long): Double {
        return (stopMillis - startMillis) / 1000.0
    }

    private fun setSubmittingState(targetTime: Double, actualTime: Double) {
        _uiState.update { it.copy(gameState = GameState.Submitting(targetTime, actualTime)) }
    }

    private suspend fun processGameStop(targetTime: Double, actualTime: Double) {
        val request = buildGameStopRequest(actualTime)
        val result = submitGameScoreUseCase(request)

        val difference = abs(targetTime - actualTime)
        val tier = calculateScoreTier(targetTime, difference)

        handleGameSubmissionResult(targetTime, actualTime, difference, tier, result)
    }

    private fun buildGameStopRequest(actualTime: Double): GameStopRequest {
        return GameStopRequest(
            sessionId = sessionId ?: "",
            clientStartTime = startTimeMillis,
            actualTime = actualTime,
            intervalTokens = intervalTokens.toList()
        )
    }

    private fun calculateScoreTier(targetTime: Double, difference: Double): ScoreTier {
        val errorPercentage = difference / targetTime

        return when {
            difference == 0.0 -> ScoreTier.PERFECT
            errorPercentage <= 0.20 -> ScoreTier.NEAR_PERFECT
            errorPercentage <= 0.50 -> ScoreTier.NEAR_MISS
            else -> ScoreTier.MISSED
        }
    }

    private fun handleGameSubmissionResult(
        targetTime: Double,
        actualTime: Double,
        difference: Double,
        tier: ScoreTier,
        result: GameResult<GameStopResponse>
    ) {
        when (result) {
            is GameResult.Success -> {
                _uiState.update {
                    it.copy(
                        gameState = GameState.Result(
                            targetTime = targetTime,
                            actualTime = actualTime,
                            difference = difference,
                            tier = tier,
                            isVerified = true
                        )
                    )
                }
            }

            is GameResult.Failure -> {
                val token = (result as? GameResult.Failure.IntegrityError)?.token

                _uiState.update {
                    it.copy(
                        gameState = GameState.Result(
                            targetTime = targetTime,
                            actualTime = actualTime,
                            difference = difference,
                            tier = tier,
                            isVerified = false,
                            rejectReason = result.message,
                            remediationToken = token
                        )
                    )
                }
            }
        }
    }

    private fun clearSessionMemory() {
        sessionId = null
        intervals = emptyList()
        intervalTokens.clear()
    }

    private fun updateChecklistUi(checklist: SecurityChecklistDto) {
        _uiState.update {
            it.copy(
                checkScreenCapture = checklist.screenCaptureSafe,
                checkAccessibility = checklist.accessibilitySafe,
                checkPlayProtect = checklist.playProtectSafe
            )
        }
    }
}