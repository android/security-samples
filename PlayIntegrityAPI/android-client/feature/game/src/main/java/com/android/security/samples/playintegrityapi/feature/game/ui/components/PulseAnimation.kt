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

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * Control Panel for the Background Particle Animation.
 */
private object PulseConfig {
    // --- Orchestration ---
    /** How long (in ms) each animation theme (Spiral, Wave, Orbit) lasts before switching. */
    const val THEME_DURATION_MS = 6000L
    /** How long (in ms) the morphing transition between themes takes. */
    const val MORPH_DURATION_MS = 2500L
    /** How often (in ms) the haptic "heartbeat" triggers. */
    const val HAPTIC_INTERVAL_MS = 400L

    // --- Particle Spawning & Lifespan ---
    /** Minimum number of particles spawned per tick. */
    const val SPAWN_COUNT_MIN = 2
    /** Maximum number of particles spawned per tick. */
    const val SPAWN_COUNT_MAX = 3
    /** Minimum delay (in ms) between spawn ticks. Lower = denser particle field. */
    const val SPAWN_DELAY_MIN_MS = 30L
    /** Maximum delay (in ms) between spawn ticks. */
    const val SPAWN_DELAY_MAX_MS = 70L
    /** Minimum lifespan (in ms) of a single particle. */
    const val PARTICLE_LIFESPAN_MIN_MS = 1800L
    /** Maximum lifespan (in ms) of a single particle. */
    const val PARTICLE_LIFESPAN_MAX_MS = 2800L

    // --- Physics & Mechanics ---
    /** Base speed of the vortex wind in the SPIRAL theme. */
    const val SPIRAL_BASE_SPEED = 2f
    /** Orbital ring distances for the ORBIT theme (Inner, Mid, Outer relative scales). */
    const val ORBIT_INNER_RING = 0.35f
    const val ORBIT_MID_RING = 0.65f
    const val ORBIT_OUTER_RING = 0.90f

    // --- Sizing ---
    /** Minimum base size of a particle in pixels. */
    const val PARTICLE_BASE_SIZE = 3f
    /** Maximum random variance added to the particle size. */
    const val PARTICLE_SIZE_VARIANCE = 5f
}

data class DynamicParticle(
    val startTime: Long,
    val duration: Long,
    val maxRadius: Float,
    val baseAngle: Float,
    val spiralFactor: Float,
    val orbitalRingScale: Float,
    val noiseSeed: Float,
    val size: Float
)

enum class AnimationTheme { SPIRAL, WAVE, ORBIT }

@Composable
fun PulseAnimation() {
    var frameTime by remember { mutableLongStateOf(0L) }
    var particles by remember { mutableStateOf(listOf<DynamicParticle>()) }
    val view = LocalView.current

    var currentTheme by remember { mutableStateOf(AnimationTheme.SPIRAL) }
    var nextTheme by remember { mutableStateOf(AnimationTheme.WAVE) }
    var transitionProgress by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(Unit) {
        var lastSpawnTime = 0L
        var nextSpawnDelay = 0L
        var lastHapticTime = 0L
        var themeStartTime = withFrameMillis { it }

        while (true) {
            withFrameMillis { time ->
                frameTime = time
                var changed = false
                val currentParticles = particles.toMutableList()

                // 1. Theme Orchestration
                val elapsedThemeTime = time - themeStartTime
                if (elapsedThemeTime < PulseConfig.MORPH_DURATION_MS) {
                    val linearProgress = elapsedThemeTime.toFloat() / PulseConfig.MORPH_DURATION_MS
                    transitionProgress = (1f - cos(linearProgress * PI.toFloat())) / 2f
                } else {
                    transitionProgress = 1f
                }

                if (elapsedThemeTime >= PulseConfig.THEME_DURATION_MS) {
                    currentTheme = nextTheme
                    nextTheme = when (currentTheme) {
                        AnimationTheme.SPIRAL -> AnimationTheme.WAVE
                        AnimationTheme.WAVE -> AnimationTheme.ORBIT
                        AnimationTheme.ORBIT -> AnimationTheme.SPIRAL
                    }
                    themeStartTime = time
                    transitionProgress = 0f
                }

                // 2. Particle Spawner
                if (lastSpawnTime == 0L || time - lastSpawnTime > nextSpawnDelay) {
                    val spawnCount = (PulseConfig.SPAWN_COUNT_MIN..PulseConfig.SPAWN_COUNT_MAX).random()
                    for (i in 0 until spawnCount) {
                        currentParticles.add(
                            DynamicParticle(
                                startTime = time,
                                duration = (PulseConfig.PARTICLE_LIFESPAN_MIN_MS..PulseConfig.PARTICLE_LIFESPAN_MAX_MS).random(),
                                maxRadius = Random.nextFloat() * 0.45f + 0.75f,
                                baseAngle = Random.nextFloat() * (2f * PI.toFloat()),
                                spiralFactor = Random.nextFloat() * 3f + PulseConfig.SPIRAL_BASE_SPEED,
                                orbitalRingScale = when (Random.nextFloat()) {
                                    in 0.0f..0.35f -> PulseConfig.ORBIT_INNER_RING
                                    in 0.35f..0.75f -> PulseConfig.ORBIT_MID_RING
                                    else -> PulseConfig.ORBIT_OUTER_RING
                                },
                                noiseSeed = Random.nextFloat() * 100f,
                                size = Random.nextFloat() * PulseConfig.PARTICLE_SIZE_VARIANCE + PulseConfig.PARTICLE_BASE_SIZE
                            )
                        )
                    }
                    lastSpawnTime = time
                    nextSpawnDelay = (PulseConfig.SPAWN_DELAY_MIN_MS..PulseConfig.SPAWN_DELAY_MAX_MS).random()
                    changed = true
                }

                // 3. Heartbeat Haptics
                if (time - lastHapticTime > PulseConfig.HAPTIC_INTERVAL_MS) {
                    view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                    lastHapticTime = time
                }

                // 4. Garbage Collection
                val initialCount = currentParticles.size
                currentParticles.removeAll { time - it.startTime > it.duration }
                if (initialCount != currentParticles.size) changed = true

                if (changed) particles = currentParticles
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val primaryColor = Color(0xFF1A73E8)
            val maxTargetDistance = size.minDimension / 2f

            for (particle in particles) {
                val progress = ((frameTime - particle.startTime) / particle.duration.toFloat()).coerceIn(0f, 1f)
                val easedProgress = 1f - (1f - progress) * (1f - progress)

                val spiralDistance = maxTargetDistance * particle.maxRadius * easedProgress
                val spiralAngle = particle.baseAngle + (progress * particle.spiralFactor * PI.toFloat())

                val waveDistance = maxTargetDistance * particle.maxRadius * progress
                val waveAngle = particle.baseAngle

                val orbitalDistance = maxTargetDistance * particle.orbitalRingScale
                val rotationSpeed = 3.5f / (particle.orbitalRingScale + 0.2f)
                val orbitalAngle = particle.baseAngle + (progress * rotationSpeed * PI.toFloat())

                // Interpolation
                val (x1, y1) = getCoordinates(currentTheme, spiralDistance, spiralAngle, waveDistance, waveAngle, orbitalDistance, orbitalAngle)
                val (x2, y2) = getCoordinates(nextTheme, spiralDistance, spiralAngle, waveDistance, waveAngle, orbitalDistance, orbitalAngle)

                val finalLocalX = x1 + (x2 - x1) * transitionProgress
                val finalLocalY = y1 + (y2 - y1) * transitionProgress

                // Organic Noise Injection
                val noiseFactor = sin(frameTime / 180f + particle.noiseSeed) * 8.dp.toPx() * (1f - progress)
                val finalX = center.x + finalLocalX + noiseFactor * cos(particle.baseAngle)
                val finalY = center.y + finalLocalY + noiseFactor * sin(particle.baseAngle)

                val alpha = (1f - progress).coerceIn(0f, 1f)
                val sizeRadius = particle.size * (1f - progress * 0.4f)

                drawCircle(
                    color = primaryColor.copy(alpha = alpha),
                    radius = sizeRadius,
                    center = Offset(finalX, finalY)
                )
            }
        }

        // Foreground Planetary Core synchronized with the breathing math
        val centerPulse = (sin(frameTime / 120f) + 1f) / 2f
        val pulseScale = 1f + (0.3f * centerPulse)

        PlanetaryCore(
            modifier = Modifier
                .size(36.dp)
                .scale(pulseScale)
        )
    }
}

private fun getCoordinates(
    theme: AnimationTheme,
    spiralDist: Float, spiralAng: Float,
    waveDist: Float, waveAng: Float,
    orbitDist: Float, orbitAng: Float
): Pair<Float, Float> {
    return when (theme) {
        AnimationTheme.SPIRAL -> Pair(spiralDist * cos(spiralAng), spiralDist * sin(spiralAng))
        AnimationTheme.WAVE -> Pair(waveDist * cos(waveAng), waveDist * sin(waveAng))
        AnimationTheme.ORBIT -> Pair(orbitDist * cos(orbitAng), orbitDist * sin(orbitAng))
    }
}