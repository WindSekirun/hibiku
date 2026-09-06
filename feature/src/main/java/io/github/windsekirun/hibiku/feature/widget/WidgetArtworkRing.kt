package io.github.windsekirun.hibiku.feature.widget

import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.Dp
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.action.Action
import androidx.glance.action.clickable
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.layout.size
import io.github.windsekirun.hibiku.core.graphics.WidgetBitmapRenderer
import io.github.windsekirun.hibiku.domain.model.RingStyle
import io.github.windsekirun.hibiku.feature.R

@Composable
fun WidgetArtworkRing(
    artwork: Bitmap?,
    progress: Float,
    isPlaying: Boolean,
    ringStyle: RingStyle,
    ringColor: Int,
    sizeDp: Dp,
    density: Float,
    modifier: GlanceModifier = GlanceModifier,
    onClick: Action = actionRunCallback<LaunchPlayerActionCallback>()
) {
    // Optimize ring bitmap dimensions (max 180px) for Binder IPC memory safety
    val sizePx = minOf((sizeDp.value * density).toInt(), 180).coerceAtLeast(32)
    val artBitmap = remember(artwork, ringColor, sizePx) {
        WidgetBitmapRenderer.renderArtworkWithBorder(
            artwork = artwork,
            borderColor = ringColor,
            sizePx = sizePx
        )
    }

    Image(
        provider = ImageProvider(artBitmap),
        contentDescription = LocalContext.current.getString(R.string.cd_album_art),
        modifier = modifier
            .size(sizeDp)
            .clickable(onClick)
    )
}
