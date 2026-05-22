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

package com.android.security.samples.playintegrityapi.feature.game.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.android.security.samples.playintegrityapi.core.ui.theme.PiaSampleTheme
import com.android.security.samples.playintegrityapi.feature.game.R
import com.android.security.samples.playintegrityapi.feature.game.ui.GameState
import com.android.security.samples.playintegrityapi.feature.game.ui.GameUiState

@Composable
fun LeaderboardStatusCard(
    isAccepted: Boolean,
    uiState: GameUiState,
    onRemediateClick: () -> Unit
) {
    val isDark = isSystemInDarkTheme()

    val containerColor = if (isAccepted) {
        if (isDark) MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f)
        else MaterialTheme.colorScheme.tertiaryContainer
    } else {
        if (isDark) MaterialTheme.colorScheme.error.copy(alpha = 0.15f)
        else MaterialTheme.colorScheme.errorContainer
    }

    val contentColor = if (isAccepted) {
        if (isDark) MaterialTheme.colorScheme.tertiary
        else MaterialTheme.colorScheme.onTertiaryContainer
    } else {
        if (isDark) MaterialTheme.colorScheme.error
        else MaterialTheme.colorScheme.onErrorContainer
    }

    val icon = if (isAccepted) Icons.Default.CheckCircle else Icons.Default.Warning

    val title = if (isAccepted) {
        stringResource(R.string.game_leaderboard_accepted_title)
    } else {
        stringResource(R.string.game_leaderboard_rejected_title)
    }

    val baseDescription = if (isAccepted) {
        stringResource(R.string.game_leaderboard_accepted_desc)
    } else {
        stringResource(R.string.game_leaderboard_rejected_desc)
    }

    val remediateText = stringResource(R.string.game_btn_remediate)
    val showRemediation = !isAccepted && (uiState.gameState as? GameState.Result)?.remediationToken != null

    val annotatedDescription = buildAnnotatedString {
        withStyle(style = SpanStyle(color = contentColor.copy(alpha = 0.8f))) {
            append(baseDescription)
        }

        if (showRemediation) {
            append(" ")
            val linkAnnotation = LinkAnnotation.Clickable(
                tag = "remediate_action",
                styles = TextLinkStyles(
                    style = SpanStyle(
                        color = contentColor,
                        fontWeight = FontWeight.Bold,
                        textDecoration = TextDecoration.Underline
                    )
                ),
                linkInteractionListener = {
                    onRemediateClick()
                }
            )
            withLink(linkAnnotation) {
                append(remediateText)
            }
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = containerColor,
            contentColor = contentColor
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .padding(top = 2.dp)
                    .size(48.dp)
                    .background(contentColor.copy(alpha = 0.1f), RoundedCornerShape(50)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = annotatedDescription,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Preview(name = "Light", showBackground = true)
@Preview(name = "Dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun LeaderboardStatusCardPreview_Accepted() {
    PiaSampleTheme(dynamicColor = false) {
        Box(modifier = Modifier
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)) {
            LeaderboardStatusCard(
                isAccepted = true,
                uiState = GameUiState(),
                onRemediateClick = {}
            )
        }
    }
}

@Preview(name = "Light", showBackground = true)
@Preview(name = "Dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun LeaderboardStatusCardPreview_Rejected() {
    PiaSampleTheme(dynamicColor = false) {
        Box(modifier = Modifier
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)) {
            LeaderboardStatusCard(
                isAccepted = false,
                uiState = GameUiState(checkScreenCapture = false),
                onRemediateClick = {}
            )
        }
    }
}