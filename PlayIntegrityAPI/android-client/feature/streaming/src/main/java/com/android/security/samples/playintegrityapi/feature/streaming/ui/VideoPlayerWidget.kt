package com.android.security.samples.playintegrityapi.feature.streaming.ui

import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.android.security.samples.playintegrityapi.core.ui.theme.PiaSampleTheme
import com.android.security.samples.playintegrityapi.feature.streaming.R
import kotlinx.coroutines.delay

@OptIn(UnstableApi::class)
@Composable
fun VideoPlayerWidget(
    exoPlayer: ExoPlayer?,
    state: VideoPlayerUiState,
    onRetry: () -> Unit,
    onLifecycleStop: () -> Unit,
    onLifecycleStart: () -> Unit,
    modifier: Modifier = Modifier
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    var showControls by remember { mutableStateOf(true) }

    val isError = state.errorMessage != null || state.isError

    val showPlayerSpinner =
        state.isLoading && state.errorMessage == null && !isError && !state.isSeamlessHandoff

    LaunchedEffect(state.isPlaying, showControls, showPlayerSpinner, isError) {
        if (state.isPlaying && showControls && !showPlayerSpinner && !isError) {
            delay(2500)
            showControls = false
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable(
                interactionSource = remember { MutableInteractionSource() }, indication = null
            ) { showControls = !showControls }, contentAlignment = Alignment.Center
    ) {
        if (LocalInspectionMode.current || exoPlayer == null) {
            CustomVideoController(
                isPlaying = false,
                isLoading = state.isLoading && !state.isSeamlessHandoff,
                isError = isError,
                errorMessage = state.errorMessage,
                isVisible = true,
                onTogglePlayPause = {})
        } else {
            DisposableEffect(lifecycleOwner, exoPlayer) {
                val observer = LifecycleEventObserver { _, event ->
                    if (event == Lifecycle.Event.ON_STOP) {
                        onLifecycleStop()
                    } else if (event == Lifecycle.Event.ON_START) {
                        onLifecycleStart()
                    }
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose {
                    lifecycleOwner.lifecycle.removeObserver(observer)
                }
            }

            AndroidView(
                factory = { ctx ->
                PlayerView(ctx).apply {
                    useController = false
                    setKeepContentOnPlayerReset(true)
                    player = exoPlayer
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                }
            }, update = { view ->
                if (view.player != exoPlayer) {
                    view.player = exoPlayer
                }
            }, modifier = Modifier.fillMaxSize()
            )

            CustomVideoController(
                isPlaying = state.isPlaying,
                isLoading = showPlayerSpinner,
                isError = isError,
                errorMessage = state.errorMessage
                    ?: stringResource(R.string.streaming_playback_failed),
                isVisible = showControls || !state.isPlaying || showPlayerSpinner || isError,
                onTogglePlayPause = {
                    if (isError) {
                        onRetry()
                    } else if (!showPlayerSpinner) {
                        if (state.isPlaying) exoPlayer.pause() else exoPlayer.play()
                    }
                })
        }
    }
}

@Composable
private fun CustomVideoController(
    isPlaying: Boolean,
    isLoading: Boolean,
    isError: Boolean,
    isVisible: Boolean,
    modifier: Modifier = Modifier,
    errorMessage: String? = null,
    onTogglePlayPause: () -> Unit,
) {
    AnimatedVisibility(
        visible = isVisible, enter = fadeIn(), exit = fadeOut(), modifier = modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = if (!isLoading && !isError) 0.2f else 0.6f)),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .shadow(4.dp, CircleShape)
                        .then(
                            if (!isLoading && !isError) Modifier.background(
                                MaterialTheme.colorScheme.primary, CircleShape
                            )
                            else Modifier
                        )
                        .clickable(enabled = !isLoading) { onTogglePlayPause() },
                    contentAlignment = Alignment.Center
                ) {
                    when {
                        isLoading -> {
                            CircularProgressIndicator(
                                color = Color.White,
                                strokeWidth = 3.dp,
                                modifier = Modifier.size(36.dp)
                            )
                        }

                        isError -> {
                            Icon(
                                imageVector = Icons.Outlined.ErrorOutline,
                                contentDescription = stringResource(R.string.streaming_playback_failed),
                                tint = Color.White,
                                modifier = Modifier.size(42.dp)
                            )
                        }

                        else -> {
                            Icon(
                                imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                contentDescription = stringResource(if (isPlaying) R.string.streaming_video_pause_content_desc else R.string.streaming_video_play_content_desc),
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }
                }

                if (isError && !errorMessage.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = errorMessage,
                        color = Color.White,
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 400, heightDp = 225)
@Composable
fun VideoPlayerWidgetPreview_Loading() {
    PiaSampleTheme(dynamicColor = false) {
        VideoPlayerWidget(
            exoPlayer = null, state = VideoPlayerUiState(
            isLoading = true, isError = false, errorMessage = null, isSeamlessHandoff = false
        ), onRetry = {}, onLifecycleStop = {}, onLifecycleStart = {})
    }
}

@Preview(showBackground = true, widthDp = 400, heightDp = 225)
@Composable
fun VideoPlayerWidgetPreview_Error() {
    PiaSampleTheme(dynamicColor = false) {
        VideoPlayerWidget(
            exoPlayer = null, state = VideoPlayerUiState(
            isLoading = false,
            isError = true,
            errorMessage = "Unable to verify Play Integrity token.",
            isSeamlessHandoff = false
        ), onRetry = {}, onLifecycleStop = {}, onLifecycleStart = {})
    }
}

@Preview(showBackground = true, widthDp = 400, heightDp = 225)
@Composable
fun VideoPlayerWidgetPreview_Ready() {
    PiaSampleTheme(dynamicColor = false) {
        VideoPlayerWidget(
            exoPlayer = null, state = VideoPlayerUiState(
            isLoading = false,
            isError = false,
            isPlaying = false,
            errorMessage = null,
            isSeamlessHandoff = false
        ), onRetry = {}, onLifecycleStop = {}, onLifecycleStart = {})
    }
}