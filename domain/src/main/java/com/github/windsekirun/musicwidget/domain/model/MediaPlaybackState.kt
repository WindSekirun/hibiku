package com.github.windsekirun.musicwidget.domain.model

import android.app.PendingIntent
import android.graphics.Bitmap

data class MediaPlaybackState(
    val isPlaying: Boolean = false,
    val title: String = "",
    val artist: String = "",
    val albumArt: Bitmap? = null,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val packageName: String? = null,
    val sessionActivity: PendingIntent? = null,
    val isShuffleEnabled: Boolean = false,
    val repeatMode: Int = 0 // 0: OFF, 1: ALL, 2: ONE
) {
    val isRepeatEnabled: Boolean
        get() = repeatMode != 0

    val progress: Float
        get() = if (durationMs > 0L) (positionMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f
}
