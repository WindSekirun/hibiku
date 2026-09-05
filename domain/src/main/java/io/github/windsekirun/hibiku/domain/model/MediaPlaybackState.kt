package io.github.windsekirun.hibiku.domain.model

import android.app.PendingIntent
import android.graphics.Bitmap

data class QueueItemInfo(
    val queueId: Long = -1L,
    val title: String = "",
    val artist: String = ""
)

data class MediaPlaybackState(
    val isPlaying: Boolean = false,
    val title: String = "",
    val artist: String = "",
    val albumArt: Bitmap? = null,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val packageName: String? = null,
    val sessionActivity: PendingIntent? = null,
    val queueIndex: Int = 1,
    val queueSize: Int = 1,
    val queueItems: List<QueueItemInfo> = emptyList()
) {
    val progress: Float
        get() = if (durationMs > 0L) (positionMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f

    val queueText: String
        get() = if (queueSize > 1) "Queue $queueIndex/$queueSize" else if (queueIndex > 1) "Queue $queueIndex" else "Queue 1/1"
}
