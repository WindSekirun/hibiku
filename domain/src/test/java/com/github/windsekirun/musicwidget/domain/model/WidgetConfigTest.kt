package com.github.windsekirun.musicwidget.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WidgetConfigTest {

    @Test
    fun defaultWidgetConfig_hasExpectedDefaults() {
        val config = WidgetConfig()
        assertEquals(RingStyle.SOLID_CLASSIC, config.ringStyle)
        assertEquals("#FFFFFF", config.borderColorHex)
        assertTrue(config.useDynamicColor)
        assertTrue(config.textVisible)
        assertEquals(M3ShapeStyle.CIRCLE, config.shapeStyle)
    }

    @Test
    fun customWidgetConfig_retainsValues() {
        val config = WidgetConfig(
            ringStyle = RingStyle.SQUIGGLY_WAVE,
            borderColorHex = "#FF5722",
            useDynamicColor = false,
            textVisible = false,
            shapeStyle = M3ShapeStyle.SQUIRCLE
        )
        assertEquals(RingStyle.SQUIGGLY_WAVE, config.ringStyle)
        assertEquals("#FF5722", config.borderColorHex)
        assertEquals(false, config.useDynamicColor)
        assertEquals(false, config.textVisible)
        assertEquals(M3ShapeStyle.SQUIRCLE, config.shapeStyle)
    }

    @Test
    fun ringStyle_containsAllFiveStyles() {
        val styles = RingStyle.entries
        assertEquals(5, styles.size)
        assertTrue(styles.contains(RingStyle.SQUIGGLY_WAVE))
        assertTrue(styles.contains(RingStyle.FLOATING_CLEAN))
        assertTrue(styles.contains(RingStyle.SEGMENTED_MINIMAL))
        assertTrue(styles.contains(RingStyle.GLOW_THUMB))
        assertTrue(styles.contains(RingStyle.SOLID_CLASSIC))
    }

    @Test
    fun m3ShapeStyle_containsAllSevenStyles() {
        val shapes = M3ShapeStyle.entries
        assertEquals(7, shapes.size)
        assertTrue(shapes.contains(M3ShapeStyle.CIRCLE))
        assertTrue(shapes.contains(M3ShapeStyle.SQUIRCLE))
        assertTrue(shapes.contains(M3ShapeStyle.SCALLOP))
        assertTrue(shapes.contains(M3ShapeStyle.CLOVER))
        assertTrue(shapes.contains(M3ShapeStyle.CUT_CORNER))
        assertTrue(shapes.contains(M3ShapeStyle.ARCH))
        assertTrue(shapes.contains(M3ShapeStyle.PILL))
    }
}
