package com.android.security.samples.playintegrityapi.feature.game.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.android.security.samples.playintegrityapi.core.ui.theme.PiaSampleTheme
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

/**
 * Control Panel for the Planetary Core.
 * Adjust these values to change the planet's geography, lighting, and rotation.
 */
private object PlanetaryConfig {
    // --- Rotation & Orientation ---
    /** The default rotational speed of the planet. */
    const val DEFAULT_SPIN_SPEED = 0.0015f
    /** The simulated axial tilt of the planet (Earth is -23.5 degrees). */
    const val AXIAL_TILT_DEGREES = -23.5f

    // --- Sizing ---
    /** The divisor applied to the canvas's minDimension to determine the planet's radius. */
    const val CORE_RADIUS_DIVISOR = 3.6f

    // --- Geography (Continents) ---
    /** Fixed seed to ensure the planet's geography looks the same every time it renders. */
    const val GEOGRAPHY_SEED = 42
    /** The number of "continents" or landmass patches to generate. */
    const val CONTINENT_COUNT = 12
    /** The base minimum size of a landmass relative to the planet's radius. */
    const val CONTINENT_BASE_SIZE = 0.2f
    /** The maximum random variance added to a continent's size. */
    const val CONTINENT_SIZE_VARIANCE = 0.45f

    // --- Visuals & Aesthetics ---
    /** Alpha applied to the primary color to create the continent color. */
    const val CONTINENT_COLOR_ALPHA = 0.7f
    /** Maximum opacity of the continents at the center of the sphere (creates 3D fade on edges). */
    const val CONTINENT_MAX_OPACITY = 0.85f
    /** Opacity of the white cloud cover drawn over the continents. */
    const val CLOUD_OPACITY = 0.4f

    // --- Specular Lighting (Atmosphere/Gloss) ---
    /** Max opacity of the specular highlight (glass orb reflection). */
    const val SPECULAR_ALPHA = 0.35f
    /** How far off-center the specular highlight is positioned (up and left). */
    const val SPECULAR_OFFSET_FACTOR = 0.35f
    /** How large the specular highlight spreads relative to the core radius. */
    const val SPECULAR_RADIUS_FACTOR = 0.7f
}

/**
 * A reusable, standalone 3D spinning planetary core.
 */
@Composable
fun PlanetaryCore(
    modifier: Modifier = Modifier,
    spinSpeed: Float = PlanetaryConfig.DEFAULT_SPIN_SPEED
) {
    var frameTime by remember { mutableLongStateOf(0L) }

    val primaryColor = MaterialTheme.colorScheme.primary
    val primaryContainer = MaterialTheme.colorScheme.primaryContainer
    val onPrimaryContainer = MaterialTheme.colorScheme.onPrimaryContainer
    val continentColor = primaryColor.copy(alpha = PlanetaryConfig.CONTINENT_COLOR_ALPHA)

    // Drive the continuous rotation independently
    LaunchedEffect(Unit) {
        while (true) {
            withFrameMillis { time ->
                frameTime = time
            }
        }
    }

    Canvas(modifier = modifier) {
        val coreRadius = size.minDimension / PlanetaryConfig.CORE_RADIUS_DIVISOR

        // 1. Base 3D Sphere (Radial Gradient Ocean mapped to Theme)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    primaryContainer,
                    primaryColor,
                    onPrimaryContainer.copy(alpha = 0.8f)
                ),
                center = center,
                radius = coreRadius
            ),
            radius = coreRadius,
            center = center
        )

        // 2. Spinning Theme-Matched Patches (Continents and Clouds)
        val spherePath = Path().apply {
            addOval(
                Rect(
                    center.x - coreRadius,
                    center.y - coreRadius,
                    center.x + coreRadius,
                    center.y + coreRadius
                )
            )
        }

        // Clip to sphere bounds so patches don't bleed out into space
        clipPath(spherePath) {
            withTransform({
                rotate(PlanetaryConfig.AXIAL_TILT_DEGREES, center)
            }) {
                val random = kotlin.random.Random(PlanetaryConfig.GEOGRAPHY_SEED)

                for (i in 0..PlanetaryConfig.CONTINENT_COUNT) {
                    val lat = random.nextFloat() * 2f - 1f // Latitude -1 to +1
                    val initialLon = random.nextFloat() * 2f * PI.toFloat()
                    val sizeScale = random.nextFloat() * PlanetaryConfig.CONTINENT_SIZE_VARIANCE + PlanetaryConfig.CONTINENT_BASE_SIZE // Size of the landmass

                    val currentLon = initialLon + (frameTime * spinSpeed)
                    // Z represents depth. Z = 1 is front facing, Z = 0 is edge, Z = -1 is back
                    val z = cos(currentLon)

                    // Only draw if facing the camera (or slightly over the horizon)
                    if (z > -0.2f) {
                        val xOffset = sin(currentLon) * coreRadius
                        val yOffset = lat * coreRadius

                        val patchX = center.x + xOffset
                        val patchY = center.y + yOffset

                        // Perspective: Squash width at the edges to simulate 3D wrapping
                        val patchWidth = (sizeScale * coreRadius) * max(0f, z)
                        val patchHeight = sizeScale * coreRadius

                        // Fade out at the extreme edges to prevent harsh popping
                        val patchAlpha = (PlanetaryConfig.CONTINENT_MAX_OPACITY * z).coerceIn(0f, PlanetaryConfig.CONTINENT_MAX_OPACITY)

                        // Continental landmass (Themed highlight)
                        drawOval(
                            color = continentColor.copy(alpha = patchAlpha),
                            topLeft = Offset(patchX - patchWidth, patchY - patchHeight),
                            size = Size(patchWidth * 2f, patchHeight * 2f)
                        )
                        // Cloud cover / atmospheric highlight (White over the continents)
                        drawOval(
                            color = Color.White.copy(alpha = patchAlpha * PlanetaryConfig.CLOUD_OPACITY),
                            topLeft = Offset(
                                patchX - patchWidth * 0.6f,
                                patchY - patchHeight * 0.5f
                            ),
                            size = Size(patchWidth * 1.2f, patchHeight)
                        )
                    }
                }
            }
        }

        // 3. Specular Lighting (Atmosphere / Glass orb reflection overlay)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color.White.copy(alpha = PlanetaryConfig.SPECULAR_ALPHA), Color.Transparent),
                center = Offset(
                    center.x - coreRadius * PlanetaryConfig.SPECULAR_OFFSET_FACTOR,
                    center.y - coreRadius * PlanetaryConfig.SPECULAR_OFFSET_FACTOR
                ),
                radius = coreRadius * PlanetaryConfig.SPECULAR_RADIUS_FACTOR
            ),
            radius = coreRadius,
            center = center
        )
    }
}


@Preview(showBackground = true, backgroundColor = 0xFFFFFFFF, widthDp = 200, heightDp = 200)
@Composable
private fun PlanetaryCorePreview() {
    PiaSampleTheme(dynamicColor = false) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            PlanetaryCore(modifier = Modifier.size(120.dp))
        }
    }
}