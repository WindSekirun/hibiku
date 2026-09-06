package io.github.windsekirun.hibiku.feature.ui.config

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.windsekirun.hibiku.core.graphics.WidgetBitmapRenderer
import io.github.windsekirun.hibiku.domain.model.RingStyle
import io.github.windsekirun.hibiku.feature.R

data class RingStyleOption(
    val style: RingStyle,
    val titleRes: Int,
    val description: String
)

val RING_STYLE_OPTIONS = listOf(
    RingStyleOption(RingStyle.SQUIGGLY_WAVE, R.string.ring_style_squiggly_wave, "물결 파형"),
    RingStyleOption(RingStyle.FLOATING_CLEAN, R.string.ring_style_floating_clean, "이중 링 클린"),
    RingStyleOption(RingStyle.SEGMENTED_MINIMAL, R.string.ring_style_segmented_minimal, "세그먼트 분할"),
    RingStyleOption(RingStyle.GLOW_THUMB, R.string.ring_style_glow_thumb, "글로우 썸"),
    RingStyleOption(RingStyle.SOLID_CLASSIC, R.string.ring_style_solid_classic, "클래식 프로그레스")
)

@Composable
fun RingStylePickerRow(
    selectedStyle: RingStyle,
    onStyleSelected: (RingStyle) -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current.density
    val previewSizePx = (44 * density).toInt().coerceAtLeast(32)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        RING_STYLE_OPTIONS.forEach { option ->
            val isSelected = option.style == selectedStyle

            val miniBitmap = remember(option.style, previewSizePx) {
                WidgetBitmapRenderer.renderArtworkWithRing(
                    artwork = null,
                    ringColor = 0xFF5CB3FF.toInt(),
                    sizePx = previewSizePx
                )
            }

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = if (isSelected) {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                } else {
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                },
                border = BorderStroke(
                    width = if (isSelected) 2.dp else 1.dp,
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                    }
                ),
                modifier = Modifier
                    .width(130.dp)
                    .clickable { onStyleSelected(option.style) }
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Image(
                        bitmap = miniBitmap.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier.size(44.dp)
                    )

                    Text(
                        text = stringResource(option.titleRes),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        fontSize = 13.sp,
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        }
                    )

                    Text(
                        text = option.description,
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Preview(name = "Ring Style Picker Row Preview", backgroundColor = 0xFF1E1E1E, showBackground = true)
@Composable
fun RingStylePickerRowPreview() {
    MaterialTheme {
        RingStylePickerRow(
            selectedStyle = RingStyle.SQUIGGLY_WAVE,
            onStyleSelected = {}
        )
    }
}
