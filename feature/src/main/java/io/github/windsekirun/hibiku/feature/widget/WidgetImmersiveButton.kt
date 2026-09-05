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
import androidx.glance.layout.size
import androidx.glance.unit.ColorProvider
import io.github.windsekirun.hibiku.feature.R

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
