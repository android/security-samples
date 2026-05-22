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

package com.android.security.samples.playintegrityapi.feature.streaming.ui

import android.content.Context
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.android.security.samples.playintegrityapi.core.integrity.IntegrityRepository
import com.android.security.samples.playintegrityapi.feature.streaming.domain.GetSecureStreamingConfigUseCase
import com.android.security.samples.playintegrityapi.feature.streaming.domain.StreamingResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.MockedConstruction
import org.mockito.Mockito
import org.mockito.Mockito.mockConstruction
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.clearInvocations
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
@OptIn(ExperimentalCoroutinesApi::class)
class StreamingViewModelTest {

    private lateinit var getSecureStreamingConfigUseCase: GetSecureStreamingConfigUseCase
    private lateinit var integrityRepository: IntegrityRepository
    private lateinit var context: Context
    private lateinit var viewModel: StreamingViewModel
    private val testDispatcher = UnconfinedTestDispatcher()

    private lateinit var mockExoPlayer: ExoPlayer
    private lateinit var exoPlayerBuilderMock: MockedConstruction<ExoPlayer.Builder>

    @Before
    fun setup() {
        Mockito.framework().clearInlineMocks()

        Dispatchers.setMain(testDispatcher)
        getSecureStreamingConfigUseCase = mock()
        integrityRepository = mock()
        context = mock()
        mockExoPlayer = mock()

        exoPlayerBuilderMock = mockConstruction(ExoPlayer.Builder::class.java) { mock, _ ->
            whenever(mock.build()).thenReturn(mockExoPlayer)
        }
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        exoPlayerBuilderMock.close()
        Mockito.framework().clearInlineMocks()
    }

    @Test
    fun `init calls warmUp on IntegrityRepository with correct GCP Project Number`() = runTest {
        whenever(getSecureStreamingConfigUseCase(any())).thenReturn(
            StreamingResult.Failure("Error")
        )

        viewModel = StreamingViewModel(
            context,
            getSecureStreamingConfigUseCase,
            integrityRepository
        )

        verify(integrityRepository).warmUp()
    }

    @Test
    fun `init fetches manifest and updates state on failure`() = runTest {
        whenever(getSecureStreamingConfigUseCase(any())).thenReturn(
            StreamingResult.Failure("Network Error")
        )

        viewModel = StreamingViewModel(
            context,
            getSecureStreamingConfigUseCase,
            integrityRepository
        )

        val state = viewModel.uiState.value
        assertFalse(state.isInitialLoading)
        assertFalse(state.isRefreshing)
        assertTrue(state.playerState.isError)
        assertEquals("Network Error", state.playerState.errorMessage)
    }

    @Test
    fun `fetchManifest delegates to UseCase when not currently loading`() = runTest {
        whenever(getSecureStreamingConfigUseCase(any())).thenReturn(
            StreamingResult.Failure("Initial Error")
        )
        viewModel = StreamingViewModel(
            context,
            getSecureStreamingConfigUseCase,
            integrityRepository
        )
        // Clear the interaction that happened automatically during ViewModel init
        clearInvocations(getSecureStreamingConfigUseCase)

        viewModel.fetchManifest()

        // Verify the forceWarmup parameter is passed as true for subsequent fetches
        verify(getSecureStreamingConfigUseCase).invoke(eq(true))
    }

    @Test
    fun `onLifecycleStop pauses exoPlayer and saves active play state`() = runTest {
        val mockSuccess = mock<StreamingResult.Success>(defaultAnswer = Mockito.RETURNS_DEEP_STUBS)
        whenever(mockSuccess.config.manifestUrl).thenReturn("https://test.com/stream.mpd")
        whenever(mockSuccess.config.playIntegrityToken).thenReturn("fake_token")
        whenever(getSecureStreamingConfigUseCase(any())).thenReturn(mockSuccess)

        viewModel = StreamingViewModel(
            context,
            getSecureStreamingConfigUseCase,
            integrityRepository
        )
        whenever(mockExoPlayer.playWhenReady).thenReturn(true)

        viewModel.onLifecycleStop()

        verify(mockExoPlayer).pause()
        viewModel.onLifecycleStart()
        verify(mockExoPlayer).play()
    }

    @Test
    fun `onLifecycleStart does not play exoPlayer if it was paused before going to background`() = runTest {
        val mockSuccess = mock<StreamingResult.Success>(defaultAnswer = Mockito.RETURNS_DEEP_STUBS)
        whenever(mockSuccess.config.manifestUrl).thenReturn("https://test.com/stream.mpd")
        whenever(mockSuccess.config.playIntegrityToken).thenReturn("fake_token")
        whenever(getSecureStreamingConfigUseCase(any())).thenReturn(mockSuccess)

        viewModel = StreamingViewModel(
            context,
            getSecureStreamingConfigUseCase,
            integrityRepository
        )

        whenever(mockExoPlayer.playWhenReady).thenReturn(false)

        viewModel.onLifecycleStop()
        viewModel.onLifecycleStart()
        verify(mockExoPlayer, never()).play()
    }

    @Test
    fun `Player listener STATE_BUFFERING updates isLoading to true`() = runTest {
        whenever(getSecureStreamingConfigUseCase(any())).thenReturn(StreamingResult.Failure("Error"))
        viewModel = StreamingViewModel(
            context,
            getSecureStreamingConfigUseCase,
            integrityRepository
        )
        val captor = argumentCaptor<Player.Listener>()
        verify(mockExoPlayer).addListener(captor.capture())
        val listener = captor.firstValue

        listener.onPlaybackStateChanged(Player.STATE_BUFFERING)

        assertTrue(viewModel.uiState.value.playerState.isLoading)
        assertFalse(viewModel.uiState.value.playerState.isError)
    }

    @Test
    fun `Player listener STATE_READY updates isLoading to false`() = runTest {
        whenever(getSecureStreamingConfigUseCase(any())).thenReturn(StreamingResult.Failure("Error"))
        viewModel = StreamingViewModel(
            context,
            getSecureStreamingConfigUseCase,
            integrityRepository
        )
        val captor = argumentCaptor<Player.Listener>()
        verify(mockExoPlayer).addListener(captor.capture())
        val listener = captor.firstValue

        listener.onPlaybackStateChanged(Player.STATE_READY)

        val state = viewModel.uiState.value
        assertFalse(state.playerState.isLoading)
        assertFalse(state.isInitialLoading)
        assertFalse(state.isRefreshing)
    }

    @Test
    fun `Player listener onPlayerError updates isError to true`() = runTest {
        whenever(getSecureStreamingConfigUseCase(any())).thenReturn(StreamingResult.Failure("Error"))
        viewModel = StreamingViewModel(
            context,
            getSecureStreamingConfigUseCase,
            integrityRepository
        )
        val captor = argumentCaptor<Player.Listener>()
        verify(mockExoPlayer).addListener(captor.capture())
        val listener = captor.firstValue

        val mockException = mock<PlaybackException>()
        listener.onPlayerError(mockException)

        assertTrue(viewModel.uiState.value.playerState.isError)
        assertFalse(viewModel.uiState.value.playerState.isLoading)
    }

    @Test
    fun `Player listener onIsPlayingChanged updates isPlaying state`() = runTest {
        whenever(getSecureStreamingConfigUseCase(any())).thenReturn(StreamingResult.Failure("Error"))
        viewModel = StreamingViewModel(
            context,
            getSecureStreamingConfigUseCase,
            integrityRepository
        )
        val captor = argumentCaptor<Player.Listener>()
        verify(mockExoPlayer).addListener(captor.capture())
        val listener = captor.firstValue

        listener.onIsPlayingChanged(true)

        assertTrue(viewModel.uiState.value.playerState.isPlaying)
    }

    private companion object {
        const val TEST_CLOUD_PROJECT_NUMBER = 12345678910L
    }
}