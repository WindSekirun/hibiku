package io.github.windsekirun.hibiku.feature.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.coerceAtLeast
import androidx.compose.ui.unit.coerceAtMost
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
import androidx.glance.layout.Box as GlanceBox
import androidx.glance.layout.Column as GlanceColumn
import androidx.glance.layout.Spacer as GlanceSpacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
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

    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            val playbackState by MediaPlaybackRepository.playbackState.collectAsState()
            val (config, accentColor) = resolveWidgetConfigAndColor(context, id, playbackState.albumArt)
            val density = context.resources.displayMetrics.density

            androidx.compose.runtime.key(playbackState.albumArt == null, playbackState.title, playbackState.artist, playbackState.isPlaying) {
                MusicWidget2x2StandardContent(
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
fun MusicWidget2x2StandardContent(
    playbackState: MediaPlaybackState,
    config: WidgetConfig,
    accentColor: Int,
    density: Float
) {
    val context = LocalContext.current
    val size = LocalSize.current

    // 짧은 변 기준으로 앨범아트 크기 결정 (정방형에 가까울수록 크게)
    val minDim = minOf(size.width.value, size.height.value)
    val artworkSizeDp: Dp = (minDim * 0.46f).coerceIn(56f, 130f).dp
    val titleFontSp: TextUnit = (minDim * 0.09f).coerceIn(11f, 18f).sp
    val artistFontSp: TextUnit = (minDim * 0.072f).coerceIn(9f, 14f).sp

    // 컨트롤 크기도 비례
    val buttonSizeDp: Dp = (minDim * 0.17f).coerceIn(20f, 36f).dp
    val playButtonSizeDp: Dp = (minDim * 0.23f).coerceIn(28f, 46f).dp
    val iconSizeDp: Dp = (minDim * 0.11f).coerceIn(12f, 22f).dp
    val playIconSizeDp: Dp = (minDim * 0.13f).coerceIn(14f, 24f).dp
    val spacingDp: Dp = (minDim * 0.065f).coerceIn(6f, 14f).dp

    WidgetBackground(
        artwork = playbackState.albumArt,
        accentColor = accentColor,
        density = density,
        useBlurBackground = config.useBlurBackground
    ) {
        GlanceColumn(
            modifier = GlanceModifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 14.dp),
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
                sizeDp = artworkSizeDp,
                density = density
            )

            // Middle: Song Title + Artist Name
            if (config.textVisible) {
                GlanceSpacer(modifier = GlanceModifier.height(6.dp))

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
                        fontSize = titleFontSp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    ),
                    modifier = GlanceModifier
                        .fillMaxWidth()
                        .clickable(actionRunCallback<LaunchPlayerActionCallback>())
                )

                GlanceSpacer(modifier = GlanceModifier.height(2.dp))

                Text(
                    text = artistText,
                    maxLines = 1,
                    style = TextStyle(
                        color = ColorProvider(Color(0xB3FFFFFF)),
                        fontSize = artistFontSp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center
                    ),
                    modifier = GlanceModifier
                        .fillMaxWidth()
                        .clickable(actionRunCallback<LaunchPlayerActionCallback>())
                )
            }

            GlanceSpacer(modifier = GlanceModifier.height(8.dp))

            // Bottom: Controls row
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

class MusicWidget2x2StandardReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = MusicWidget2x2Standard()
}
