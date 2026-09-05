package com.github.windsekirun.musicwidget.feature.widget

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
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.github.windsekirun.musicwidget.domain.model.MediaPlaybackState
import com.github.windsekirun.musicwidget.domain.model.WidgetConfig
import com.github.windsekirun.musicwidget.domain.repository.MediaPlaybackRepository
import com.github.windsekirun.musicwidget.feature.R

class MusicWidget4x2 : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            val playbackState by MediaPlaybackRepository.playbackState.collectAsState()
            val (config, accentColor) = resolveWidgetConfigAndColor(context, id, playbackState.albumArt)
            val density = context.resources.displayMetrics.density

            MusicWidget4x2Content(
                playbackState = playbackState,
                config = config,
                accentColor = accentColor,
                density = density
            )
        }
    }
}

@Composable
fun MusicWidget4x2Content(
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
        Row(
            modifier = GlanceModifier
                .fillMaxSize()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: Circular Album Art + Ring
            WidgetArtworkRing(
                artwork = playbackState.albumArt,
                progress = playbackState.progress,
                isPlaying = playbackState.isPlaying,
                ringStyle = config.ringStyle,
                ringColor = accentColor,
                sizeDp = 100.dp,
                density = density
            )

            Spacer(modifier = GlanceModifier.width(14.dp))

            // Right: Song info + controls
            Column(
                modifier = GlanceModifier
                    .fillMaxHeight()
                    .defaultWeight(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (config.textVisible) {
                    val titleText = playbackState.title.ifBlank {
                        context.getString(R.string.no_music_playing)
                    }
                    val artistText = playbackState.artist.ifBlank {
                        context.getString(R.string.unknown_artist)
                    }

                    Text(
                        text = titleText,
                        maxLines = 1,
                        style = TextStyle(
                            color = ColorProvider(Color.White),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        modifier = GlanceModifier
                            .fillMaxWidth()
                            .clickable(actionRunCallback<LaunchPlayerActionCallback>())
                    )

                    Spacer(modifier = GlanceModifier.height(2.dp))

                    Text(
                        text = artistText,
                        maxLines = 1,
                        style = TextStyle(
                            color = ColorProvider(Color(0xCCFFFFFF)),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Normal
                        ),
                        modifier = GlanceModifier
                            .fillMaxWidth()
                            .clickable(actionRunCallback<LaunchPlayerActionCallback>())
                    )

                    Spacer(modifier = GlanceModifier.height(8.dp))
                }

                WidgetControlsRow(
                    isPlaying = playbackState.isPlaying,
                    accentColor = accentColor,
                    buttonSize = 36.dp,
                    playButtonSize = 42.dp,
                    iconSize = 20.dp,
                    playIconSize = 24.dp,
                    spacing = 14.dp
                )
            }
        }
    }
}

class MusicWidget4x2Receiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = MusicWidget4x2()
}
