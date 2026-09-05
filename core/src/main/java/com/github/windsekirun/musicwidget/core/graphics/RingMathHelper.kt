package com.github.windsekirun.musicwidget.core.graphics

import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

object RingMathHelper {

    fun calculateSweepAngle(progress: Float): Float {
        return progress.coerceIn(0f, 1f) * 360f
    }

    fun calculateThumbAngle(progress: Float, startAngle: Float = -90f): Float {
        return startAngle + calculateSweepAngle(progress)
    }

    fun calculateSquigglyRadialOffset(
        angleRad: Double,
        waveCount: Int,
        amplitude: Float,
        phase: Float = 0f
    ): Float {
        return (sin(angleRad * waveCount + phase) * amplitude).toFloat()
    }

    fun calculateSegmentTicks(progress: Float, totalSegments: Int = 36): List<Boolean> {
        require(totalSegments > 0) { "totalSegments must be positive" }
        val activeCount = (progress.coerceIn(0f, 1f) * totalSegments).roundToInt()
        return List(totalSegments) { index -> index < activeCount }
    }

    fun calculateThumbPosition(
        cx: Float,
        cy: Float,
        radius: Float,
        angleDeg: Float
    ): Pair<Float, Float> {
        val angleRad = Math.toRadians(angleDeg.toDouble())
        val x = cx + radius * cos(angleRad).toFloat()
        val y = cy + radius * sin(angleRad).toFloat()
        return Pair(x, y)
    }
}
