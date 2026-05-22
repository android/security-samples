package com.android.security.samples.playintegrityapi.feature.game.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.android.security.samples.playintegrityapi.core.ui.theme.PiaSampleTheme
import com.android.security.samples.playintegrityapi.feature.game.R
import com.android.security.samples.playintegrityapi.feature.game.ui.GameState
import com.android.security.samples.playintegrityapi.feature.game.ui.GameUiState

@Composable
fun SecurityChecklist(uiState: GameUiState) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SecurityCheckItem(
            text = stringResource(
                if (uiState.checkScreenCapture) R.string.game_checklist_screen_capture
                else R.string.game_checklist_screen_capture_fail
            ),
            isPassed = uiState.checkScreenCapture
        )
        SecurityCheckItem(
            text = stringResource(
                if (uiState.checkAccessibility) R.string.game_checklist_accessibility
                else R.string.game_checklist_accessibility_fail
            ),
            isPassed = uiState.checkAccessibility
        )
        SecurityCheckItem(
            text = stringResource(
                if (uiState.checkPlayProtect) R.string.game_checklist_play_protect
                else R.string.game_checklist_play_protect_fail
            ),
            isPassed = uiState.checkPlayProtect
        )
    }
}

@Composable
fun SecurityCheckItem(text: String, isPassed: Boolean) {
    val isDark = isSystemInDarkTheme()

    // Dynamic color maps
    val themeColor = if (isPassed) {
        MaterialTheme.colorScheme.tertiary
    } else {
        MaterialTheme.colorScheme.error
    }

    val backgroundColor = if (isPassed) {
        if (isDark) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
    } else {
        // Keeps the red failure background soft and visually appealing in dark mode
        if (isDark) MaterialTheme.colorScheme.error.copy(alpha = 0.12f)
        else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f)
    }

    val textColor = if (isPassed) {
        MaterialTheme.colorScheme.onSurface
    } else {
        if (isDark) MaterialTheme.colorScheme.error
        else MaterialTheme.colorScheme.onErrorContainer
    }

    val iconVector = if (isPassed) {
        Icons.Default.Check
    } else {
        Icons.Default.Clear
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .height(IntrinsicSize.Min),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(4.dp)
                .fillMaxHeight()
                .background(themeColor)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Icon(
            imageVector = iconVector,
            contentDescription = null,
            tint = themeColor,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = textColor,
            modifier = Modifier
                .padding(vertical = 16.dp)
                .weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Preview(name = "Light", showBackground = true)
@Preview(name = "Dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun SecurityCheckItemPreview_Passed() {
    PiaSampleTheme(dynamicColor = false) {
        Box(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp)
        ) {
            SecurityCheckItem(
                text = "No risky apps with screen capture detected",
                isPassed = true
            )
        }
    }
}

@Preview(name = "Light", showBackground = true)
@Preview(name = "Dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun SecurityCheckItemPreview_Failed() {
    PiaSampleTheme(dynamicColor = false) {
        Box(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp)
        ) {
            SecurityCheckItem(
                text = "Screen recording detected. Sensitive data hidden.",
                isPassed = false
            )
        }
    }
}

@Preview(name = "Light", showBackground = true)
@Preview(name = "Dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun SecurityChecklistPreview_AllPassed() {
    PiaSampleTheme(dynamicColor = false) {
        Box(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp)
        ) {
            SecurityChecklist(
                uiState = GameUiState(
                    gameState = GameState.Idle,
                    checkScreenCapture = true,
                    checkAccessibility = true,
                    checkPlayProtect = true
                )
            )
        }
    }
}

@Preview(name = "Light", showBackground = true)
@Preview(name = "Dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun SecurityChecklistPreview_MixedResults() {
    PiaSampleTheme(dynamicColor = false) {
        Box(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp)
        ) {
            SecurityChecklist(
                uiState = GameUiState(
                    gameState = GameState.Idle,
                    checkScreenCapture = false,
                    checkAccessibility = true,
                    checkPlayProtect = false
                )
            )
        }
    }
}