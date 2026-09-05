package io.github.windsekirun.hibiku.feature.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import androidx.core.graphics.toColorInt
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidgetManager
import io.github.windsekirun.hibiku.core.graphics.PaletteExtractor
import io.github.windsekirun.hibiku.domain.model.WidgetConfig
import io.github.windsekirun.hibiku.feature.data.WidgetPreferencesRepository

fun resolveWidgetConfigAndColor(
    context: Context,
    glanceId: GlanceId,
    artwork: Bitmap?
): Pair<WidgetConfig, Int> {
    val appWidgetId = runCatching {
        GlanceAppWidgetManager(context).getAppWidgetId(glanceId)
    }.getOrDefault(-1)

    val prefs = WidgetPreferencesRepository(context)
    val config = if (appWidgetId != -1) {
        prefs.loadConfig(appWidgetId)
    } else {
        val configuredIds = prefs.getAllConfiguredWidgetIds()
        if (configuredIds.isNotEmpty()) {
            prefs.loadConfig(configuredIds.last())
        } else {
            WidgetConfig()
        }
    }

    val defaultColor = runCatching {
        config.borderColorHex.toColorInt()
    }.getOrDefault(AndroidColor.WHITE)

    val accentColor = if (config.useDynamicColor) {
        PaletteExtractor.extractAccentColor(artwork, defaultColor)
    } else {
        defaultColor
    }

    return Pair(config, accentColor)
}
