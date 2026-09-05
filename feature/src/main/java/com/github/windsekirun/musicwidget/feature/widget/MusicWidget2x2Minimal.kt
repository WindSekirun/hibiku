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
import androidx.glance.layout.Column
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import com.github.windsekirun.musicwidget.domain.model.MediaPlaybackState
import com.github.windsekirun.musicwidget.domain.model.WidgetConfig
import com.github.windsekirun.musicwidget.domain.repository.MediaPlaybackRepository

class MusicWidget2x2Minimal : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            val playbackState by MediaPlaybackRepository.playbackState.collectAsState()
            val (config, accentColor) = resolveWidgetConfigAndColor(context, id, playbackState.albumArt)
            val density = context.resources.displayMetrics.density

            MusicWidget2x2MinimalContent(
                playbackState = playbackState,
                config = config,
                accentColor = accentColor,
                density = density
            )
        }
    }
}

@Composable
fun MusicWidget2x2MinimalContent(
    playbackState: MediaPlaybackState,
    config: WidgetConfig,
    accentColor: Int,
    density: Float
) {
    WidgetBackground(
        artwork = playbackState.albumArt,
        accentColor = accentColor,
        density = density
    ) {
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .padding(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Large circular album art + ring
            WidgetArtworkRing(
                artwork = playbackState.albumArt,
                progress = playbackState.progress,
                isPlaying = playbackState.isPlaying,
                ringStyle = config.ringStyle,
                ringColor = accentColor,
                sizeDp = 100.dp,
                density = density
            )

            Spacer(modifier = GlanceModifier.height(4.dp))

            // Compact controls row
            WidgetControlsRow(
                isPlaying = playbackState.isPlaying,
                accentColor = accentColor,
                buttonSize = 26.dp,
                playButtonSize = 30.dp,
                iconSize = 14.dp,
                playIconSize = 18.dp,
                spacing = 8.dp
            )
        }
    }
}

class MusicWidget2x2MinimalReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = MusicWidget2x2Minimal()
}
