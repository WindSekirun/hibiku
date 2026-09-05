package io.github.windsekirun.hibiku.feature.data

import android.content.Context
import android.content.SharedPreferences
import io.github.windsekirun.hibiku.domain.model.M3ShapeStyle
import io.github.windsekirun.hibiku.domain.model.RingStyle
import io.github.windsekirun.hibiku.domain.model.WidgetConfig

/**
 * Repository responsible for persisting and retrieving per-widget configurations.
 */
class WidgetPreferencesRepository(
    private val preferences: SharedPreferences
) {
    constructor(context: Context) : this(
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    )

    /**
     * Saves the [WidgetConfig] for the specified [appWidgetId].
     */
    fun saveConfig(appWidgetId: Int, config: WidgetConfig) {
        preferences.edit()
            .putString(keyRingStyle(appWidgetId), config.ringStyle.name)
            .putString(keyBorderColor(appWidgetId), config.borderColorHex)
            .putBoolean(keyDynamicColor(appWidgetId), config.useDynamicColor)
            .putBoolean(keyTextVisible(appWidgetId), config.textVisible)
            .putString(keyShapeStyle(appWidgetId), config.shapeStyle.name)
            .apply()
    }

    /**
     * Loads the [WidgetConfig] for the specified [appWidgetId].
     * Returns default [WidgetConfig] if not found or corrupted.
     */
    fun loadConfig(appWidgetId: Int): WidgetConfig {
        val defaultConfig = WidgetConfig()

        val ringStyleStr = preferences.getString(keyRingStyle(appWidgetId), null)
        val ringStyle = ringStyleStr?.let {
            runCatching { RingStyle.valueOf(it) }.getOrNull()
        } ?: defaultConfig.ringStyle

        val borderColorHex = preferences.getString(keyBorderColor(appWidgetId), null)
            ?: defaultConfig.borderColorHex

        val dynamicColorKey = keyDynamicColor(appWidgetId)
        val useDynamicColor = if (preferences.contains(dynamicColorKey)) {
            preferences.getBoolean(dynamicColorKey, defaultConfig.useDynamicColor)
        } else {
            defaultConfig.useDynamicColor
        }

        val textVisibleKey = keyTextVisible(appWidgetId)
        val textVisible = if (preferences.contains(textVisibleKey)) {
            preferences.getBoolean(textVisibleKey, defaultConfig.textVisible)
        } else {
            defaultConfig.textVisible
        }

        val shapeStyleStr = preferences.getString(keyShapeStyle(appWidgetId), null)
        val shapeStyle = shapeStyleStr?.let {
            runCatching { M3ShapeStyle.valueOf(it) }.getOrNull()
        } ?: defaultConfig.shapeStyle

        return WidgetConfig(
            ringStyle = ringStyle,
            borderColorHex = borderColorHex,
            useDynamicColor = useDynamicColor,
            textVisible = textVisible,
            shapeStyle = shapeStyle
        )
    }

    /**
     * Removes all saved configurations for the specified [appWidgetId].
     */
    fun removeConfig(appWidgetId: Int) {
        preferences.edit()
            .remove(keyRingStyle(appWidgetId))
            .remove(keyBorderColor(appWidgetId))
            .remove(keyDynamicColor(appWidgetId))
            .remove(keyTextVisible(appWidgetId))
            .remove(keyShapeStyle(appWidgetId))
            .apply()
    }

    /**
     * Returns a set of all widget IDs that have saved configurations.
     */
    fun getAllConfiguredWidgetIds(): Set<Int> {
        return preferences.all.keys.mapNotNull { key ->
            keyRegex.find(key)?.groupValues?.get(1)?.toIntOrNull()
        }.toSet()
    }

    companion object {
        const val PREFS_NAME = "music_widget_preferences"

        private val keyRegex = Regex("""^widget_(-?\d+)_.+""")

        fun keyRingStyle(appWidgetId: Int): String = "widget_${appWidgetId}_ring_style"
        fun keyBorderColor(appWidgetId: Int): String = "widget_${appWidgetId}_border_color"
        fun keyDynamicColor(appWidgetId: Int): String = "widget_${appWidgetId}_dynamic_color"
        fun keyTextVisible(appWidgetId: Int): String = "widget_${appWidgetId}_text_visible"
        fun keyShapeStyle(appWidgetId: Int): String = "widget_${appWidgetId}_shape_style"
        fun keyMinimalOverlay(appWidgetId: Int): String = "widget_${appWidgetId}_minimal_overlay"
    }

    /**
     * Toggles or gets the dimmed overlay visibility state for 2x2 Minimal widget.
     */
    fun isMinimalOverlayVisible(appWidgetId: Int): Boolean {
        return preferences.getBoolean(keyMinimalOverlay(appWidgetId), false)
    }

    fun setMinimalOverlayVisible(appWidgetId: Int, visible: Boolean) {
        preferences.edit().putBoolean(keyMinimalOverlay(appWidgetId), visible).apply()
    }

    fun toggleMinimalOverlay(appWidgetId: Int): Boolean {
        val next = !isMinimalOverlayVisible(appWidgetId)
        setMinimalOverlayVisible(appWidgetId, next)
        return next
    }
}
