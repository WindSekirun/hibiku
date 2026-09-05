package io.github.windsekirun.hibiku.feature.ui.immersive

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.isActive
import java.util.Locale
import kotlin.math.pow
import kotlin.math.sin

data class WavePoint(val x: Float, val y: Float)

fun formatTimeMs(ms: Long): String {
    val totalSeconds = (ms.coerceAtLeast(0L) / 1000)
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.US, "%02d:%02d", minutes, seconds)
    }
}

fun calculateWaveY(
    x: Float,
    centerY: Float,
    amplitude: Float,
    wavelength: Float,
    phase: Float
): Float {
    if (wavelength <= 0f) return centerY
    val angle = (x / wavelength) * (2.0 * Math.PI) + phase
    return centerY + (sin(angle).toFloat() * amplitude)
}

fun generateWavePoints(
    width: Float,
    centerY: Float,
    amplitude: Float,
    wavelength: Float,
    phase: Float,
    step: Float = 4f
): List<WavePoint> {
    if (width <= 0f) return emptyList()
    val safeStep = if (step <= 0f) 4f else step
    val points = mutableListOf<WavePoint>()
    var curX = 0f
    while (curX < width) {
        val y = calculateWaveY(curX, centerY, amplitude, wavelength, phase)
        points.add(WavePoint(curX, y))
        curX += safeStep
    }
    val endY = calculateWaveY(width, centerY, amplitude, wavelength, phase)
    points.add(WavePoint(width, endY))
    return points
}

fun calculateSeekPosition(touchX: Float, totalWidth: Float, durationMs: Long): Long {
    if (totalWidth <= 0f || durationMs <= 0L) return 0L
    val ratio = (touchX / totalWidth).coerceIn(0f, 1f)
    return (ratio * durationMs).toLong()
}

@Composable
fun SquigglySeekBar(
    positionMs: Long,
    durationMs: Long,
    isPlaying: Boolean,
    accentColor: Color,
    modifier: Modifier = Modifier,
    inactiveColor: Color = Color.White.copy(alpha = 0.25f),
    thumbRadius: Dp = 7.dp,
    strokeWidth: Dp = 4.dp,
    waveAmplitude: Dp = 5.dp,
    wavelength: Dp = 32.dp,
    onSeek: (Long) -> Unit
) {
    val density = LocalDensity.current
    val strokeWidthPx = with(density) { strokeWidth.toPx() }
    val thumbRadiusPx = with(density) { thumbRadius.toPx() }
    val targetAmplitudePx = with(density) { if (isPlaying) waveAmplitude.toPx() else 0f }
    val wavelengthPx = with(density) { wavelength.toPx() }

    val animatedAmplitude by animateFloatAsState(
        targetValue = targetAmplitudePx,
        animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing),
        label = "AmplitudeAnimation"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "SquigglePhase")
    val animatedPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2.0 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "PhaseAnimation"
    )

    val phase = if (isPlaying) animatedPhase else 0f

    var isDragging by remember { mutableStateOf(false) }
    var dragRatio by remember { mutableFloatStateOf(0f) }

    val currentRatio = if (durationMs > 0L) {
        (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
    } else 0f

    val displayRatio = if (isDragging) dragRatio else currentRatio
    val displayPositionMs = if (isDragging) {
        (dragRatio * durationMs).toLong()
    } else positionMs

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(36.dp)
                .pointerInput(durationMs) {
                    detectTapGestures { offset ->
                        val targetMs = calculateSeekPosition(offset.x, size.width.toFloat(), durationMs)
                        onSeek(targetMs)
                    }
                }
                .pointerInput(durationMs) {
                    detectHorizontalDragGestures(
                        onDragStart = { offset ->
                            isDragging = true
                            dragRatio = (offset.x / size.width.toFloat()).coerceIn(0f, 1f)
                        },
                        onDragEnd = {
                            val targetMs = (dragRatio * durationMs).toLong()
                            isDragging = false
                            onSeek(targetMs)
                        },
                        onDragCancel = {
                            isDragging = false
                        },
                        onHorizontalDrag = { change, _ ->
                            change.consume()
                            val clampedX = change.position.x.coerceIn(0f, size.width.toFloat())
                            dragRatio = clampedX / size.width.toFloat()
                        }
                    )
                }
        ) {
            val width = size.width
            val height = size.height
            val centerY = height / 2f
            val activeWidth = (width * displayRatio).coerceIn(0f, width)

            // Draw inactive track (straight line)
            if (activeWidth < width) {
                drawLine(
                    color = inactiveColor,
                    start = Offset(activeWidth, centerY),
                    end = Offset(width, centerY),
                    strokeWidth = strokeWidthPx,
                    cap = StrokeCap.Round
                )
            }

            // Draw active squiggly track
            if (activeWidth > 0f) {
                val wavePoints = generateWavePoints(
                    width = activeWidth,
                    centerY = centerY,
                    amplitude = animatedAmplitude,
                    wavelength = wavelengthPx,
                    phase = phase,
                    step = 3f
                )

                if (wavePoints.isNotEmpty()) {
                    val path = Path().apply {
                        moveTo(wavePoints.first().x, wavePoints.first().y)
                        for (i in 1 until wavePoints.size) {
                            lineTo(wavePoints[i].x, wavePoints[i].y)
                        }
                    }
                    drawPath(
                        path = path,
                        color = accentColor,
                        style = Stroke(
                            width = strokeWidthPx,
                            cap = StrokeCap.Round
                        )
                    )
                }
            }

            // Draw thumb indicator
            val thumbY = if (activeWidth > 0f) {
                calculateWaveY(activeWidth, centerY, animatedAmplitude, wavelengthPx, phase)
            } else {
                centerY
            }

            // Subtle outer glow/halo ring
            drawCircle(
                color = accentColor.copy(alpha = 0.25f),
                radius = thumbRadiusPx + 5f,
                center = Offset(activeWidth, thumbY)
            )

            // Inner thumb
            drawCircle(
                color = accentColor,
                radius = thumbRadiusPx,
                center = Offset(activeWidth, thumbY)
            )
        }

        Spacer(modifier = Modifier.height(2.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = formatTimeMs(displayPositionMs),
                style = MaterialTheme.typography.labelMedium,
                color = Color.White.copy(alpha = 0.75f)
            )
            Text(
                text = formatTimeMs(durationMs),
                style = MaterialTheme.typography.labelMedium,
                color = Color.White.copy(alpha = 0.75f)
            )
        }
    }
}

// -------------------------------------------------------------------------
// Compose UI Previews
// -------------------------------------------------------------------------

@Preview(name = "Squiggly SeekBar Playing Preview", showBackground = true, backgroundColor = 0xFF0B0C0E)
@Composable
fun SquigglySeekBarPlayingPreview() {
    MaterialTheme {
        SquigglySeekBar(
            positionMs = 75_000L,
            durationMs = 210_000L,
            isPlaying = true,
            accentColor = Color(0xFF5CB3FF),
            onSeek = {},
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(name = "Squiggly SeekBar Paused Preview", showBackground = true, backgroundColor = 0xFF0B0C0E)
@Composable
fun SquigglySeekBarPausedPreview() {
    MaterialTheme {
        SquigglySeekBar(
            positionMs = 30_000L,
            durationMs = 180_000L,
            isPlaying = false,
            accentColor = Color(0xFF5CB3FF),
            onSeek = {},
            modifier = Modifier.padding(16.dp)
        )
    }
}

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
    thumbBorderColor: Color = Color.White,
    thumbInnerColor: Color = Color(0xFF1E1B2E)
) {
    // 1. [수정] 0~2PI 루프 리셋 제거 -> 단조 증가하는 연속 시간(초) 누적으로 변경
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

        // 3. Thumb 드로잉 (Glow 효과 + 메인 컬러 채우기 + 외곽 림)
        val thumbCenter = Offset(activeWidth, trackY)
        val glowRadius = 16.dp.toPx()

        // 1) 메인 컬러 기반 방사형 글로우(Glow)
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
        // 2) 원 내부를 mainColor로 채움
        drawCircle(
            color = mainColor,
            radius = thumbRadius,
            center = thumbCenter
        )
        // 3) 외곽 화이트 테두리
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
            // [수정] +부호를 -부호로 변경하여 파동 진행 방향을 Thumb(오른쪽) 방향으로 전환
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

@Composable
fun FluidWaveSeekBar(
    positionMs: Long,
    durationMs: Long,
    isPlaying: Boolean,
    accentColor: Color,
    modifier: Modifier = Modifier,
    onSeek: (Long) -> Unit
) {
    var isDragging by remember { mutableStateOf(false) }
    var dragRatio by remember { mutableFloatStateOf(0f) }

    val currentRatio = if (durationMs > 0L) {
        (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
    } else 0f

    val displayRatio = if (isDragging) dragRatio else currentRatio
    val displayPositionMs = if (isDragging) {
        (dragRatio * durationMs).toLong()
    } else positionMs

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        GalaxyFluidWaveProgressBar(
            progress = displayRatio,
            onProgressChange = { newRatio ->
                val targetMs = (newRatio * durationMs).toLong()
                onSeek(targetMs)
            },
            mainColor = accentColor,
            isPlaying = isPlaying,
            inactiveTrackColor = Color.White.copy(alpha = 0.25f),
            thumbBorderColor = Color.White,
            thumbInnerColor = Color(0xFF1E1B2E)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = formatTimeMs(displayPositionMs),
                style = MaterialTheme.typography.labelMedium,
                color = Color.White.copy(alpha = 0.75f)
            )
            Text(
                text = formatTimeMs(durationMs),
                style = MaterialTheme.typography.labelMedium,
                color = Color.White.copy(alpha = 0.75f)
            )
        }
    }
}

