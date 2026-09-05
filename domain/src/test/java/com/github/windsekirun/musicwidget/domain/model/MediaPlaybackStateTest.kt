package com.github.windsekirun.musicwidget.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class MediaPlaybackStateTest {

    @Test
    fun progress_whenDurationIsZero_returnsZero() {
        val state = MediaPlaybackState(
            positionMs = 1000L,
            durationMs = 0L
        )
        assertEquals(0f, state.progress, 0.0001f)
    }

    @Test
    fun progress_whenDurationIsNegative_returnsZero() {
        val state = MediaPlaybackState(
            positionMs = 500L,
            durationMs = -1000L
        )
        assertEquals(0f, state.progress, 0.0001f)
    }

    @Test
    fun progress_whenMidwayThrough_returnsProportionalFraction() {
        val state = MediaPlaybackState(
            positionMs = 50_000L,
            durationMs = 100_000L
        )
        assertEquals(0.5f, state.progress, 0.0001f)
    }

    @Test
    fun progress_whenPositionExceedsDuration_clampsToOne() {
        val state = MediaPlaybackState(
            positionMs = 120_000L,
            durationMs = 100_000L
        )
        assertEquals(1f, state.progress, 0.0001f)
    }

    @Test
    fun progress_whenPositionIsNegative_clampsToZero() {
        val state = MediaPlaybackState(
            positionMs = -5000L,
            durationMs = 100_000L
        )
        assertEquals(0f, state.progress, 0.0001f)
    }

    @Test
    fun progress_defaultState_returnsZero() {
        val state = MediaPlaybackState()
        assertEquals(0f, state.progress, 0.0001f)
    }
}
