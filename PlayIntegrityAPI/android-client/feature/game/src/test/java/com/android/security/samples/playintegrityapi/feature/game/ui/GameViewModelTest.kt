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

import android.os.SystemClock
import com.android.security.samples.playintegrityapi.core.integrity.IntegrityRepository
import com.android.security.samples.playintegrityapi.feature.game.data.remote.GameInitiateResponse
import com.android.security.samples.playintegrityapi.feature.game.data.remote.GameStatusResponse
import com.android.security.samples.playintegrityapi.feature.game.data.remote.GameStopResponse
import com.android.security.samples.playintegrityapi.feature.game.data.remote.SecurityChecklistDto
import com.android.security.samples.playintegrityapi.feature.game.domain.GameResult
import com.android.security.samples.playintegrityapi.feature.game.domain.GenerateIntervalTokenUseCase
import com.android.security.samples.playintegrityapi.feature.game.domain.GetGameStatusUseCase
import com.android.security.samples.playintegrityapi.feature.game.domain.InitiateGameUseCase
import com.android.security.samples.playintegrityapi.feature.game.domain.SubmitGameScoreUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.MockedStatic
import org.mockito.Mockito.mockStatic
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class GameViewModelTest {

    private lateinit var initiateGameUseCase: InitiateGameUseCase
    private lateinit var getGameStatusUseCase: GetGameStatusUseCase
    private lateinit var generateIntervalTokenUseCase: GenerateIntervalTokenUseCase
    private lateinit var submitGameScoreUseCase: SubmitGameScoreUseCase
    private lateinit var integrityRepository: IntegrityRepository
    private lateinit var viewModel: GameViewModel

    private lateinit var systemClockMock: MockedStatic<SystemClock>

    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        initiateGameUseCase = mock()
        getGameStatusUseCase = mock()
        generateIntervalTokenUseCase = mock()
        submitGameScoreUseCase = mock()
        integrityRepository = mock()

        viewModel = GameViewModel(
            initiateGameUseCase,
            getGameStatusUseCase,
            generateIntervalTokenUseCase,
            submitGameScoreUseCase,
            integrityRepository
        )

        // Mock SystemClock to return a constant 1000L milliseconds
        systemClockMock = mockStatic(SystemClock::class.java)
        systemClockMock.`when`<Long> { SystemClock.elapsedRealtime() }.thenReturn(1000L)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        // Ensure the static mock is closed to prevent leaking into other test classes
        systemClockMock.close()
    }

    @Test
    fun `initializeSession on success sets state to Ready and updates checklist`() = runTest {
        val mockChecklist = SecurityChecklistDto(
            isSecure = true,
            screenCaptureSafe = true,
            accessibilitySafe = false,
            playProtectSafe = true
        )
        val mockResponse = GameInitiateResponse(
            status = "OK",
            sessionId = "session-123",
            targetTime = 5.0,
            intervals = emptyList(),
            checklist = mockChecklist
        )
        whenever(initiateGameUseCase()).thenReturn(GameResult.Success(mockResponse))

        viewModel.initializeSession()

        val state = viewModel.uiState.value
        assertTrue(state.gameState is GameState.Ready)
        assertEquals(5.0, (state.gameState as GameState.Ready).targetTime, 0.0)
        assertTrue(state.checkScreenCapture)
        assertFalse(state.checkAccessibility)
        assertTrue(state.checkPlayProtect)
    }

    @Test
    fun `initializeSession on failure sets state to InitError`() = runTest {
        val errorMessage = "Environment compromised"
        whenever(initiateGameUseCase()).thenReturn(GameResult.Failure.NetworkError(errorMessage))

        viewModel.initializeSession()

        val state = viewModel.uiState.value
        assertTrue(state.gameState is GameState.InitError)
        assertEquals(errorMessage, (state.gameState as GameState.InitError).message)
    }

    @Test
    fun `startGame updates state to Playing when previously Ready`() = runTest {
        val mockResponse = GameInitiateResponse(
            status = "OK",
            sessionId = "session-123",
            targetTime = 4.0,
            intervals = emptyList(),
            checklist = SecurityChecklistDto(true, true, true, true)
        )
        whenever(initiateGameUseCase()).thenReturn(GameResult.Success(mockResponse))
        viewModel.initializeSession()

        viewModel.startGame()

        val state = viewModel.uiState.value
        assertTrue(state.gameState is GameState.Playing)
        assertEquals(4.0, (state.gameState as GameState.Playing).targetTime, 0.0)
    }

    @Test
    fun `stopGame updates state to Result and calls submit endpoint`() = runTest {
        val mockResponse = GameInitiateResponse(
            status = "OK",
            sessionId = "session-123",
            targetTime = 10.0,
            intervals = emptyList(),
            checklist = SecurityChecklistDto(true, true, true, true)
        )
        whenever(initiateGameUseCase()).thenReturn(GameResult.Success(mockResponse))
        whenever(submitGameScoreUseCase(any())).thenReturn(
            GameResult.Success(
                GameStopResponse(
                    "OK", "Success"
                )
            )
        )
        viewModel.initializeSession()
        viewModel.startGame()

        viewModel.stopGame()

        val state = viewModel.uiState.value
        assertTrue(state.gameState is GameState.Result)
        val resultState = state.gameState as GameState.Result
        assertTrue(resultState.isVerified)
        assertEquals(10.0, resultState.targetTime, 0.0)
        // Because the test runs instantly, actualTime is ~0.0s and difference is ~10.0s
        // Therefore, we expect ScoreTier.MISSED based on the viewmodel logic.
        assertEquals(ScoreTier.MISSED, resultState.tier)
        verify(submitGameScoreUseCase).invoke(any())
    }

    @Test
    fun `stopGame sets unverified Result when server submission fails`() = runTest {
        val mockResponse = GameInitiateResponse(
            status = "OK",
            sessionId = "session-123",
            targetTime = 5.0,
            intervals = emptyList(),
            checklist = SecurityChecklistDto(true, true, true, true)
        )
        whenever(initiateGameUseCase()).thenReturn(GameResult.Success(mockResponse))
        whenever(submitGameScoreUseCase(any())).thenReturn(GameResult.Failure.IntegrityError("Untrusted Environment"))
        viewModel.initializeSession()
        viewModel.startGame()

        viewModel.stopGame()

        val state = viewModel.uiState.value
        assertTrue(state.gameState is GameState.Result)
        val resultState = state.gameState as GameState.Result
        assertFalse(resultState.isVerified)
        assertEquals("Untrusted Environment", resultState.rejectReason)
    }

    @Test
    fun `validateEnvironmentOnResume updates checklist if game is Playing`() = runTest {
        val mockResponse = GameInitiateResponse(
            status = "OK",
            sessionId = "session-123",
            targetTime = 5.0,
            intervals = emptyList(),
            checklist = SecurityChecklistDto(true, true, true, true)
        )
        whenever(initiateGameUseCase()).thenReturn(GameResult.Success(mockResponse))
        viewModel.initializeSession()
        viewModel.startGame()
        val updatedChecklist = SecurityChecklistDto(
            true, screenCaptureSafe = true, accessibilitySafe = true, playProtectSafe = false
        )
        whenever(getGameStatusUseCase()).thenReturn(
            GameResult.Success(
                GameStatusResponse(
                    "OK", updatedChecklist
                )
            )
        )

        viewModel.validateEnvironmentOnResume()

        val state = viewModel.uiState.value
        assertTrue(state.checkScreenCapture)
        assertTrue(state.checkAccessibility)
        assertFalse(state.checkPlayProtect)
    }

    @Test
    fun `resetGame re-initializes the session`() = runTest {
        val mockResponse = GameInitiateResponse(
            status = "OK",
            sessionId = "session-123",
            targetTime = 5.0,
            intervals = emptyList(),
            checklist = SecurityChecklistDto(true, true, true, true)
        )
        whenever(initiateGameUseCase()).thenReturn(GameResult.Success(mockResponse))


        viewModel.resetGame()
        // Fast-forward virtual time to skip the 1500ms delay in resetGame()
        // and allow initializeSession() to complete.
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.gameState is GameState.Ready)
        verify(initiateGameUseCase).invoke()
    }
}