package io.github.windsekirun.hibiku.feature.ui.immersive

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun VinylOverlay() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val maxRadius = size.minDimension / 2f

        // Concentric vinyl groove rings
        val grooveFractions = listOf(0.92f, 0.85f, 0.78f, 0.70f, 0.62f, 0.55f, 0.47f, 0.40f)
        for (fraction in grooveFractions) {
            drawCircle(
                color = Color.White.copy(alpha = 0.07f),
                radius = maxRadius * fraction,
                center = center,
                style = Stroke(width = 1.2f)
            )
        }

        // Center vinyl label border
        drawCircle(
            color = Color.Black.copy(alpha = 0.5f),
            radius = maxRadius * 0.32f,
            center = center
        )
        drawCircle(
            color = Color.White.copy(alpha = 0.2f),
            radius = maxRadius * 0.32f,
            center = center,
            style = Stroke(width = 1.5f)
        )

        // Center spindle hole
        drawCircle(
            color = Color(0xFF0B0C0E),
            radius = maxRadius * 0.08f,
            center = center
        )
    }
}

@Preview(name = "Vinyl Overlay Preview", showBackground = true, backgroundColor = 0xFF1E2129)
@Composable
fun VinylOverlayPreview() {
    MaterialTheme {
        Box(modifier = Modifier.size(200.dp)) {
            VinylOverlay()
        }
    }
}
