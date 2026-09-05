package io.github.windsekirun.hibiku.feature.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.coerceAtLeast
import androidx.compose.ui.unit.coerceAtMost
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.LocalSize
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box as GlanceBox
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import io.github.windsekirun.hibiku.domain.model.MediaPlaybackState
import io.github.windsekirun.hibiku.domain.model.WidgetConfig
import io.github.windsekirun.hibiku.domain.repository.MediaPlaybackRepository

/**
 * Pure 2x2 Widget: Transparent background with only circular album art and progress ring.
 */
class MusicWidget2x2Pure : GlanceAppWidget() {

    override val sizeMode = SizeMode.Exact

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
    val size = LocalSize.current

    // 짧은 변의 85% 를 앨범아트로 채움 (Pure는 여백 없이 꽉 차는 느낌)
    val minDim = minOf(size.width.value, size.height.value)
    val artworkSizeDp = (minDim * 0.85f).dp
        .coerceAtLeast(80.dp)
        .coerceAtMost(220.dp)

    GlanceBox(
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
            sizeDp = artworkSizeDp,
            density = density
        )

        // Immersive player button in top-right
        GlanceBox(
            modifier = GlanceModifier.fillMaxSize().padding(top = 14.dp, end = 14.dp),
            contentAlignment = Alignment.TopEnd
        ) {
            WidgetImmersiveButton()
        }
    }
}

class MusicWidget2x2PureReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = MusicWidget2x2Pure()
}
