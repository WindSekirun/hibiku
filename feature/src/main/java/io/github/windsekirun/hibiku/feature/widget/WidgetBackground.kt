package io.github.windsekirun.hibiku.feature.widget

import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box as GlanceBox
import androidx.glance.layout.ContentScale
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.unit.ColorProvider
import io.github.windsekirun.hibiku.core.graphics.WidgetBitmapRenderer

@Composable
fun WidgetBackground(
    artwork: Bitmap?,
    accentColor: Int,
    density: Float,
    useBlurBackground: Boolean = true,
    modifier: GlanceModifier = GlanceModifier.fillMaxSize(),
    cornerRadiusDp: Dp = 32.dp,
    content: @Composable () -> Unit
) {
    val size = LocalSize.current
    // Optimize bitmap dimensions to stay well within Android's 1MB Binder IPC transaction limit
    val wPx = minOf((size.width.value * density).toInt(), 160).coerceAtLeast(64)
    val hPx = minOf((size.height.value * density).toInt(), 120).coerceAtLeast(48)
    val cornerRadiusPx = cornerRadiusDp.value * density

    val bgBitmap = remember(artwork, wPx, hPx, accentColor, useBlurBackground) {
        if (useBlurBackground && artwork != null && !artwork.isRecycled) {
            WidgetBitmapRenderer.renderBlurredBackground(
                artwork = artwork,
                widthPx = wPx,
                heightPx = hPx,
                dimAlpha = 0.45f,
                cornerRadiusPx = cornerRadiusPx,
                strokeColor = 0
            )
        } else {
            Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
        }
    }

    GlanceBox(
        modifier = modifier
            .cornerRadius(cornerRadiusDp)
            .background(ColorProvider(Color(0xE612141CL))),
        contentAlignment = Alignment.Center
    ) {
        Image(
            provider = ImageProvider(bgBitmap),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = GlanceModifier.fillMaxSize().cornerRadius(cornerRadiusDp)
        )

        // Top-Right Expand Button (btn-expand positioned at top-right corner matching mockup)
        GlanceBox(
            modifier = GlanceModifier.fillMaxSize().padding(top = 14.dp, end = 14.dp),
            contentAlignment = Alignment.TopEnd
        ) {
            WidgetImmersiveButton()
        }

        content()
    }
}
