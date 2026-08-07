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
import android.util.Log
import androidx.annotation.OptIn
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.dash.DashMediaSource
import com.android.security.samples.playintegrityapi.core.integrity.IntegrityRepository
import com.android.security.samples.playintegrityapi.core.integrity.di.GoogleCloudProjectNumber
import com.android.security.samples.playintegrityapi.core.network.NetworkConstants
import com.android.security.samples.playintegrityapi.feature.streaming.domain.GetSecureStreamingConfigUseCase
import com.android.security.samples.playintegrityapi.feature.streaming.domain.StreamingResult
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Represents the UI state specifically for the video player.
 *
 * @property errorMessage A user-facing message describing what went wrong when play back fails.
 * @property isPlaying True if the media is currently playing, false if paused or stopped.
 * @property isLoading True if the player is actively buffering or preparing media.
 * @property isError True if a playback error occurred, triggering the error overlay and retry logic.
 * @property isSeamlessHandoff True if the player is transitioning to a new manifest (e.g., after a refresh).
 *                             When true, it suppresses the loading spinner to prevent jarring UI flashes.
 */
data class VideoPlayerUiState(
    val errorMessage: String? = null,
    val isPlaying: Boolean = false,
    val isLoading: Boolean = true,
    val isError: Boolean = false,
    val isSeamlessHandoff: Boolean = false
)


/**
 * Represents the overall UI state of the Streaming screen.
 *
 * @property activeTierIndex The index of the currently playing quality tier based on video height
 *                           (0 = Premium/1080p, down to 4 = Restricted). Used to highlight the active card.
 * @property isInitialLoading True during the very first manifest fetch when the screen is opened.
 * @property isRefreshing True when the user manually requests a manifest refresh via the UI button.
 * @property playerState The nested UI state for the ExoPlayer widget.
 */
data class StreamingUiState(
    val activeTierIndex: Int = -1,
    val isInitialLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val playerState: VideoPlayerUiState = VideoPlayerUiState()
)

@HiltViewModel
class StreamingViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val getSecureStreamingConfigUseCase: GetSecureStreamingConfigUseCase,
    private val integrityRepository: IntegrityRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(StreamingUiState())
    val uiState: StateFlow<StreamingUiState> = _uiState.asStateFlow()
    private var resumeOnForeground: Boolean = true
    private val warmUpTokenJob: Job = viewModelScope.launch {
        integrityRepository.warmUp()
    }

    val exoPlayer: ExoPlayer = initializeExoPlayer()

    init {
        executeManifestFetch(isInitial = true)
    }

    fun fetchManifest() {
        if (_uiState.value.isInitialLoading || _uiState.value.isRefreshing) return

        _uiState.update { state ->
            state.copy(
                isRefreshing = true,
                playerState = state.playerState.copy(errorMessage = null, isError = false)
            )
        }
        executeManifestFetch(isInitial = false)
    }

    fun onLifecycleStop() {
        resumeOnForeground = exoPlayer.playWhenReady
        exoPlayer.pause()
    }

    fun onLifecycleStart() {
        if (resumeOnForeground) {
            exoPlayer.play()
        }
    }

    override fun onCleared() {
        super.onCleared()
        exoPlayer.release()
    }

    private fun initializeExoPlayer(): ExoPlayer {
        return ExoPlayer.Builder(context).build().apply {
            repeatMode = Player.REPEAT_MODE_ALL
            addListener(createPlayerListener())
        }
    }

    private fun createPlayerListener() = object : Player.Listener {
        override fun onIsPlayingChanged(isPlayingStatus: Boolean) =
            handleIsPlayingChanged(isPlayingStatus)

        override fun onPlaybackStateChanged(playbackState: Int) =
            handlePlaybackStateChanged(playbackState)

        override fun onPlayerError(error: PlaybackException) = handlePlayerError(error)
        override fun onTracksChanged(tracks: Tracks) = handleTracksChanged(tracks)
    }

    private fun executeManifestFetch(isInitial: Boolean) {
        viewModelScope.launch {
            // If this is the initial manifest fetch, let's wait for the PIA warm-up job
            // (initiated when the micro app started) to complete before fetching the manifest.
            if (isInitial) warmUpTokenJob.join()

            // On subsequent fetches, we force a full refresh. Since we aim to showcase the PIA
            // test response feature, forcing a warmup is required so that any changes made to the
            // test response in the Play Console take effect immediately.
            when (val result = getSecureStreamingConfigUseCase(forceWarmup = !isInitial)) {
                is StreamingResult.Success -> handleManifestFetchSuccess(result, isInitial)
                is StreamingResult.Failure -> handleManifestFetchFailure(result.message)
            }
        }
    }

    @OptIn(UnstableApi::class)
    private fun handleManifestFetchSuccess(result: StreamingResult.Success, isInitial: Boolean) {
        _uiState.update { state ->
            state.copy(
                playerState = state.playerState.copy(
                    errorMessage = null,
                    isSeamlessHandoff = !isInitial
                )
            )
        }

        preparePlayerMediaSource(
            manifestUrl = result.config.manifestUrl,
            integrityToken = result.config.playIntegrityToken,
            isInitial = isInitial
        )
    }

    private fun handleManifestFetchFailure(errorMessage: String?) {
        _uiState.update { state ->
            state.copy(
                isInitialLoading = false,
                isRefreshing = false,
                playerState = state.playerState.copy(
                    errorMessage = errorMessage,
                    isLoading = false,
                    isError = true,
                    isSeamlessHandoff = false
                )
            )
        }
    }

    // ExoPlayer Network Injection:
    // The client does not manually download the XML manifest. Instead, it natively
    // instructs ExoPlayer to append the integrity token to its outbound HTTP headers
    // using DefaultHttpDataSource.Factory().setDefaultRequestProperties().
    // This factory is passed into the DashMediaSource, ensuring the token is present
    // when ExoPlayer requests the .mpd file over the network.
    @OptIn(UnstableApi::class)
    private fun preparePlayerMediaSource(
        manifestUrl: String,
        integrityToken: String?,
        isInitial: Boolean
    ) {
        val dataSourceFactory = DefaultHttpDataSource.Factory().apply {
            if (!integrityToken.isNullOrBlank()) {
                setDefaultRequestProperties(mapOf(NetworkConstants.Header.PLAY_INTEGRITY_TOKEN to integrityToken))
            }
        }

        val mediaItem = MediaItem.fromUri(manifestUrl)
        val mediaSource = DashMediaSource.Factory(dataSourceFactory).createMediaSource(mediaItem)

        val currentPosition = exoPlayer.currentPosition
        val wasPlaying = exoPlayer.playWhenReady

        exoPlayer.setMediaSource(mediaSource)

        if (!isInitial && currentPosition > 0L) {
            exoPlayer.seekTo(currentPosition)
        }

        exoPlayer.prepare()
        exoPlayer.playWhenReady = if (isInitial) true else wasPlaying
    }

    private fun handleIsPlayingChanged(isPlayingStatus: Boolean) {
        _uiState.update { state ->
            if (state.playerState.isSeamlessHandoff && !isPlayingStatus) {
                state
            } else {
                state.copy(playerState = state.playerState.copy(isPlaying = isPlayingStatus))
            }
        }
    }

    private fun handlePlaybackStateChanged(playbackState: Int) {
        when (playbackState) {
            Player.STATE_BUFFERING -> {
                if (!_uiState.value.isRefreshing) {
                    _uiState.update { state ->
                        state.copy(
                            playerState = state.playerState.copy(
                                isLoading = true,
                                isError = false
                            )
                        )
                    }
                }
            }

            Player.STATE_READY -> {
                _uiState.update { state ->
                    state.copy(
                        isInitialLoading = false,
                        isRefreshing = false,
                        playerState = state.playerState.copy(
                            isLoading = false,
                            isError = false,
                            isSeamlessHandoff = false
                        )
                    )
                }
            }
        }
    }

    private fun handlePlayerError(error: PlaybackException) {
        Log.e(javaClass.name, "Exoplayer playback error: $error")
        _uiState.update { state ->
            state.copy(
                isInitialLoading = false,
                isRefreshing = false,
                playerState = state.playerState.copy(
                    isError = true,
                    isLoading = false,
                    isSeamlessHandoff = false
                )
            )
        }
    }

    // Handling Dynamic Tiers & UI State:
    // The Android client is completely agnostic to the quality tier it receives.
    // ExoPlayer automatically parses the dynamically filtered DASH manifest returned
    // by the Node.js server. When the manifest loads, onTracksChanged scans the available
    // video tracks to find the maximum videoHeight the server authorized.
    private fun handleTracksChanged(tracks: Tracks) {
        var maxHeight = 0
        for (group in tracks.groups) {
            if (group.type == C.TRACK_TYPE_VIDEO) {
                for (i in 0 until group.length) {
                    val height = group.getTrackFormat(i).height
                    if (height > maxHeight) maxHeight = height
                }
            }
        }
        if (maxHeight > 0) updateActiveTierBasedOnManifest(maxHeight)
    }

    private fun updateActiveTierBasedOnManifest(maxVideoHeight: Int) {
        val newTierIndex = when {
            maxVideoHeight >= 1080 -> 0
            maxVideoHeight >= 720 -> 1
            maxVideoHeight >= 480 -> 2
            maxVideoHeight >= 240 -> 3
            else -> 4
        }

        if (_uiState.value.activeTierIndex != newTierIndex) {
            _uiState.update { it.copy(activeTierIndex = newTierIndex) }
        }
    }
}