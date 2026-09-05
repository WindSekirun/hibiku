package io.github.windsekirun.hibiku.feature.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.graphics.toColorInt
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.Action
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box as GlanceBox
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Row as GlanceRow
import androidx.glance.layout.Spacer as GlanceSpacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.unit.ColorProvider
import io.github.windsekirun.hibiku.core.graphics.PaletteExtractor
import io.github.windsekirun.hibiku.core.graphics.WidgetBitmapRenderer
import io.github.windsekirun.hibiku.domain.model.RingStyle
import io.github.windsekirun.hibiku.domain.model.WidgetConfig
import io.github.windsekirun.hibiku.feature.R
import io.github.windsekirun.hibiku.feature.data.WidgetPreferencesRepository

fun resolveWidgetConfigAndColor(
    context: Context,
    glanceId: GlanceId,
    artwork: Bitmap?
): Pair<WidgetConfig, Int> {
    val appWidgetId = runCatching {
        GlanceAppWidgetManager(context).getAppWidgetId(glanceId)
    }.getOrDefault(-1)

    val prefs = WidgetPreferencesRepository(context)
    val config = if (appWidgetId != -1) {
        prefs.loadConfig(appWidgetId)
    } else {
        val configuredIds = prefs.getAllConfiguredWidgetIds()
        if (configuredIds.isNotEmpty()) {
            prefs.loadConfig(configuredIds.last())
        } else {
            WidgetConfig()
        }
    }

    val defaultColor = runCatching {
        config.borderColorHex.toColorInt()
    }.getOrDefault(AndroidColor.WHITE)

    val accentColor = if (config.useDynamicColor) {
        PaletteExtractor.extractAccentColor(artwork, defaultColor)
    } else {
        defaultColor
    }

    return Pair(config, accentColor)
}

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
            .background(ColorProvider(Color(0xE612141C))),
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
    val artBitmap = remember(artwork, progress, isPlaying, ringStyle, ringColor, sizePx) {
        WidgetBitmapRenderer.renderArtworkWithRing(
            artwork = artwork,
            progress = progress,
            isPlaying = isPlaying,
            ringStyle = ringStyle,
            ringColor = ringColor,
            trackColor = 0x33FFFFFF,
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

@Composable
fun WidgetControlsRow(
    isPlaying: Boolean,
    accentColor: Int,
    modifier: GlanceModifier = GlanceModifier,
    buttonSize: Dp = 32.dp,
    playButtonSize: Dp = 44.dp,
    iconSize: Dp = 20.dp,
    playIconSize: Dp = 22.dp,
    spacing: Dp = 14.dp
) {
    GlanceRow(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Prev button (clean icon button matching mockup)
        GlanceBox(
            modifier = GlanceModifier
                .size(buttonSize)
                .clickable(actionRunCallback<PrevActionCallback>()),
            contentAlignment = Alignment.Center
        ) {
            Image(
                provider = ImageProvider(R.drawable.ic_widget_prev),
                contentDescription = LocalContext.current.getString(R.string.cd_previous),
                modifier = GlanceModifier.size(iconSize),
                colorFilter = ColorFilter.tint(ColorProvider(Color.White))
            )
        }

        GlanceSpacer(modifier = GlanceModifier.width(spacing))

        // Play/Pause button (solid white circle + dark icon matching mockup)
        val playIconRes = if (isPlaying) R.drawable.ic_widget_pause else R.drawable.ic_widget_play
        val playCd = if (isPlaying) R.string.cd_pause else R.string.cd_play

        GlanceBox(
            modifier = GlanceModifier
                .size(playButtonSize)
                .cornerRadius(playButtonSize / 2)
                .background(ColorProvider(Color.White))
                .clickable(actionRunCallback<PlayPauseActionCallback>()),
            contentAlignment = Alignment.Center
        ) {
            Image(
                provider = ImageProvider(playIconRes),
                contentDescription = LocalContext.current.getString(playCd),
                modifier = GlanceModifier.size(playIconSize),
                colorFilter = ColorFilter.tint(ColorProvider(Color(0xFF11111B)))
            )
        }

        GlanceSpacer(modifier = GlanceModifier.width(spacing))

        // Next button (clean icon button matching mockup)
        GlanceBox(
            modifier = GlanceModifier
                .size(buttonSize)
                .clickable(actionRunCallback<NextActionCallback>()),
            contentAlignment = Alignment.Center
        ) {
            Image(
                provider = ImageProvider(R.drawable.ic_widget_next),
                contentDescription = LocalContext.current.getString(R.string.cd_next),
                modifier = GlanceModifier.size(iconSize),
                colorFilter = ColorFilter.tint(ColorProvider(Color.White))
            )
        }
    }
}

@Composable
fun WidgetImmersiveButton(
    modifier: GlanceModifier = GlanceModifier,
    sizeDp: Dp = 28.dp,
    iconSizeDp: Dp = 14.dp
) {
    GlanceBox(
        modifier = modifier
            .size(sizeDp)
            .cornerRadius(sizeDp / 2)
            .background(ColorProvider(Color(0x33FFFFFF)))
            .clickable(actionRunCallback<LaunchImmersiveActionCallback>()),
        contentAlignment = Alignment.Center
    ) {
        Image(
            provider = ImageProvider(R.drawable.ic_widget_immersive),
            contentDescription = LocalContext.current.getString(R.string.cd_open_immersive),
            modifier = GlanceModifier.size(iconSizeDp),
            colorFilter = ColorFilter.tint(ColorProvider(Color.White))
        )
    }
}


