package io.github.windsekirun.hibiku.feature.ui.immersive

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun FluidWaveSeekBar(
    positionMs: Long,
    durationMs: Long,
    isPlaying: Boolean,
    accentColor: Color,
    modifier: Modifier = Modifier,
    isDarkMode: Boolean = true,
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

    val inactiveColor = if (isDarkMode) Color.White.copy(alpha = 0.25f) else Color.Black.copy(alpha = 0.15f)
    val thumbBorder = if (isDarkMode) Color.White else Color(0xFF111111)
    val thumbInner = if (isDarkMode) Color(0xFF1E1B2E) else Color(0xFFFFFFFF)
    val timeTextColor = if (isDarkMode) Color.White.copy(alpha = 0.75f) else Color.Black.copy(alpha = 0.65f)

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
            inactiveTrackColor = inactiveColor,
            thumbBorderColor = thumbBorder
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
                color = timeTextColor
            )
            Text(
                text = formatTimeMs(durationMs),
                style = MaterialTheme.typography.labelMedium,
                color = timeTextColor
            )
        }
    }
}

@Preview(name = "Fluid Wave Seek Bar Preview", showBackground = true, backgroundColor = 0xFF0B0C0E)
@Composable
fun FluidWaveSeekBarPreview() {
    MaterialTheme {
        FluidWaveSeekBar(
            positionMs = 85_000L,
            durationMs = 210_000L,
            isPlaying = true,
            accentColor = Color(0xFF5CB3FF),
            onSeek = {},
            modifier = Modifier.padding(16.dp)
        )
    }
}
