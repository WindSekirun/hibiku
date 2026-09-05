package io.github.windsekirun.hibiku.feature.data

import android.content.Context
import android.content.SharedPreferences
import io.github.windsekirun.hibiku.domain.model.M3ShapeStyle
import io.github.windsekirun.hibiku.domain.model.RingStyle
import io.github.windsekirun.hibiku.domain.model.WidgetConfig
import io.github.windsekirun.hibiku.feature.ui.immersive.ImmersiveShapeStyle

enum class SeekBarStyle(val label: String) {
    WAVY("물결 웨이브"),
    FLUID_WAVE("플루이드 웨이브")
}

enum class ImmersiveThemeMode(val label: String) {
    DARK("다크 모드"),
    LIGHT("라이트 모드"),
    SYSTEM("시스템 따라감")
}

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
        val editor = preferences.edit()
        putConfig(editor, appWidgetId, config)
        if (appWidgetId == -1) {
            getAllConfiguredWidgetIds().forEach { id ->
                putConfig(editor, id, config)
            }
        }
        editor.commit()
    }

    private fun putConfig(editor: SharedPreferences.Editor, id: Int, config: WidgetConfig) {
        editor.putString(keyRingStyle(id), config.ringStyle.name)
            .putString(keyBorderColor(id), config.borderColorHex)
            .putBoolean(keyDynamicColor(id), config.useDynamicColor)
            .putBoolean(keyTextVisible(id), config.textVisible)
            .putString(keyShapeStyle(id), config.shapeStyle.name)
            .putBoolean(keyUseBlur(id), config.useBlurBackground)
    }

    /**
     * Loads the [WidgetConfig] for the specified [appWidgetId].
     * Returns default [WidgetConfig] if not found or corrupted.
     */
    fun loadConfig(appWidgetId: Int): WidgetConfig {
        val defaultConfig = if (appWidgetId != -1 && preferences.contains(keyRingStyle(-1))) {
            loadConfigInternal(-1, WidgetConfig())
        } else {
            WidgetConfig()
        }

        return loadConfigInternal(appWidgetId, defaultConfig)
    }

    private fun loadConfigInternal(appWidgetId: Int, defaultConfig: WidgetConfig): WidgetConfig {
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

        val useBlurKey = keyUseBlur(appWidgetId)
        val useBlurBackground = if (preferences.contains(useBlurKey)) {
            preferences.getBoolean(useBlurKey, defaultConfig.useBlurBackground)
        } else {
            defaultConfig.useBlurBackground
        }

        return WidgetConfig(
            ringStyle = ringStyle,
            borderColorHex = borderColorHex,
            useDynamicColor = useDynamicColor,
            textVisible = textVisible,
            shapeStyle = shapeStyle,
            useBlurBackground = useBlurBackground
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
            .remove(keyUseBlur(appWidgetId))
            .commit()
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
        const val KEY_IMMERSIVE_SHAPE = "immersive_shape_style"
        const val KEY_SEEK_BAR_STYLE = "immersive_seek_bar_style"
        const val KEY_IMMERSIVE_THEME = "immersive_theme_mode"
        const val KEY_AUTO_LAUNCH_CHARGING = "auto_launch_charging"

        private val keyRegex = Regex("""^widget_(-?\d+)_.+""")

        fun keyRingStyle(appWidgetId: Int): String = "widget_${appWidgetId}_ring_style"
        fun keyBorderColor(appWidgetId: Int): String = "widget_${appWidgetId}_border_color"
        fun keyDynamicColor(appWidgetId: Int): String = "widget_${appWidgetId}_dynamic_color"
        fun keyTextVisible(appWidgetId: Int): String = "widget_${appWidgetId}_text_visible"
        fun keyShapeStyle(appWidgetId: Int): String = "widget_${appWidgetId}_shape_style"
        fun keyUseBlur(appWidgetId: Int): String = "widget_${appWidgetId}_use_blur"
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

    /**
     * Saves and retrieves global Immersive Player shape style selection.
     */
    fun saveImmersiveShape(style: ImmersiveShapeStyle) {
        preferences.edit().putString(KEY_IMMERSIVE_SHAPE, style.name).apply()
    }

    fun getImmersiveShape(): ImmersiveShapeStyle {
        val name = preferences.getString(KEY_IMMERSIVE_SHAPE, null)
        return name?.let {
            runCatching { ImmersiveShapeStyle.valueOf(it) }.getOrNull()
        } ?: ImmersiveShapeStyle.SCALLOP
    }

    /**
     * Saves and retrieves global Immersive Player seek bar style selection.
     */
    fun saveSeekBarStyle(style: SeekBarStyle) {
        preferences.edit().putString(KEY_SEEK_BAR_STYLE, style.name).apply()
    }

    fun getSeekBarStyle(): SeekBarStyle {
        val name = preferences.getString(KEY_SEEK_BAR_STYLE, null)
        return name?.let {
            runCatching { SeekBarStyle.valueOf(it) }.getOrNull()
        } ?: SeekBarStyle.WAVY
    }

    /**
     * Saves and retrieves Immersive Player theme mode (Dark / Light / Follow System).
     */
    fun saveImmersiveThemeMode(mode: ImmersiveThemeMode) {
        preferences.edit().putString(KEY_IMMERSIVE_THEME, mode.name).apply()
    }

    fun getImmersiveThemeMode(): ImmersiveThemeMode {
        val name = preferences.getString(KEY_IMMERSIVE_THEME, null)
        return name?.let {
            runCatching { ImmersiveThemeMode.valueOf(it) }.getOrNull()
        } ?: ImmersiveThemeMode.DARK
    }

    /**
     * Saves and retrieves auto-launch Immersive StandBy Player on charging preference.
     */
    fun saveAutoLaunchOnCharging(enabled: Boolean) {
        preferences.edit().putBoolean(KEY_AUTO_LAUNCH_CHARGING, enabled).apply()
    }

    fun isAutoLaunchOnChargingEnabled(): Boolean {
        return preferences.getBoolean(KEY_AUTO_LAUNCH_CHARGING, true)
    }
}
