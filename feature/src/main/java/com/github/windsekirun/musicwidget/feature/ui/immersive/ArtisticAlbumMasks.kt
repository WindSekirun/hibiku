package com.github.windsekirun.musicwidget.feature.ui.immersive

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.github.windsekirun.musicwidget.feature.R
import kotlin.math.cos
import kotlin.math.sin

enum class ImmersiveShapeStyle(val label: String) {
    FIGURE_8("Figure 8"),
    SQUIRCLE("Squircle"),
    VINYL("Vinyl LP"),
    SCALLOP("Scallop")
}

class Figure8Shape : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val w = size.width
        val h = size.height
        val path = Path().apply {
            moveTo(w * 0.5f, 0f)
            // Top-right lobe
            cubicTo(w * 0.85f, 0f, w, h * 0.12f, w, h * 0.28f)
            // Inward to right waist
            cubicTo(w, h * 0.42f, w * 0.82f, h * 0.5f, w * 0.80f, h * 0.5f)
            // Outward to bottom-right lobe
            cubicTo(w * 0.82f, h * 0.5f, w, h * 0.58f, w, h * 0.72f)
            // Down to bottom center
            cubicTo(w, h * 0.88f, w * 0.85f, h, w * 0.5f, h)
            // Bottom-left lobe
            cubicTo(w * 0.15f, h, 0f, h * 0.88f, 0f, h * 0.72f)
            // Inward to left waist
            cubicTo(0f, h * 0.58f, w * 0.18f, h * 0.5f, w * 0.20f, h * 0.5f)
            // Outward to top-left lobe
            cubicTo(w * 0.18f, h * 0.5f, 0f, h * 0.42f, 0f, h * 0.28f)
            // Back to top center
            cubicTo(0f, h * 0.12f, w * 0.15f, 0f, w * 0.5f, 0f)
            close()
        }
        return Outline.Generic(path)
    }
}

class SquircleShape(val cornerPercent: Int = 28) : Shape {
    private val delegate = RoundedCornerShape(cornerPercent)

    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline = delegate.createOutline(size, layoutDirection, density)
}

data class ScallopSegment(
    val startX: Float,
    val startY: Float,
    val ctrlX: Float,
    val ctrlY: Float,
    val endX: Float,
    val endY: Float
)

fun calculateScallopSegments(
    points: Int,
    cx: Float,
    cy: Float,
    innerRadius: Float,
    outerRadius: Float
): List<ScallopSegment> {
    if (points <= 0) return emptyList()
    val segments = mutableListOf<ScallopSegment>()
    val stepAngle = (2.0 * Math.PI) / points.toDouble()

    for (i in 0 until points) {
        val currentAngle = i * stepAngle
        val nextAngle = (i + 1) * stepAngle
        val midAngle = currentAngle + (stepAngle / 2.0)

        val startX = (cx + innerRadius * cos(currentAngle)).toFloat()
        val startY = (cy + innerRadius * sin(currentAngle)).toFloat()

        val ctrlX = (cx + outerRadius * cos(midAngle)).toFloat()
        val ctrlY = (cy + outerRadius * sin(midAngle)).toFloat()

        val endX = (cx + innerRadius * cos(nextAngle)).toFloat()
        val endY = (cy + innerRadius * sin(nextAngle)).toFloat()

        segments.add(ScallopSegment(startX, startY, ctrlX, ctrlY, endX, endY))
    }
    return segments
}

class ScallopShape(val points: Int = 12) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val path = Path()
        val width = size.width
        val height = size.height
        val cx = width / 2f
        val cy = height / 2f
        val maxRadius = minOf(width, height) / 2f
        val innerRadius = maxRadius * 0.85f
        val outerRadius = maxRadius

        val segments = calculateScallopSegments(points, cx, cy, innerRadius, outerRadius)
        segments.forEachIndexed { index, segment ->
            if (index == 0) {
                path.moveTo(segment.startX, segment.startY)
            }
            path.quadraticTo(segment.ctrlX, segment.ctrlY, segment.endX, segment.endY)
        }
        path.close()
        return Outline.Generic(path)
    }
}

fun getShapeForStyle(style: ImmersiveShapeStyle): Shape = when (style) {
    ImmersiveShapeStyle.FIGURE_8 -> Figure8Shape()
    ImmersiveShapeStyle.SQUIRCLE -> SquircleShape(28)
    ImmersiveShapeStyle.VINYL -> CircleShape
    ImmersiveShapeStyle.SCALLOP -> ScallopShape(12)
}

@Composable
fun ArtisticAlbumArt(
    bitmap: Bitmap?,
    shape: ImmersiveShapeStyle,
    modifier: Modifier = Modifier,
    glowColor: Color = Color(0xFF5CB3FF),
    contentDescription: String? = null
) {
    val targetShape = getShapeForStyle(shape)

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(targetShape),
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null && !bitmap.isRecycled) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = contentDescription,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // Vinyl groove and center cutout accents
            if (shape == ImmersiveShapeStyle.VINYL) {
                VinylOverlay()
            }
        } else {
            // Stylized placeholder
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                glowColor.copy(alpha = 0.45f),
                                Color(0xFF1E2129),
                                Color(0xFF111317)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (shape == ImmersiveShapeStyle.VINYL) {
                    VinylOverlay()
                }

                Icon(
                    painter = painterResource(id = R.drawable.ic_widget_play),
                    contentDescription = contentDescription,
                    tint = glowColor.copy(alpha = 0.85f),
                    modifier = Modifier.size(54.dp)
                )
            }
        }
    }
}

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

@Composable
fun ScallopButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 56.dp,
    backgroundColor: Color = Color.White.copy(alpha = 0.12f),
    contentColor: Color = Color.White,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(ScallopShape(12))
            .background(backgroundColor)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            color = Color.Transparent,
            contentColor = contentColor
        ) {
            content()
        }
    }
}

@Composable
fun M3BoldPillButton(
    isPlaying: Boolean,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    width: Dp = 104.dp,
    height: Dp = 60.dp
) {
    val iconRes = if (isPlaying) R.drawable.ic_widget_pause else R.drawable.ic_widget_play

    Box(
        modifier = modifier
            .width(width)
            .height(height)
            .shadow(elevation = 8.dp, shape = RoundedCornerShape(percent = 50), spotColor = accentColor)
            .clip(RoundedCornerShape(percent = 50))
            .background(accentColor)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(id = iconRes),
            contentDescription = if (isPlaying) "Pause" else "Play",
            tint = Color(0xFF0B0C0E),
            modifier = Modifier.size(32.dp)
        )
    }
}
