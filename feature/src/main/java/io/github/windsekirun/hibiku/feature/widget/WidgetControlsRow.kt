package io.github.windsekirun.hibiku.feature.widget

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.glance.ColorFilter
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.action.clickable
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box as GlanceBox
import androidx.glance.layout.Row as GlanceRow
import androidx.glance.layout.Spacer as GlanceSpacer
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.unit.ColorProvider
import io.github.windsekirun.hibiku.feature.R

@Composable
fun WidgetControlsRow(
    isPlaying: Boolean,
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
