package com.github.windsekirun.musicwidget.feature.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import com.github.windsekirun.musicwidget.domain.model.MediaPlaybackState
import com.github.windsekirun.musicwidget.domain.model.WidgetConfig
import com.github.windsekirun.musicwidget.domain.repository.MediaPlaybackRepository

/**
 * Pure 2x2 Widget: Transparent background with only circular album art and progress ring.
 */
class MusicWidget2x2Pure : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            val playbackState by MediaPlaybackRepository.playbackState.collectAsState()
            val (config, accentColor) = resolveWidgetConfigAndColor(context, id, playbackState.albumArt)
            val density = context.resources.displayMetrics.density

            MusicWidget2x2PureContent(
                playbackState = playbackState,
                config = config,
                accentColor = accentColor,
                density = density
            )
        }
    }
}

@Composable
fun MusicWidget2x2PureContent(
    playbackState: MediaPlaybackState,
    config: WidgetConfig,
    accentColor: Int,
    density: Float
) {
    Box(
        modifier = GlanceModifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        // Pure transparent: Only circular artwork + progress ring
        WidgetArtworkRing(
            artwork = playbackState.albumArt,
            progress = playbackState.progress,
            isPlaying = playbackState.isPlaying,
            ringStyle = config.ringStyle,
            ringColor = accentColor,
            sizeDp = 136.dp,
            density = density
        )

        // Immersive player button in top-right
        WidgetImmersiveButton(
            modifier = GlanceModifier
                .padding(top = 4.dp, end = 4.dp)
        )
    }
}

class MusicWidget2x2PureReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = MusicWidget2x2Pure()
}
