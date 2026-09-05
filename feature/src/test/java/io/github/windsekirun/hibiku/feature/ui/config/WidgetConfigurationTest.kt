package io.github.windsekirun.hibiku.feature.ui.config

import io.github.windsekirun.hibiku.domain.model.M3ShapeStyle
import io.github.windsekirun.hibiku.domain.model.RingStyle
import io.github.windsekirun.hibiku.domain.model.WidgetConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WidgetConfigurationTest {

    @Test
    fun ringStyleOptions_containsAllFiveRingStyles() {
        val configuredStyles = RING_STYLE_OPTIONS.map { it.style }

        assertEquals(5, RING_STYLE_OPTIONS.size)
        assertTrue(configuredStyles.contains(RingStyle.SQUIGGLY_WAVE))
        assertTrue(configuredStyles.contains(RingStyle.FLOATING_CLEAN))
        assertTrue(configuredStyles.contains(RingStyle.SEGMENTED_MINIMAL))
        assertTrue(configuredStyles.contains(RingStyle.GLOW_THUMB))
        assertTrue(configuredStyles.contains(RingStyle.SOLID_CLASSIC))
    }

    @Test
    fun widgetConfig_updatesStateCorrectly() {
        val initial = WidgetConfig()
        assertEquals(RingStyle.SOLID_CLASSIC, initial.ringStyle)
        assertEquals("#FFFFFF", initial.borderColorHex)
        assertTrue(initial.useDynamicColor)
        assertTrue(initial.textVisible)
        assertEquals(M3ShapeStyle.CIRCLE, initial.shapeStyle)

        val updated = initial.copy(
            ringStyle = RingStyle.SQUIGGLY_WAVE,
            borderColorHex = "#5CB3FF",
            useDynamicColor = false,
            textVisible = false
        )

        assertEquals(RingStyle.SQUIGGLY_WAVE, updated.ringStyle)
        assertEquals("#5CB3FF", updated.borderColorHex)
        assertEquals(false, updated.useDynamicColor)
        assertEquals(false, updated.textVisible)
    }
}
