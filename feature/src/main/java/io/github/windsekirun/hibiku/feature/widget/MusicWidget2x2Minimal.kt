package io.github.windsekirun.hibiku.feature.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box as GlanceBox
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.unit.ColorProvider
import io.github.windsekirun.hibiku.domain.model.MediaPlaybackState
import io.github.windsekirun.hibiku.domain.model.WidgetConfig
import io.github.windsekirun.hibiku.domain.repository.MediaPlaybackRepository
import io.github.windsekirun.hibiku.feature.data.WidgetPreferencesRepository

class MusicWidget2x2Minimal : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            val playbackState by MediaPlaybackRepository.playbackState.collectAsState()
            val (config, accentColor) = resolveWidgetConfigAndColor(context, id, playbackState.albumArt)
            val density = context.resources.displayMetrics.density

            val appWidgetId = runCatching {
                GlanceAppWidgetManager(context).getAppWidgetId(id)
            }.getOrDefault(-1)
            val prefs = WidgetPreferencesRepository(context)
            val isOverlayVisible = prefs.isMinimalOverlayVisible(appWidgetId)

            MusicWidget2x2MinimalContent(
                playbackState = playbackState,
                config = config,
                accentColor = accentColor,
                density = density,
                isOverlayVisible = isOverlayVisible
            )
        }
    }
}

@Composable
fun MusicWidget2x2MinimalContent(
    playbackState: MediaPlaybackState,
    config: WidgetConfig,
    accentColor: Int,
    density: Float,
    isOverlayVisible: Boolean = false
) {
    WidgetBackground(
        artwork = playbackState.albumArt,
        accentColor = accentColor,
        density = density
    ) {
        GlanceBox(
            modifier = GlanceModifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            // Immersive player transition button in top-right corner
            WidgetImmersiveButton(
                modifier = GlanceModifier
                    .padding(top = 6.dp, end = 6.dp)
            )

            // Artwork + Ring (taps toggle overlay controls)
            WidgetArtworkRing(
                artwork = playbackState.albumArt,
                progress = playbackState.progress,
                isPlaying = playbackState.isPlaying,
                ringStyle = config.ringStyle,
                ringColor = accentColor,
                sizeDp = 120.dp,
                density = density,
                onClick = actionRunCallback<ToggleMinimalOverlayActionCallback>()
            )

            // Dimmed overlay with controls if toggled on
            if (isOverlayVisible) {
                GlanceBox(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .background(ColorProvider(Color(0x99000000)))
                        .cornerRadius(16.dp)
                        .clickable(actionRunCallback<ToggleMinimalOverlayActionCallback>()),
                    contentAlignment = Alignment.Center
                ) {
                    WidgetControlsRow(
                        isPlaying = playbackState.isPlaying,
                        accentColor = accentColor,
                        buttonSize = 34.dp,
                        playButtonSize = 42.dp,
                        iconSize = 18.dp,
                        playIconSize = 22.dp,
                        spacing = 10.dp
                    )
                }
            }
        }
    }
}

class MusicWidget2x2MinimalReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = MusicWidget2x2Minimal()
}
