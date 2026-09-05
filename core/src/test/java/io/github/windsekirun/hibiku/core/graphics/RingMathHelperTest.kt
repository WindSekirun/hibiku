package io.github.windsekirun.hibiku.core.graphics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI

class RingMathHelperTest {

    @Test
    fun calculateSweepAngle_standardValues() {
        assertEquals(0f, RingMathHelper.calculateSweepAngle(0f), 0.001f)
        assertEquals(90f, RingMathHelper.calculateSweepAngle(0.25f), 0.001f)
        assertEquals(180f, RingMathHelper.calculateSweepAngle(0.5f), 0.001f)
        assertEquals(270f, RingMathHelper.calculateSweepAngle(0.75f), 0.001f)
        assertEquals(360f, RingMathHelper.calculateSweepAngle(1.0f), 0.001f)
    }

    @Test
    fun calculateSweepAngle_clampedValues() {
        assertEquals(0f, RingMathHelper.calculateSweepAngle(-0.5f), 0.001f)
        assertEquals(360f, RingMathHelper.calculateSweepAngle(1.5f), 0.001f)
    }

    @Test
    fun calculateThumbAngle_defaultStartAngle() {
        assertEquals(-90f, RingMathHelper.calculateThumbAngle(0f), 0.001f)
        assertEquals(0f, RingMathHelper.calculateThumbAngle(0.25f), 0.001f)
        assertEquals(90f, RingMathHelper.calculateThumbAngle(0.5f), 0.001f)
        assertEquals(180f, RingMathHelper.calculateThumbAngle(0.75f), 0.001f)
        assertEquals(270f, RingMathHelper.calculateThumbAngle(1.0f), 0.001f)
    }

    @Test
    fun calculateThumbAngle_customStartAngle() {
        assertEquals(0f, RingMathHelper.calculateThumbAngle(0f, startAngle = 0f), 0.001f)
        assertEquals(180f, RingMathHelper.calculateThumbAngle(0.5f, startAngle = 0f), 0.001f)
        assertEquals(360f, RingMathHelper.calculateThumbAngle(1.0f, startAngle = 0f), 0.001f)
    }

    @Test
    fun calculateSquigglyRadialOffset_peaksAndTroughs() {
        val waveCount = 12
        val amplitude = 4f

        // At angle 0, offset should be 0
        val offsetZero = RingMathHelper.calculateSquigglyRadialOffset(0.0, waveCount, amplitude, phase = 0f)
        assertEquals(0f, offsetZero, 0.001f)

        // Peak: angle * waveCount = PI / 2 => angle = PI / (2 * 12)
        val anglePeak = PI / (2.0 * waveCount)
        val offsetPeak = RingMathHelper.calculateSquigglyRadialOffset(anglePeak, waveCount, amplitude, phase = 0f)
        assertEquals(4f, offsetPeak, 0.001f)

        // Trough: angle * waveCount = 3 * PI / 2 => angle = 3 * PI / (2 * 12)
        val angleTrough = 3.0 * PI / (2.0 * waveCount)
        val offsetTrough = RingMathHelper.calculateSquigglyRadialOffset(angleTrough, waveCount, amplitude, phase = 0f)
        assertEquals(-4f, offsetTrough, 0.001f)

        // Phase shift: phase = PI / 2 at angle 0 gives peak
        val offsetWithPhase = RingMathHelper.calculateSquigglyRadialOffset(0.0, waveCount, amplitude, phase = (PI / 2.0).toFloat())
        assertEquals(4f, offsetWithPhase, 0.001f)
    }

    @Test
    fun calculateSegmentTicks_countsAndStates() {
        val totalSegments = 36

        // 0% progress -> all false
        val ticks0 = RingMathHelper.calculateSegmentTicks(0f, totalSegments)
        assertEquals(36, ticks0.size)
        assertTrue(ticks0.none { it })

        // 50% progress -> first 18 true, last 18 false
        val ticks50 = RingMathHelper.calculateSegmentTicks(0.5f, totalSegments)
        assertEquals(36, ticks50.size)
        assertEquals(18, ticks50.count { it })
        for (i in 0 until 18) {
            assertTrue("Segment $i should be active", ticks50[i])
        }
        for (i in 18 until 36) {
            assertFalse("Segment $i should be inactive", ticks50[i])
        }

        // 100% progress -> all true
        val ticks100 = RingMathHelper.calculateSegmentTicks(1f, totalSegments)
        assertEquals(36, ticks100.size)
        assertTrue(ticks100.all { it })

        // Clamping negative
        val ticksNeg = RingMathHelper.calculateSegmentTicks(-0.2f, totalSegments)
        assertTrue(ticksNeg.none { it })

        // Clamping overflow
        val ticksOverflow = RingMathHelper.calculateSegmentTicks(1.5f, totalSegments)
        assertTrue(ticksOverflow.all { it })
    }

    @Test
    fun calculateThumbPosition_cardinalPoints() {
        val cx = 100f
        val cy = 100f
        val radius = 50f

        // 0 deg -> right
        val (xRight, yRight) = RingMathHelper.calculateThumbPosition(cx, cy, radius, 0f)
        assertEquals(150f, xRight, 0.001f)
        assertEquals(100f, yRight, 0.001f)

        // 90 deg -> down
        val (xDown, yDown) = RingMathHelper.calculateThumbPosition(cx, cy, radius, 90f)
        assertEquals(100f, xDown, 0.001f)
        assertEquals(150f, yDown, 0.001f)

        // 180 deg -> left
        val (xLeft, yLeft) = RingMathHelper.calculateThumbPosition(cx, cy, radius, 180f)
        assertEquals(50f, xLeft, 0.001f)
        assertEquals(100f, yLeft, 0.001f)

        // -90 deg -> up
        val (xUp, yUp) = RingMathHelper.calculateThumbPosition(cx, cy, radius, -90f)
        assertEquals(100f, xUp, 0.001f)
        assertEquals(50f, yUp, 0.001f)
    }
}
