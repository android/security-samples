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
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.android.security.samples.playintegrityapi.core.ui.theme.PiaSampleTheme
import com.android.security.samples.playintegrityapi.feature.game.R
import com.android.security.samples.playintegrityapi.feature.game.ui.components.GameArena
import com.android.security.samples.playintegrityapi.feature.game.ui.components.GameControls
import com.android.security.samples.playintegrityapi.feature.game.ui.components.LeaderboardStatusCard
import com.android.security.samples.playintegrityapi.feature.game.ui.components.SecurityChecklist

@Composable
fun GameRoute(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: GameViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.validateEnvironmentOnResume()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    GameScreen(
        uiState = uiState,
        onBackClick = onBackClick,
        onInitializeClick = viewModel::initializeSession,
        onStartClick = viewModel::startGame,
        onStopClick = viewModel::stopGame,
        onResetClick = viewModel::resetGame,
        onRemediateClick = {
            (context as? Activity)?.let { viewModel.triggerRemediationDialog(it) }
        },
        modifier = modifier
    )
}

enum class BottomCardState {
    HIDDEN, CHECKLIST, LEADERBOARD
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameScreen(
    uiState: GameUiState,
    onBackClick: () -> Unit,
    onInitializeClick: () -> Unit,
    onStartClick: () -> Unit,
    onStopClick: () -> Unit,
    onResetClick: () -> Unit,
    onRemediateClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Text(
                            text = stringResource(id = R.string.game_top_bar_title),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
                )
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                    thickness = 1.dp
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(vertical = 16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .animateContentSize(animationSpec = tween(durationMillis = 350))
            ) {
                Text(
                    modifier = Modifier.padding(horizontal = 24.dp),
                    text = stringResource(id = R.string.game_header_title),
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.sp
                    ),
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    modifier = Modifier.padding(horizontal = 24.dp),
                    text = stringResource(id = R.string.game_header_subtitle),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(28.dp))
                GameArena(
                    modifier = Modifier
                        .padding(horizontal = 24.dp)
                        .shadow(
                            elevation = 4.dp,
                            shape = MaterialTheme.shapes.large
                        ), state = uiState.gameState
                )

                // Define which component should be crossfaded in
                val bottomCardState = when (uiState.gameState) {
                    is GameState.Result -> BottomCardState.LEADERBOARD
                    is GameState.Ready, is GameState.Playing, is GameState.Submitting -> BottomCardState.CHECKLIST
                    else -> BottomCardState.HIDDEN
                }

                var retainedIsAccepted by remember { mutableStateOf(true) }
                var retainedUiState by remember { mutableStateOf(uiState) }

                LaunchedEffect(uiState.gameState) {
                    if (uiState.gameState is GameState.Result) {
                        retainedIsAccepted = uiState.gameState.isVerified
                        retainedUiState = uiState
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .defaultMinSize(minHeight = 180.dp)
                        .verticalScroll(rememberScrollState()),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Crossfade(
                        targetState = bottomCardState,
                        animationSpec = tween(400),
                        label = "checklist_to_leaderboard"
                    ) { cardState ->
                        when (cardState) {
                            BottomCardState.LEADERBOARD -> {
                                LeaderboardStatusCard(
                                    isAccepted = retainedIsAccepted,
                                    uiState = retainedUiState,
                                    onRemediateClick = onRemediateClick
                                )
                            }

                            BottomCardState.CHECKLIST -> {
                                SecurityChecklist(uiState = uiState)
                            }

                            BottomCardState.HIDDEN -> {
                                Box(modifier = Modifier.fillMaxWidth())
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            GameControls(
                modifier = Modifier.padding(horizontal = 24.dp),
                state = uiState.gameState,
                onInitializeClick = onInitializeClick,
                onStartClick = onStartClick,
                onStopClick = onStopClick,
                onResetClick = onResetClick
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
private fun GameScreenPreview_Idle() {
    PiaSampleTheme(dynamicColor = false) {
        GameScreen(
            uiState = GameUiState(gameState = GameState.Idle),
            onBackClick = {},
            onInitializeClick = {},
            onStartClick = {},
            onStopClick = {},
            onResetClick = {},
            onRemediateClick = {}
        )
    }
}