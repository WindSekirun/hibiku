package com.github.windsekirun.musicwidget.feature.data

import android.content.SharedPreferences
import com.github.windsekirun.musicwidget.domain.model.M3ShapeStyle
import com.github.windsekirun.musicwidget.domain.model.RingStyle
import com.github.windsekirun.musicwidget.domain.model.WidgetConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class WidgetPreferencesRepositoryTest {

    private lateinit var fakePreferences: FakeSharedPreferences
    private lateinit var repository: WidgetPreferencesRepository

    @Before
    fun setUp() {
        fakePreferences = FakeSharedPreferences()
        repository = WidgetPreferencesRepository(fakePreferences)
    }

    @Test
    fun loadConfig_returnsDefaultConfigForNewWidgetId() {
        val config = repository.loadConfig(100)

        assertEquals(WidgetConfig(), config)
    }

    @Test
    fun saveConfig_and_loadConfig_persistsCustomConfig() {
        val customConfig = WidgetConfig(
            ringStyle = RingStyle.GLOW_THUMB,
            borderColorHex = "#FF4081",
            useDynamicColor = false,
            textVisible = false,
            shapeStyle = M3ShapeStyle.CLOVER
        )

        repository.saveConfig(101, customConfig)
        val loaded = repository.loadConfig(101)

        assertEquals(customConfig, loaded)
    }

    @Test
    fun saveConfig_usesExactKeyNames() {
        val customConfig = WidgetConfig(
            ringStyle = RingStyle.SQUIGGLY_WAVE,
            borderColorHex = "#123456",
            useDynamicColor = true,
            textVisible = false,
            shapeStyle = M3ShapeStyle.SQUIRCLE
        )

        repository.saveConfig(42, customConfig)

        assertEquals("SQUIGGLY_WAVE", fakePreferences.getString("widget_42_ring_style", null))
        assertEquals("#123456", fakePreferences.getString("widget_42_border_color", null))
        assertEquals(true, fakePreferences.getBoolean("widget_42_dynamic_color", false))
        assertEquals(false, fakePreferences.getBoolean("widget_42_text_visible", true))
        assertEquals("SQUIRCLE", fakePreferences.getString("widget_42_shape_style", null))
    }

    @Test
    fun saveConfig_overwritesExistingConfig() {
        val initialConfig = WidgetConfig(
            ringStyle = RingStyle.SOLID_CLASSIC,
            borderColorHex = "#000000",
            useDynamicColor = true,
            textVisible = true,
            shapeStyle = M3ShapeStyle.CIRCLE
        )
        repository.saveConfig(200, initialConfig)

        val updatedConfig = WidgetConfig(
            ringStyle = RingStyle.SEGMENTED_MINIMAL,
            borderColorHex = "#FFFFFF",
            useDynamicColor = false,
            textVisible = false,
            shapeStyle = M3ShapeStyle.ARCH
        )
        repository.saveConfig(200, updatedConfig)

        val loaded = repository.loadConfig(200)
        assertEquals(updatedConfig, loaded)
    }

    @Test
    fun removeConfig_removesAllKeysForGivenWidgetId() {
        val config1 = WidgetConfig(ringStyle = RingStyle.FLOATING_CLEAN)
        val config2 = WidgetConfig(ringStyle = RingStyle.GLOW_THUMB)

        repository.saveConfig(1, config1)
        repository.saveConfig(2, config2)

        repository.removeConfig(1)

        assertEquals(WidgetConfig(), repository.loadConfig(1))
        assertEquals(config2, repository.loadConfig(2))

        assertFalse(fakePreferences.contains("widget_1_ring_style"))
        assertFalse(fakePreferences.contains("widget_1_border_color"))
        assertFalse(fakePreferences.contains("widget_1_dynamic_color"))
        assertFalse(fakePreferences.contains("widget_1_text_visible"))
        assertFalse(fakePreferences.contains("widget_1_shape_style"))
    }

    @Test
    fun getAllConfiguredWidgetIds_returnsAllConfiguredIds() {
        repository.saveConfig(10, WidgetConfig(ringStyle = RingStyle.FLOATING_CLEAN))
        repository.saveConfig(25, WidgetConfig(ringStyle = RingStyle.SQUIGGLY_WAVE))
        repository.saveConfig(99, WidgetConfig(ringStyle = RingStyle.SOLID_CLASSIC))

        val ids = repository.getAllConfiguredWidgetIds()
        assertEquals(setOf(10, 25, 99), ids)

        repository.removeConfig(25)
        assertEquals(setOf(10, 99), repository.getAllConfiguredWidgetIds())
    }

    @Test
    fun loadConfig_handlesInvalidRingStyleSafelyWithDefault() {
        fakePreferences.edit()
            .putString("widget_55_ring_style", "INVALID_STYLE_NAME")
            .putString("widget_55_border_color", "#112233")
            .apply()

        val loaded = repository.loadConfig(55)

        assertEquals(RingStyle.SOLID_CLASSIC, loaded.ringStyle)
        assertEquals("#112233", loaded.borderColorHex)
    }

    @Test
    fun loadConfig_handlesInvalidShapeStyleSafelyWithDefault() {
        fakePreferences.edit()
            .putString("widget_77_shape_style", "NON_EXISTENT_SHAPE")
            .apply()

        val loaded = repository.loadConfig(77)

        assertEquals(M3ShapeStyle.CIRCLE, loaded.shapeStyle)
    }

    @Test
    fun getAllConfiguredWidgetIds_ignoresUnrelatedKeys() {
        fakePreferences.edit()
            .putString("some_other_preference_key", "value")
            .putString("widget_invalid_key", "test")
            .apply()

        repository.saveConfig(12, WidgetConfig())

        val ids = repository.getAllConfiguredWidgetIds()
        assertEquals(setOf(12), ids)
    }
}

/**
 * In-memory test implementation of [SharedPreferences] for unit testing.
 */
class FakeSharedPreferences : SharedPreferences {
    private val storage = mutableMapOf<String, Any?>()

    override fun getAll(): Map<String, *> = HashMap(storage)

    override fun getString(key: String?, defValue: String?): String? {
        return storage[key] as? String ?: defValue
    }

    @Suppress("UNCHECKED_CAST")
    override fun getStringSet(key: String?, defValues: Set<String>?): Set<String>? {
        return storage[key] as? Set<String> ?: defValues
    }

    override fun getInt(key: String?, defValue: Int): Int {
        return storage[key] as? Int ?: defValue
    }

    override fun getLong(key: String?, defValue: Long): Long {
        return storage[key] as? Long ?: defValue
    }

    override fun getFloat(key: String?, defValue: Float): Float {
        return storage[key] as? Float ?: defValue
    }

    override fun getBoolean(key: String?, defValue: Boolean): Boolean {
        return storage[key] as? Boolean ?: defValue
    }

    override fun contains(key: String?): Boolean {
        return storage.containsKey(key)
    }

    override fun edit(): SharedPreferences.Editor = FakeEditor(storage)

    override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {}

    override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {}

    class FakeEditor(private val storage: MutableMap<String, Any?>) : SharedPreferences.Editor {
        private val modifications = mutableMapOf<String, Any?>()
        private val removals = mutableSetOf<String>()
        private var clear = false

        override fun putString(key: String?, value: String?): SharedPreferences.Editor = apply {
            if (key != null) {
                modifications[key] = value
                removals.remove(key)
            }
        }

        override fun putStringSet(key: String?, values: Set<String>?): SharedPreferences.Editor = apply {
            if (key != null) {
                modifications[key] = values
                removals.remove(key)
            }
        }

        override fun putInt(key: String?, value: Int): SharedPreferences.Editor = apply {
            if (key != null) {
                modifications[key] = value
                removals.remove(key)
            }
        }

        override fun putLong(key: String?, value: Long): SharedPreferences.Editor = apply {
            if (key != null) {
                modifications[key] = value
                removals.remove(key)
            }
        }

        override fun putFloat(key: String?, value: Float): SharedPreferences.Editor = apply {
            if (key != null) {
                modifications[key] = value
                removals.remove(key)
            }
        }

        override fun putBoolean(key: String?, value: Boolean): SharedPreferences.Editor = apply {
            if (key != null) {
                modifications[key] = value
                removals.remove(key)
            }
        }

        override fun remove(key: String?): SharedPreferences.Editor = apply {
            if (key != null) {
                removals.add(key)
                modifications.remove(key)
            }
        }

        override fun clear(): SharedPreferences.Editor = apply {
            clear = true
        }

        override fun commit(): Boolean {
            apply()
            return true
        }

        override fun apply() {
            if (clear) {
                storage.clear()
            }
            for (key in removals) {
                storage.remove(key)
            }
            storage.putAll(modifications)
        }
    }
}
