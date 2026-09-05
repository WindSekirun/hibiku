package io.github.windsekirun.hibiku.feature.ui.immersive

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SquigglySeekBarTest {

    @Test
    fun formatTimeMs_formatsStandardMinutesAndSeconds() {
        assertEquals("00:00", formatTimeMs(0L))
        assertEquals("00:45", formatTimeMs(45_000L))
        assertEquals("01:05", formatTimeMs(65_000L))
        assertEquals("03:45", formatTimeMs(225_000L))
        assertEquals("10:00", formatTimeMs(600_000L))
    }

    @Test
    fun formatTimeMs_handlesNegativeAndZeroGracefully() {
        assertEquals("00:00", formatTimeMs(-1000L))
        assertEquals("00:00", formatTimeMs(-1L))
    }

    @Test
    fun formatTimeMs_formatsHoursWhenExceedingOneHour() {
        assertEquals("1:01:01", formatTimeMs(3_661_000L))
        assertEquals("2:00:00", formatTimeMs(7_200_000L))
    }

    @Test
    fun calculateWaveY_returnsAccurateSineCoordinates() {
        val centerY = 20f
        val amplitude = 6f
        val wavelength = 40f
        val phase = 0f

        // At x = 0: sin(0) = 0
        val y0 = calculateWaveY(0f, centerY, amplitude, wavelength, phase)
        assertEquals(20f, y0, 0.001f)

        // At x = wavelength / 4 (10f): sin(PI/2) = 1 -> y = 20 + 6 = 26
        val yQuarter = calculateWaveY(10f, centerY, amplitude, wavelength, phase)
        assertEquals(26f, yQuarter, 0.001f)

        // At x = wavelength / 2 (20f): sin(PI) = 0 -> y = 20
        val yHalf = calculateWaveY(20f, centerY, amplitude, wavelength, phase)
        assertEquals(20f, yHalf, 0.001f)

        // At x = 3 * wavelength / 4 (30f): sin(3PI/2) = -1 -> y = 20 - 6 = 14
        val yThreeQuarter = calculateWaveY(30f, centerY, amplitude, wavelength, phase)
        assertEquals(14f, yThreeQuarter, 0.001f)

        // At x = wavelength (40f): sin(2PI) = 0 -> y = 20
        val yFull = calculateWaveY(40f, centerY, amplitude, wavelength, phase)
        assertEquals(20f, yFull, 0.001f)
    }

    @Test
    fun generateWavePoints_producesContinuousCurveWithinBounds() {
        val width = 100f
        val centerY = 24f
        val amplitude = 5f
        val wavelength = 20f
        val phase = 0f

        val points = generateWavePoints(
            width = width,
            centerY = centerY,
            amplitude = amplitude,
            wavelength = wavelength,
            phase = phase,
            step = 5f
        )

        assertTrue("Should generate points", points.isNotEmpty())
        assertEquals("Start point x should be 0", 0f, points.first().x, 0.001f)
        assertEquals("End point x should equal width", width, points.last().x, 0.001f)

        points.forEach { point ->
            assertTrue("X should be within [0, $width]", point.x in 0f..width)
            assertTrue(
                "Y should be within amplitude bounds [${centerY - amplitude}, ${centerY + amplitude}]",
                point.y in (centerY - amplitude - 0.01f)..(centerY + amplitude + 0.01f)
            )
        }
    }

    @Test
    fun generateWavePoints_returnsEmptyForZeroOrNegativeWidth() {
        val emptyPointsZero = generateWavePoints(0f, 20f, 4f, 20f, 0f)
        assertTrue(emptyPointsZero.isEmpty())

        val emptyPointsNegative = generateWavePoints(-10f, 20f, 4f, 20f, 0f)
        assertTrue(emptyPointsNegative.isEmpty())
    }

    @Test
    fun calculateSeekPosition_computesCorrectTimestamp() {
        val totalWidth = 200f
        val durationMs = 180_000L // 3 minutes

        // At left edge: 0 ms
        assertEquals(0L, calculateSeekPosition(0f, totalWidth, durationMs))

        // At center: 90_000 ms
        assertEquals(90_000L, calculateSeekPosition(100f, totalWidth, durationMs))

        // At right edge: 180_000 ms
        assertEquals(180_000L, calculateSeekPosition(200f, totalWidth, durationMs))

        // Negative touch clamped to 0
        assertEquals(0L, calculateSeekPosition(-50f, totalWidth, durationMs))

        // Over-drag clamped to durationMs
        assertEquals(180_000L, calculateSeekPosition(250f, totalWidth, durationMs))

        // Invalid width or duration returns 0
        assertEquals(0L, calculateSeekPosition(50f, 0f, durationMs))
        assertEquals(0L, calculateSeekPosition(50f, totalWidth, 0L))
    }
}
