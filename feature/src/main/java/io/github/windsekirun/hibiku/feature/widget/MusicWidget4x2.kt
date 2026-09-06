package io.github.windsekirun.hibiku.feature.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.Alignment
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
import io.github.windsekirun.hibiku.domain.model.MediaPlaybackState
import io.github.windsekirun.hibiku.domain.model.WidgetConfig
import io.github.windsekirun.hibiku.domain.repository.MediaPlaybackRepository
import io.github.windsekirun.hibiku.feature.R
import androidx.glance.layout.Column as GlanceColumn
import androidx.glance.layout.Row as GlanceRow
import androidx.glance.layout.Spacer as GlanceSpacer

class MusicWidget4x2 : GlanceAppWidget() {

    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            val playbackState by MediaPlaybackRepository.playbackState.collectAsState()
            val (config, accentColor) = resolveWidgetConfigAndColor(context, id, playbackState.albumArt)
            val density = context.resources.displayMetrics.density

            android.util.Log.i("MusicWidget4x2", "[provideGlance] id=$id, title='${playbackState.title}', artist='${playbackState.artist}', isPlaying=${playbackState.isPlaying}, pos=${playbackState.positionMs}/${playbackState.durationMs}, prog=${playbackState.progress}")

            androidx.compose.runtime.key(
                playbackState.albumArt?.generationId ?: 0,
                playbackState.title,
                playbackState.artist,
                playbackState.isPlaying,
                config
            ) {
                MusicWidget4x2Content(
                    playbackState = playbackState,
                    config = config,
                    accentColor = accentColor,
                    density = density
                )
            }
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
    val size = LocalSize.current

    // 위젯 높이 기준으로 요소 크기 비례 계산
    val h = size.height.value  // dp 값
    val artworkSizeDp: Dp = h.coerceIn(60f, 125f / 0.68f).times(0.68f).dp
    val artistFontSp: TextUnit = maxOf(10f, minOf(17f, h * 0.11f)).sp
    val titleFontSp: TextUnit = maxOf(13f, minOf(28f, h * 0.17f)).sp
    val spacerAfterTextDp: Dp = h.coerceIn(4f / 0.07f, 14f / 0.07f).times(0.07f).dp

    // 컨트롤 버튼 크기도 높이에 비례
    val buttonSizeDp: Dp = h.coerceIn(24f / 0.24f, 40f / 0.24f).times(0.24f).dp
    val playButtonSizeDp: Dp = h.coerceIn(34f / 0.34f, 52f / 0.34f).times(0.34f).dp
    val iconSizeDp: Dp = h.coerceIn(14f / 0.15f, 24f / 0.15f).times(0.15f).dp
    val playIconSizeDp: Dp = h.coerceIn(16f / 0.17f, 26f / 0.17f).times(0.17f).dp
    val spacingDp: Dp = h.coerceIn(8f / 0.12f, 20f / 0.12f).times(0.12f).dp


    WidgetBackground(
        artwork = playbackState.albumArt,
        accentColor = accentColor,
        density = density,
        useBlurBackground = config.useBlurBackground
    ) {
        GlanceRow(
            modifier = GlanceModifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: Circular Album Art + Ring
            WidgetArtworkRing(
                artwork = playbackState.albumArt,
                progress = playbackState.progress,
                isPlaying = playbackState.isPlaying,
                ringStyle = config.ringStyle,
                ringColor = accentColor,
                sizeDp = artworkSizeDp,
                density = density
            )

            GlanceSpacer(modifier = GlanceModifier.width(14.dp))

            // Right: Song info + controls
            GlanceColumn(
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

                    // Artist Name First (Uppercase)
                    Text(
                        text = artistText.uppercase(),
                        maxLines = 1,
                        style = TextStyle(
                            color = ColorProvider(Color(0xB3FFFFFF)),
                            fontSize = artistFontSp,
                            fontWeight = FontWeight.Medium
                        ),
                        modifier = GlanceModifier
                            .fillMaxWidth()
                            .clickable(actionRunCallback<LaunchPlayerActionCallback>())
                    )

                    GlanceSpacer(modifier = GlanceModifier.height(2.dp))

                    // Track Title Second (Large Bold White)
                    Text(
                        text = titleText,
                        maxLines = 1,
                        style = TextStyle(
                            color = ColorProvider(Color.White),
                            fontSize = titleFontSp,
                            fontWeight = FontWeight.Bold
                        ),
                        modifier = GlanceModifier
                            .fillMaxWidth()
                            .clickable(actionRunCallback<LaunchPlayerActionCallback>())
                    )

                    GlanceSpacer(modifier = GlanceModifier.height(spacerAfterTextDp))
                }

                WidgetControlsRow(
                    isPlaying = playbackState.isPlaying,
                    accentColor = accentColor,
                    buttonSize = buttonSizeDp,
                    playButtonSize = playButtonSizeDp,
                    iconSize = iconSizeDp,
                    playIconSize = playIconSizeDp,
                    spacing = spacingDp
                )
            }
        }
    }
}

class MusicWidget4x2Receiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = MusicWidget4x2()
}
