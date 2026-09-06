package io.github.windsekirun.hibiku.feature.ui.immersive

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.isActive
import kotlin.math.pow
import kotlin.math.sin

private data class DynamicWaveSpec(
    val color: Color,
    val amplitudeDp: Float,
    val wavelengthDp: Float,
    val speedMultiplier: Float,
    val phaseOffset: Float,
    val curvatureExponent: Float
)

@Composable
fun GalaxyFluidWaveProgressBar(
    progress: Float,
    onProgressChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    mainColor: Color = Color(0xFFD500F9),
    isPlaying: Boolean = true,
    inactiveTrackColor: Color = Color.White.copy(alpha = 0.25f),
    thumbBorderColor: Color = Color.White
) {
    val elapsedTimeSeconds = produceState(initialValue = 0f, key1 = isPlaying) {
        if (!isPlaying) return@produceState
        var lastFrameTimeNanos = 0L
        while (isActive) {
            androidx.compose.animation.core.withInfiniteAnimationFrameMillis { frameTimeMillis ->
                val currentFrameNanos = frameTimeMillis * 1_000_000L
                if (lastFrameTimeNanos != 0L) {
                    val deltaSeconds = (currentFrameNanos - lastFrameTimeNanos) / 1_000_000_000f
                    value += deltaSeconds * 2.4f
                }
                lastFrameTimeNanos = currentFrameNanos
            }
        }
    }

    val waveLayers = remember(mainColor) {
        listOf(
            DynamicWaveSpec(
                color = lerp(mainColor, Color.White, 0.05f).copy(alpha = 0.25f),
                amplitudeDp = 14f,
                wavelengthDp = 140f,
                speedMultiplier = 0.7f,
                phaseOffset = 0.0f,
                curvatureExponent = 1.0f
            ),
            DynamicWaveSpec(
                color = lerp(mainColor, Color.White, 0.20f).copy(alpha = 0.35f),
                amplitudeDp = 11f,
                wavelengthDp = 105f,
                speedMultiplier = 1.0f,
                phaseOffset = 1.4f,
                curvatureExponent = 1.2f
            ),
            DynamicWaveSpec(
                color = lerp(mainColor, Color.White, 0.45f).copy(alpha = 0.50f),
                amplitudeDp = 8f,
                wavelengthDp = 80f,
                speedMultiplier = 1.35f,
                phaseOffset = 2.8f,
                curvatureExponent = 1.3f
            ),
            DynamicWaveSpec(
                color = lerp(mainColor, Color.White, 0.75f).copy(alpha = 0.70f),
                amplitudeDp = 5f,
                wavelengthDp = 60f,
                speedMultiplier = 1.75f,
                phaseOffset = 4.2f,
                curvatureExponent = 1.5f
            )
        )
    }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    onProgressChange((offset.x / size.width).coerceIn(0f, 1f))
                }
            }
            .pointerInput(Unit) {
                detectHorizontalDragGestures { change, _ ->
                    change.consume()
                    onProgressChange((change.position.x / size.width).coerceIn(0f, 1f))
                }
            }
    ) {
        val width = size.width
        val height = size.height
        val trackY = height * 0.72f
        val activeWidth = width * progress.coerceIn(0f, 1f)

        val trackStrokeWidth = 3.5.dp.toPx()
        val thumbRadius = 7.5.dp.toPx()

        drawLine(
            color = inactiveTrackColor,
            start = Offset(0f, trackY),
            end = Offset(width, trackY),
            strokeWidth = trackStrokeWidth,
            cap = StrokeCap.Round
        )

        if (activeWidth > 2f) {
            waveLayers.forEach { layer ->
                drawSingleWaveLayer(
                    layer = layer,
                    activeWidth = activeWidth,
                    trackY = trackY,
                    continuousTime = elapsedTimeSeconds.value
                )
            }

            drawLine(
                color = mainColor,
                start = Offset(0f, trackY),
                end = Offset(activeWidth, trackY),
                strokeWidth = trackStrokeWidth,
                cap = StrokeCap.Round
            )
        }

        val thumbCenter = Offset(activeWidth, trackY)
        val glowRadius = 16.dp.toPx()

        drawCircle(
            brush = androidx.compose.ui.graphics.Brush.radialGradient(
                colors = listOf(
                    mainColor.copy(alpha = 0.65f),
                    mainColor.copy(alpha = 0.2f),
                    Color.Transparent
                ),
                center = thumbCenter,
                radius = glowRadius
            ),
            radius = glowRadius,
            center = thumbCenter
        )
        drawCircle(
            color = mainColor,
            radius = thumbRadius,
            center = thumbCenter
        )
        drawCircle(
            color = thumbBorderColor,
            radius = thumbRadius,
            center = thumbCenter,
            style = Stroke(width = 2.5.dp.toPx())
        )
    }
}

private fun DrawScope.drawSingleWaveLayer(
    layer: DynamicWaveSpec,
    activeWidth: Float,
    trackY: Float,
    continuousTime: Float
) {
    val maxAmplitudePx = layer.amplitudeDp.dp.toPx()
    val wavelengthPx = layer.wavelengthDp.dp.toPx()
    val angularFrequency = continuousTime * layer.speedMultiplier

    val path = Path().apply {
        moveTo(0f, trackY)

        val stepPx = 3f
        var currentX = 0f

        while (currentX <= activeWidth) {
            val damp = sin(Math.PI * (currentX / activeWidth)).toFloat()
            val theta = ((currentX / wavelengthPx) * 2 * Math.PI) - angularFrequency + layer.phaseOffset
            val sineNorm = ((1f + sin(theta).toFloat()) / 2f)
                .pow(layer.curvatureExponent)

            val waveHeight = maxAmplitudePx * damp * sineNorm
            val y = trackY - waveHeight

            lineTo(currentX, y)
            currentX += stepPx
        }

        lineTo(activeWidth, trackY)
        close()
    }

    drawPath(
        path = path,
        color = layer.color
    )
}

@Preview(name = "Galaxy Fluid Wave Progress Bar Preview", showBackground = true, backgroundColor = 0xFF0B0C0E)
@Composable
fun GalaxyFluidWaveProgressBarPreview() {
    MaterialTheme {
        GalaxyFluidWaveProgressBar(
            progress = 0.55f,
            onProgressChange = {},
            mainColor = Color(0xFF5CB3FF),
            isPlaying = true,
            modifier = Modifier.padding(16.dp)
        )
    }
}
