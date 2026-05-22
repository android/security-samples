package com.android.security.samples.playintegrityapi.feature.game.ui.components

import android.content.res.Configuration
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.security.samples.playintegrityapi.core.ui.theme.PiaSampleTheme
import com.android.security.samples.playintegrityapi.feature.game.R
import com.android.security.samples.playintegrityapi.feature.game.ui.GameState
import com.android.security.samples.playintegrityapi.feature.game.ui.ScoreTier

@Composable
fun GameArena(
    state: GameState,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(MaterialTheme.shapes.large)
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        AnimatedContent(
            targetState = state,
            label = "arena_content",
            contentAlignment = Alignment.Center,
            transitionSpec = {
                val exitDuration = 200
                val enterDuration = 250

                val exitTransition = fadeOut(
                    animationSpec = tween(exitDuration, easing = FastOutLinearInEasing)
                ) + scaleOut(
                    targetScale = 0.6f,
                    animationSpec = tween(exitDuration, easing = FastOutLinearInEasing)
                )

                val enterTransition = fadeIn(
                    animationSpec = tween(enterDuration, exitDuration, LinearOutSlowInEasing)
                ) + scaleIn(
                    initialScale = 0.6f,
                    animationSpec = tween(enterDuration, exitDuration, LinearOutSlowInEasing)
                )

                enterTransition togetherWith exitTransition using SizeTransform(clip = false)
            }
        ) { targetState ->
            when (targetState) {
                is GameState.Idle, is GameState.Initializing -> {
                    val infiniteTransition = rememberInfiniteTransition(label = "idle_pulse")

                    val scaleAnim by infiniteTransition.animateFloat(
                        initialValue = 0.8f,
                        targetValue = 1.2f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(800, easing = LinearOutSlowInEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "idle_scale"
                    )

                    val alphaAnim by infiniteTransition.animateFloat(
                        initialValue = 0.6f,
                        targetValue = 1.0f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(800, easing = LinearOutSlowInEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "idle_alpha"
                    )

                    PlanetaryCore(
                        modifier = Modifier
                            .size(36.dp)
                            .scale(scaleAnim)
                            .alpha(alphaAnim)
                    )
                }

                is GameState.Ready -> {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = stringResource(R.string.game_arena_target_label),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = String.format("%.2fs", targetState.targetTime),
                            style = MaterialTheme.typography.headlineLarge.copy(fontSize = 64.sp),
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                is GameState.Playing -> {
                    PulseAnimation()
                }

                is GameState.Submitting -> {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = stringResource(R.string.game_arena_verifying),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = stringResource(
                                R.string.game_arena_target_value,
                                targetState.targetTime
                            ),
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = stringResource(
                                R.string.game_arena_stopped_at,
                                targetState.actualTime
                            ),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                is GameState.Result -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        if (targetState.tier == ScoreTier.PERFECT || targetState.tier == ScoreTier.NEAR_PERFECT) {
                            ConfettiCelebration()
                        }

                        val (labelText, labelColor) = when (targetState.tier) {
                            ScoreTier.PERFECT -> stringResource(R.string.game_arena_result_perfect) to MaterialTheme.colorScheme.tertiary
                            ScoreTier.NEAR_PERFECT -> stringResource(R.string.game_arena_result_near_perfect) to MaterialTheme.colorScheme.tertiary
                            ScoreTier.NEAR_MISS -> stringResource(R.string.game_arena_result_near_miss) to Color(
                                0xFFE67E22
                            )

                            ScoreTier.MISSED -> stringResource(R.string.game_arena_result_missed) to MaterialTheme.colorScheme.error
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = labelText,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = labelColor
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = stringResource(
                                    R.string.game_arena_target_value,
                                    targetState.targetTime
                                ),
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                text = stringResource(
                                    R.string.game_arena_actual_value,
                                    targetState.actualTime
                                ),
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = stringResource(
                                    R.string.game_arena_diff_value,
                                    targetState.difference
                                ),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                is GameState.InitError -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Initialization Error",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Initialization Failed",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = targetState.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

// --- Previews ---

@Preview(name = "Light", showBackground = true, widthDp = 320)
@Preview(name = "Dark", showBackground = true, widthDp = 320, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun GameArenaPreview_Error() {
    PiaSampleTheme(dynamicColor = false) {
        Box(modifier = Modifier.background(MaterialTheme.colorScheme.background).padding(16.dp)) {
            GameArena(
                state = GameState.InitError(
                    message = "Cheat toggling detected: Environment compromised at interval 3.14s."
                )
            )
        }
    }
}

@Preview(name = "Light", showBackground = true, widthDp = 320)
@Preview(name = "Dark", showBackground = true, widthDp = 320, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun GameArenaPreview_Idle() {
    PiaSampleTheme(dynamicColor = false) {
        Box(modifier = Modifier.background(MaterialTheme.colorScheme.background).padding(16.dp)) {
            GameArena(state = GameState.Idle)
        }
    }
}

@Preview(name = "Light", showBackground = true, widthDp = 320)
@Preview(name = "Dark", showBackground = true, widthDp = 320, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun GameArenaPreview_Ready() {
    PiaSampleTheme(dynamicColor = false) {
        Box(modifier = Modifier.background(MaterialTheme.colorScheme.background).padding(16.dp)) {
            GameArena(state = GameState.Ready(targetTime = 5.00))
        }
    }
}

@Preview(name = "Light", showBackground = true, widthDp = 320)
@Preview(name = "Dark", showBackground = true, widthDp = 320, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun GameArenaPreview_Playing() {
    PiaSampleTheme(dynamicColor = false) {
        Box(modifier = Modifier.background(MaterialTheme.colorScheme.background).padding(16.dp)) {
            // Will render the pulsing dot animation
            GameArena(state = GameState.Playing(targetTime = 5.00))
        }
    }
}

@Preview(name = "Light", showBackground = true, widthDp = 320)
@Preview(name = "Dark", showBackground = true, widthDp = 320, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun GameArenaPreview_Submitting() {
    PiaSampleTheme(dynamicColor = false) {
        Box(modifier = Modifier.background(MaterialTheme.colorScheme.background).padding(16.dp)) {
            GameArena(state = GameState.Submitting(targetTime = 5.00, actualTime = 5.12))
        }
    }
}

@Preview(name = "Light", showBackground = true, widthDp = 320)
@Preview(name = "Dark", showBackground = true, widthDp = 320, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun GameArenaPreview_Result_Perfect() {
    PiaSampleTheme(dynamicColor = false) {
        Box(modifier = Modifier.background(MaterialTheme.colorScheme.background).padding(16.dp)) {
            GameArena(
                state = GameState.Result(
                    targetTime = 5.00,
                    actualTime = 5.00,
                    difference = 0.00,
                    tier = ScoreTier.PERFECT,
                    isVerified = true
                )
            )
        }
    }
}

@Preview(name = "Light", showBackground = true, widthDp = 320)
@Preview(name = "Dark", showBackground = true, widthDp = 320, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun GameArenaPreview_Result_NearMiss() {
    PiaSampleTheme(dynamicColor = false) {
        Box(modifier = Modifier.background(MaterialTheme.colorScheme.background).padding(16.dp)) {
            GameArena(
                state = GameState.Result(
                    targetTime = 5.00,
                    actualTime = 5.60,
                    difference = 0.60,
                    tier = ScoreTier.NEAR_MISS,
                    isVerified = true
                )
            )
        }
    }
}

@Preview(name = "Light", showBackground = true, widthDp = 320)
@Preview(name = "Dark", showBackground = true, widthDp = 320, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun GameArenaPreview_Result_Missed() {
    PiaSampleTheme(dynamicColor = false) {
        Box(modifier = Modifier.background(MaterialTheme.colorScheme.background).padding(16.dp)) {
            GameArena(
                state = GameState.Result(
                    targetTime = 5.00,
                    actualTime = 7.50,
                    difference = 2.50,
                    tier = ScoreTier.MISSED,
                    isVerified = true
                )
            )
        }
    }
}