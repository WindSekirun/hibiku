package io.github.windsekirun.hibiku.feature.ui.immersive

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.windsekirun.hibiku.feature.R

@Composable
fun ShapePreviewCard(
    style: ImmersiveShapeStyle,
    isSelected: Boolean,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    bitmap: Bitmap? = null
) {
    val targetShape = getShapeForStyle(style)
    val cardBg = if (isSelected) accentColor.copy(alpha = 0.20f) else Color.White.copy(alpha = 0.08f)
    val borderColor = if (isSelected) accentColor else Color.White.copy(alpha = 0.15f)

    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onClick),
        color = cardBg,
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(targetShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                accentColor.copy(alpha = 0.65f),
                                Color(0xFF252932),
                                Color(0xFF13151A)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (bitmap != null && !bitmap.isRecycled) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_widget_play),
                        contentDescription = null,
                        tint = if (isSelected) accentColor else Color.White.copy(alpha = 0.75f),
                        modifier = Modifier.size(22.dp)
                    )
                }

                if (style == ImmersiveShapeStyle.VINYL) {
                    VinylOverlay()
                }
            }

            Text(
                text = style.label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) accentColor else Color.White
            )
        }
    }
}

@Preview(name = "Shape Preview Card Preview", showBackground = true, backgroundColor = 0xFF16181D)
@Composable
fun ShapePreviewCardPreview() {
    MaterialTheme {
        ShapePreviewCard(
            style = ImmersiveShapeStyle.SCALLOP,
            isSelected = true,
            accentColor = Color(0xFF5CB3FF),
            onClick = {}
        )
    }
}
