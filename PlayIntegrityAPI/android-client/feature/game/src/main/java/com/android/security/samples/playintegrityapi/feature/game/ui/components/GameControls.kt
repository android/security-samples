package com.android.security.samples.playintegrityapi.feature.game.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.android.security.samples.playintegrityapi.core.ui.components.LoadableButton
import com.android.security.samples.playintegrityapi.core.ui.theme.PiaSampleTheme
import com.android.security.samples.playintegrityapi.feature.game.R
import com.android.security.samples.playintegrityapi.feature.game.ui.GameState
import com.android.security.samples.playintegrityapi.feature.game.ui.ScoreTier

@Composable
fun GameControls(
    state: GameState,
    onInitializeClick: () -> Unit,
    onStartClick: () -> Unit,
    onStopClick: () -> Unit,
    onResetClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()

    // When the button acts as an interrupt/error/stop action, we tint it red.
    val isDestructiveAction = state is GameState.Playing || state is GameState.Submitting || state is GameState.InitError

    // Use container colors in dark mode for a deeper, less glaring button appearance
    val buttonColor = if (isDestructiveAction) {
        if (isDark) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.error
    } else {
        if (isDark) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.primary
    }

    val textColor = if (isDestructiveAction) {
        if (isDark) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onError
    } else {
        if (isDark) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onPrimary
    }

    val buttonText = when (state) {
        GameState.Idle -> stringResource(R.string.game_btn_start_secure_session)
        GameState.Initializing -> "" // Text doesn't matter, spinner will show
        is GameState.Ready -> stringResource(R.string.game_btn_start)
        is GameState.Playing -> stringResource(R.string.game_btn_stop)
        is GameState.Submitting -> "" // Text doesn't matter, spinner will show
        is GameState.Result -> if (state.isVerified) {
            stringResource(R.string.game_btn_play_again)
        } else {
            stringResource(R.string.game_btn_try_again)
        }
        is GameState.InitError -> stringResource(R.string.game_btn_try_again)
    }

    val isLoading = state is GameState.Initializing || state is GameState.Submitting

    LoadableButton(
        onClick = {
            when (state) {
                GameState.Idle -> onInitializeClick()
                is GameState.Ready -> onStartClick()
                is GameState.Playing -> onStopClick()
                is GameState.Result -> onResetClick()
                is GameState.InitError -> onInitializeClick()
                else -> {}
            }
        },
        isLoading = isLoading,
        containerColor = buttonColor,
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Text(
            text = buttonText,
            color = textColor,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

@Preview(name = "Light", showBackground = true, widthDp = 320)
@Preview(name = "Dark", showBackground = true, widthDp = 320, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun GameControlsPreview_Idle() {
    PiaSampleTheme(dynamicColor = false) {
        Box(modifier = Modifier
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)) {
            GameControls(
                state = GameState.Idle,
                onInitializeClick = {}, onStartClick = {}, onStopClick = {}, onResetClick = {}
            )
        }
    }
}

@Preview(name = "Light", showBackground = true, widthDp = 320)
@Preview(name = "Dark", showBackground = true, widthDp = 320, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun GameControlsPreview_Initializing() {
    PiaSampleTheme(dynamicColor = false) {
        Box(modifier = Modifier
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)) {
            GameControls(
                state = GameState.Initializing,
                onInitializeClick = {}, onStartClick = {}, onStopClick = {}, onResetClick = {}
            )
        }
    }
}

@Preview(name = "Light", showBackground = true, widthDp = 320)
@Preview(name = "Dark", showBackground = true, widthDp = 320, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun GameControlsPreview_Ready() {
    PiaSampleTheme(dynamicColor = false) {
        Box(modifier = Modifier
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)) {
            GameControls(
                state = GameState.Ready(targetTime = 5.0),
                onInitializeClick = {}, onStartClick = {}, onStopClick = {}, onResetClick = {}
            )
        }
    }
}

@Preview(name = "Light", showBackground = true, widthDp = 320)
@Preview(name = "Dark", showBackground = true, widthDp = 320, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun GameControlsPreview_Playing() {
    PiaSampleTheme(dynamicColor = false) {
        Box(modifier = Modifier
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)) {
            GameControls(
                state = GameState.Playing(targetTime = 5.0),
                onInitializeClick = {}, onStartClick = {}, onStopClick = {}, onResetClick = {}
            )
        }
    }
}

@Preview(name = "Light", showBackground = true, widthDp = 320)
@Preview(name = "Dark", showBackground = true, widthDp = 320, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun GameControlsPreview_Submitting() {
    PiaSampleTheme(dynamicColor = false) {
        Box(modifier = Modifier
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)) {
            GameControls(
                state = GameState.Submitting(targetTime = 5.0, actualTime = 5.1),
                onInitializeClick = {}, onStartClick = {}, onStopClick = {}, onResetClick = {}
            )
        }
    }
}

@Preview(name = "Light", showBackground = true, widthDp = 320)
@Preview(name = "Dark", showBackground = true, widthDp = 320, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun GameControlsPreview_Result() {
    PiaSampleTheme(dynamicColor = false) {
        Box(modifier = Modifier
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)) {
            GameControls(
                state = GameState.Result(
                    targetTime = 5.0,
                    actualTime = 5.0,
                    difference = 0.0,
                    tier = ScoreTier.PERFECT,
                    isVerified = true
                ),
                onInitializeClick = {}, onStartClick = {}, onStopClick = {}, onResetClick = {}
            )
        }
    }
}

@Preview(name = "Light", showBackground = true, widthDp = 320)
@Preview(name = "Dark", showBackground = true, widthDp = 320, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun GameControlsPreview_Result_Rejected() {
    PiaSampleTheme(dynamicColor = false) {
        Box(modifier = Modifier
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)) {
            GameControls(
                state = GameState.Result(
                    targetTime = 5.0,
                    actualTime = 5.0,
                    difference = 0.0,
                    tier = ScoreTier.PERFECT,
                    isVerified = false
                ),
                onInitializeClick = {}, onStartClick = {}, onStopClick = {}, onResetClick = {}
            )
        }
    }
}

@Preview(name = "Light", showBackground = true, widthDp = 320)
@Preview(name = "Dark", showBackground = true, widthDp = 320, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun GameControlsPreview_Error() {
    PiaSampleTheme(dynamicColor = false) {
        Box(modifier = Modifier
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)) {
            GameControls(
                state = GameState.InitError("Environment comprised."),
                onInitializeClick = {}, onStartClick = {}, onStopClick = {}, onResetClick = {}
            )
        }
    }
}