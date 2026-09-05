package io.github.windsekirun.hibiku.feature.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.LocalContext
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box as GlanceBox
import androidx.glance.layout.Column as GlanceColumn
import androidx.glance.layout.Spacer as GlanceSpacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import io.github.windsekirun.hibiku.domain.model.MediaPlaybackState
import io.github.windsekirun.hibiku.domain.model.WidgetConfig
import io.github.windsekirun.hibiku.domain.repository.MediaPlaybackRepository
import io.github.windsekirun.hibiku.feature.R

class MusicWidget2x2Standard : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            val playbackState by MediaPlaybackRepository.playbackState.collectAsState()
            val (config, accentColor) = resolveWidgetConfigAndColor(context, id, playbackState.albumArt)
            val density = context.resources.displayMetrics.density

            MusicWidget2x2StandardContent(
                playbackState = playbackState,
                config = config,
                accentColor = accentColor,
                density = density
            )
        }
    }
}

@Composable
fun MusicWidget2x2StandardContent(
    playbackState: MediaPlaybackState,
    config: WidgetConfig,
    accentColor: Int,
    density: Float
) {
    val context = LocalContext.current

    WidgetBackground(
        artwork = playbackState.albumArt,
        accentColor = accentColor,
        density = density
    ) {
        GlanceBox(
            modifier = GlanceModifier.fillMaxSize()
        ) {
            // Immersive transition button in top-right corner
            WidgetImmersiveButton(
                modifier = GlanceModifier
                    .padding(top = 6.dp, end = 6.dp)
            )

            GlanceColumn(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Top: Circular Album Art + Ring
                WidgetArtworkRing(
                    artwork = playbackState.albumArt,
                    progress = playbackState.progress,
                    isPlaying = playbackState.isPlaying,
                    ringStyle = config.ringStyle,
                    ringColor = accentColor,
                    sizeDp = 82.dp,
                    density = density
                )

                // Middle: Song Title only (artist omitted to prevent cramped controls)
                if (config.textVisible) {
                    GlanceSpacer(modifier = GlanceModifier.height(5.dp))

                    val titleText = playbackState.title.ifBlank {
                        context.getString(R.string.no_music_playing)
                    }

                    Text(
                        text = titleText,
                        maxLines = 1,
                        style = TextStyle(
                            color = ColorProvider(Color.White),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        ),
                        modifier = GlanceModifier.clickable(actionRunCallback<LaunchPlayerActionCallback>())
                    )
                }

                GlanceSpacer(modifier = GlanceModifier.height(6.dp))

                // Bottom: Controls row
                WidgetControlsRow(
                    isPlaying = playbackState.isPlaying,
                    accentColor = accentColor,
                    buttonSize = 30.dp,
                    playButtonSize = 36.dp,
                    iconSize = 16.dp,
                    playIconSize = 20.dp,
                    spacing = 8.dp
                )
            }
        }
    }
}

class MusicWidget2x2StandardReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = MusicWidget2x2Standard()
}
