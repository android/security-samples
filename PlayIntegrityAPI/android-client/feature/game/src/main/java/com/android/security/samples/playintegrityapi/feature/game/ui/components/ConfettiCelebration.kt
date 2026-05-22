package com.android.security.samples.playintegrityapi.feature.game.ui.components

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.tooling.preview.Preview
import com.android.security.samples.playintegrityapi.core.ui.theme.PiaSampleTheme
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * Control Panel for the Confetti Celebration.
 * Adjust these values to change how the celebration looks, feels, and behaves.
 */
private object ConfettiConfig {
    // --- Orchestration & Timing ---
    /** How long (in ms) confetti rains from the top of the screen. */
    const val RAIN_DURATION_MS = 3000L

    /** How often (in ms) a new batch of confetti drops during the rain phase. */
    const val RAIN_SPAWN_INTERVAL_MS = 40L

    /** Number of pieces dropped per interval. Higher = denser rain. */
    const val RAIN_PIECES_PER_TICK = 5

    /** Delay (in ms) before the bottom cannons fire their bursts. */
    const val CANNON_FIRE_DELAY_MS = 800L

    /** Number of pieces fired from EACH cannon (left and right). */
    const val CANNON_PARTICLE_COUNT = 70

    /** Fallback timeout (in ms) to completely stop the animation loop. */
    const val MAX_ANIMATION_DURATION_MS = 4000L

    // --- Physics Environment ---
    /** Friction. 1.0 = no drag. Lower = pieces slow down faster horizontally. */
    const val AIR_DRAG = 0.96f

    /** Downward pull added every frame. */
    const val GRAVITY = 0.0006f

    /** Maximum falling speed. Prevents confetti from dropping like heavy rocks. */
    const val TERMINAL_VELOCITY = 0.005f

    /** How violently the pieces sway side-to-side as they fall (leaf flutter effect). */
    const val FLUTTER_DRIFT_AMPLITUDE = 0.0015f

    /** Y-coordinate threshold to delete pieces (0.0 = top, 1.0 = bottom edge). */
    const val DESPAWN_Y_THRESHOLD = 1.2f

    // --- Cannon Mechanics ---
    /** Minimum explosive speed of pieces shot from the bottom cannons. */
    const val CANNON_BASE_SPEED = 0.015f

    /** Additional random speed added to create a spread effect. */
    const val CANNON_SPEED_VARIANCE = 0.035f

    // --- Sizing ---
    /** Minimum width of a confetti piece. */
    const val MIN_WIDTH = 16f

    /** Minimum height of a confetti piece. */
    const val MIN_HEIGHT = 16f
}

data class ConfettiPiece(
    val x: Float,
    val y: Float,
    val vx: Float,
    val vy: Float,
    val color: Color,
    val width: Float,
    val height: Float,
    val rotation: Float,
    val rotationSpeed: Float,
    val wobble: Float,
    val wobbleSpeed: Float
)

@Composable
fun ConfettiCelebration(
    modifier: Modifier = Modifier
) {
    val view = LocalView.current
    var pieces by remember { mutableStateOf(emptyList<ConfettiPiece>()) }

    fun randomVibrantColor(): Color {
        val hue = Random.nextFloat() * 360f
        val sat = Random.nextFloat() * 0.4f + 0.6f // 60-100% saturation
        val light = Random.nextFloat() * 0.2f + 0.5f // 50-70% lightness
        return Color.hsl(hue, sat, light)
    }

    fun createTopConfetti(): ConfettiPiece {
        return ConfettiPiece(
            x = Random.nextFloat(),
            y = -0.1f,
            vx = Random.nextFloat() * 0.004f - 0.002f,
            vy = Random.nextFloat() * 0.002f + 0.002f,
            color = randomVibrantColor(),
            width = Random.nextFloat() * 16f + ConfettiConfig.MIN_WIDTH,
            height = Random.nextFloat() * 24f + ConfettiConfig.MIN_HEIGHT,
            rotation = Random.nextFloat() * 360f,
            rotationSpeed = Random.nextFloat() * 10f - 5f,
            wobble = Random.nextFloat() * 2f * PI.toFloat(),
            wobbleSpeed = Random.nextFloat() * 0.1f + 0.05f
        )
    }

    fun createCannonConfetti(isLeft: Boolean): ConfettiPiece {
        // Left cannon aims Up-Right (-PI/3), Right cannon aims Up-Left (-2PI/3)
        val baseAngle = if (isLeft) -PI / 3 else -2 * PI / 3
        val spread = PI / 6
        val finalAngle = baseAngle + (Random.nextFloat() * spread - spread / 2)
        val speed =
            Random.nextFloat() * ConfettiConfig.CANNON_SPEED_VARIANCE + ConfettiConfig.CANNON_BASE_SPEED

        return ConfettiPiece(
            x = if (isLeft) -0.05f else 1.05f, // Start slightly outside the bottom corners
            y = 1.05f,
            vx = cos(finalAngle).toFloat() * speed,
            vy = sin(finalAngle).toFloat() * speed,
            color = randomVibrantColor(),
            width = Random.nextFloat() * 18f + ConfettiConfig.MIN_WIDTH,
            height = Random.nextFloat() * 26f + ConfettiConfig.MIN_HEIGHT,
            rotation = Random.nextFloat() * 360f,
            rotationSpeed = Random.nextFloat() * 20f - 10f,
            wobble = Random.nextFloat() * 2f * PI.toFloat(),
            wobbleSpeed = Random.nextFloat() * 0.15f + 0.05f
        )
    }

    LaunchedEffect(Unit) {
        val startTime = withFrameMillis { it }
        var lastTime = startTime
        var nextTopSpawn = startTime
        var cannonsFired = false

        var currentPieces = listOf<ConfettiPiece>()

        while (true) {
            withFrameMillis { time ->
                val elapsed = time - startTime
                // Normalize step to prevent massive jumps on dropped frames
                val dt = ((time - lastTime) / 16f).coerceIn(0f, 2f)
                lastTime = time

                val newPieces = currentPieces.toMutableList()

                // Phase 1: Rain from the top
                if (elapsed < ConfettiConfig.RAIN_DURATION_MS && time > nextTopSpawn) {
                    for (i in 0 until ConfettiConfig.RAIN_PIECES_PER_TICK) {
                        newPieces.add(createTopConfetti())
                    }
                    nextTopSpawn = time + ConfettiConfig.RAIN_SPAWN_INTERVAL_MS
                }

                // Phase 2: Fire cannons from the bottom
                if (elapsed > ConfettiConfig.CANNON_FIRE_DELAY_MS && !cannonsFired) {
                    view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                    for (i in 0 until ConfettiConfig.CANNON_PARTICLE_COUNT) newPieces.add(
                        createCannonConfetti(isLeft = true)
                    )
                    for (i in 0 until ConfettiConfig.CANNON_PARTICLE_COUNT) newPieces.add(
                        createCannonConfetti(isLeft = false)
                    )
                    cannonsFired = true
                }

                // Physics Update Loop
                currentPieces = newPieces.mapNotNull { p ->
                    // Air resistance (friction)
                    val newVx = p.vx * ConfettiConfig.AIR_DRAG
                    // Gravity pulling down
                    var newVy = p.vy * ConfettiConfig.AIR_DRAG + (ConfettiConfig.GRAVITY * dt)

                    // Terminal velocity limit
                    if (newVy > ConfettiConfig.TERMINAL_VELOCITY) newVy =
                        ConfettiConfig.TERMINAL_VELOCITY

                    // Leaf-like flutter drift side-to-side
                    val drift = sin(p.wobble) * ConfettiConfig.FLUTTER_DRIFT_AMPLITUDE * dt

                    val newX = p.x + (newVx + drift) * dt
                    val newY = p.y + newVy * dt

                    // Keep the piece if it hasn't fallen far below the screen bounds
                    if (newY < ConfettiConfig.DESPAWN_Y_THRESHOLD) {
                        p.copy(
                            x = newX,
                            y = newY,
                            vx = newVx,
                            vy = newVy,
                            rotation = p.rotation + p.rotationSpeed * dt,
                            wobble = p.wobble + p.wobbleSpeed * dt
                        )
                    } else null
                }

                pieces = currentPieces
            }

            // End the coroutine when the show is over and all confetti has fallen
            if (withFrameMillis { it } - startTime > ConfettiConfig.MAX_ANIMATION_DURATION_MS && pieces.isEmpty()) {
                break
            }
        }
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        for (p in pieces) {
            withTransform({
                translate(p.x * w, p.y * h)
                rotate(p.rotation)
                // 3D tumble effect using cosine of the wobble scaling the X axis
                scale(scaleX = cos(p.wobble), scaleY = 1f)
            }) {
                drawRect(
                    color = p.color,
                    topLeft = Offset(-p.width / 2f, -p.height / 2f),
                    size = Size(p.width, p.height)
                )
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFFFFFF, widthDp = 320, heightDp = 400)
@Composable
private fun ConfettiCelebrationPreview() {
    PiaSampleTheme(dynamicColor = false) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            ConfettiCelebration()
        }
    }
}