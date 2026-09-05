package io.github.windsekirun.hibiku.feature.service

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSession
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Bundle
import android.os.PowerManager
import android.service.notification.NotificationListenerService
import android.util.Log
import androidx.core.content.ContextCompat
import io.github.windsekirun.hibiku.domain.model.MediaPlaybackState
import io.github.windsekirun.hibiku.domain.model.QueueItemInfo
import io.github.windsekirun.hibiku.domain.repository.MediaPlaybackRepository
import io.github.windsekirun.hibiku.feature.receiver.PowerConnectionReceiver
import io.github.windsekirun.hibiku.feature.receiver.ScreenStateReceiver
import io.github.windsekirun.hibiku.feature.widget.WidgetUpdateHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel

class MediaNotificationListenerService : NotificationListenerService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private lateinit var mediaSessionManager: MediaSessionManager
    private lateinit var listenerComponent: ComponentName
    private lateinit var ticker: PlaybackTicker
    private lateinit var screenReceiver: ScreenStateReceiver

    private var activeController: MediaController? = null
    private var isReceiverRegistered = false

    private val sessionsChangedListener = MediaSessionManager.OnActiveSessionsChangedListener { controllers ->
        updateActiveSession(controllers)
    }

    private val controllerCallback = object : MediaController.Callback() {
        override fun onPlaybackStateChanged(state: PlaybackState?) {
            activeController?.let { updatePlaybackFromController(it) }
        }

        override fun onMetadataChanged(metadata: MediaMetadata?) {
            activeController?.let { updatePlaybackFromController(it) }
        }

        override fun onQueueChanged(queue: MutableList<MediaSession.QueueItem>?) {
            activeController?.let { updatePlaybackFromController(it) }
        }

        override fun onExtrasChanged(extras: Bundle?) {
            activeController?.let { updatePlaybackFromController(it) }
        }

        override fun onQueueTitleChanged(title: CharSequence?) {
            activeController?.let { updatePlaybackFromController(it) }
        }

        override fun onSessionDestroyed() {
            handleSessionDestroyed()
        }
    }

    override fun onCreate() {
        super.onCreate()
        mediaSessionManager = getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
        listenerComponent = ComponentName(this, MediaNotificationListenerService::class.java)

        ticker = PlaybackTicker(
            repository = MediaPlaybackRepository,
            coroutineScope = serviceScope,
            dispatcher = Dispatchers.Default,
            onTick = {
                WidgetUpdateHelper.updateAllWidgets(this@MediaNotificationListenerService)
            }
        )

        screenReceiver = ScreenStateReceiver { isScreenOn ->
            ticker.setScreenOn(isScreenOn)
        }
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        try {
            mediaSessionManager.addOnActiveSessionsChangedListener(sessionsChangedListener, listenerComponent)
        } catch (e: SecurityException) {
            Log.e(TAG, "Failed to register active sessions changed listener", e)
        }

        registerScreenReceiver()
        registerPowerReceiver()

        val powerManager = getSystemService(Context.POWER_SERVICE) as? PowerManager
        val isScreenOn = powerManager?.isInteractive ?: true
        ticker.setScreenOn(isScreenOn)

        queryActiveSessions()
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        try {
            mediaSessionManager.removeOnActiveSessionsChangedListener(sessionsChangedListener)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to remove active sessions changed listener", e)
        }

        unregisterScreenReceiver()
        unregisterPowerReceiver()
        detachActiveController()
        setupActionHandler(null)
        ticker.stop()
        MediaPlaybackRepository.reset()
        WidgetUpdateHelper.updateAllWidgets(this)
    }

    override fun onDestroy() {
        unregisterScreenReceiver()
        unregisterPowerReceiver()
        detachActiveController()
        setupActionHandler(null)
        ticker.stop()
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun registerScreenReceiver() {
        if (!isReceiverRegistered) {
            try {
                ContextCompat.registerReceiver(
                    this,
                    screenReceiver,
                    ScreenStateReceiver.createIntentFilter(),
                    ContextCompat.RECEIVER_EXPORTED
                )
                isReceiverRegistered = true
            } catch (e: Exception) {
                Log.e(TAG, "Failed to register screen receiver", e)
            }
        }
    }

    private fun unregisterScreenReceiver() {
        if (isReceiverRegistered) {
            try {
                unregisterReceiver(screenReceiver)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to unregister screen receiver", e)
            }
            isReceiverRegistered = false
        }
    }

    private var powerReceiver: PowerConnectionReceiver? = null

    private fun registerPowerReceiver() {
        if (powerReceiver == null) {
            try {
                val receiver = PowerConnectionReceiver()
                val filter = IntentFilter(Intent.ACTION_POWER_CONNECTED)
                ContextCompat.registerReceiver(
                    this,
                    receiver,
                    filter,
                    ContextCompat.RECEIVER_EXPORTED
                )
                powerReceiver = receiver
            } catch (e: Exception) {
                Log.e(TAG, "Failed to register power connection receiver", e)
            }
        }
    }

    private fun unregisterPowerReceiver() {
        powerReceiver?.let {
            try {
                unregisterReceiver(it)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to unregister power connection receiver", e)
            }
        }
        powerReceiver = null
    }

    private fun queryActiveSessions() {
        try {
            val sessions = mediaSessionManager.getActiveSessions(listenerComponent)
            updateActiveSession(sessions)
        } catch (e: SecurityException) {
            Log.e(TAG, "Notification listener permission not granted or session query failed", e)
        }
    }

    private fun updateActiveSession(controllers: List<MediaController>?) {
        val newController = controllers?.firstOrNull { it.playbackState?.state == PlaybackState.STATE_PLAYING }
            ?: controllers?.firstOrNull()

        if (newController?.sessionToken != activeController?.sessionToken) {
            detachActiveController()
            activeController = newController
            newController?.registerCallback(controllerCallback)
            setupActionHandler(newController)
        }

        if (newController != null) {
            updatePlaybackFromController(newController)
        } else {
            setupActionHandler(null)
            ticker.stop()
            MediaPlaybackRepository.reset()
            WidgetUpdateHelper.updateAllWidgets(this)
        }
    }

    private fun detachActiveController() {
        try {
            activeController?.unregisterCallback(controllerCallback)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to unregister controller callback", e)
        }
        activeController = null
    }

    private fun updatePlaybackFromController(controller: MediaController) {
        val playbackState = controller.playbackState
        val metadata = controller.metadata
        val current = MediaPlaybackRepository.playbackState.value

        val isPlaying = when (playbackState?.state) {
            PlaybackState.STATE_PLAYING -> true
            PlaybackState.STATE_PAUSED, PlaybackState.STATE_STOPPED, PlaybackState.STATE_NONE, PlaybackState.STATE_ERROR -> false
            PlaybackState.STATE_BUFFERING, PlaybackState.STATE_CONNECTING, PlaybackState.STATE_FAST_FORWARDING, PlaybackState.STATE_REWINDING -> current.isPlaying
            else -> playbackState?.state == PlaybackState.STATE_PLAYING
        }

        val title = metadata?.getString(MediaMetadata.METADATA_KEY_TITLE)
            ?: metadata?.getString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE)
            ?: ""
        val artist = metadata?.getString(MediaMetadata.METADATA_KEY_ARTIST)
            ?: metadata?.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST)
            ?: metadata?.getString(MediaMetadata.METADATA_KEY_AUTHOR)
            ?: ""
        val albumArt = metadata?.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
            ?: metadata?.getBitmap(MediaMetadata.METADATA_KEY_ART)
        val durationMs = metadata?.getLong(MediaMetadata.METADATA_KEY_DURATION) ?: 0L

        val rawPosition = playbackState?.position ?: 0L
        val isSameTrack = current.title == title && current.artist == artist
        val positionMs = when {
            isSameTrack && rawPosition <= 0L && current.positionMs > 0L && (isPlaying || playbackState?.state == PlaybackState.STATE_BUFFERING) -> current.positionMs
            rawPosition >= 0L -> rawPosition
            else -> 0L
        }

        val isShuffleEnabled = playbackState?.extras?.getBoolean("SHUFFLE_ENABLED") ?: false
        val repeatMode = playbackState?.extras?.getInt("REPEAT_MODE") ?: 0

        val queue = runCatching { controller.queue }.getOrNull()
        val activeQueueId = playbackState?.activeQueueItemId
        val trackNumber = metadata?.getLong(MediaMetadata.METADATA_KEY_TRACK_NUMBER)?.toInt() ?: 0
        val numTracks = metadata?.getLong(MediaMetadata.METADATA_KEY_NUM_TRACKS)?.toInt() ?: 0

        val queueSize = when {
            !queue.isNullOrEmpty() -> queue.size
            numTracks > 0 -> numTracks
            else -> 1
        }

        val queueIndex = when {
            !queue.isNullOrEmpty() && activeQueueId != null -> {
                val idx = queue.indexOfFirst { it.queueId == activeQueueId }
                if (idx >= 0) idx + 1 else 1
            }
            trackNumber > 0 -> trackNumber
            else -> 1
        }

        val queueItemsList = runCatching {
            queue?.mapIndexed { index, item ->
                val queueTitle = item.description.title?.toString()?.ifBlank { null }
                    ?: "Track ${index + 1}"
                val queueArtist = item.description.subtitle?.toString()
                    ?: item.description.description?.toString()
                    ?: ""
                QueueItemInfo(
                    queueId = item.queueId,
                    title = queueTitle,
                    artist = queueArtist
                )
            }
        }.getOrNull() ?: emptyList()

        val state = MediaPlaybackState(
            isPlaying = isPlaying,
            title = title,
            artist = artist,
            albumArt = albumArt,
            positionMs = positionMs,
            durationMs = durationMs,
            packageName = controller.packageName,
            sessionActivity = controller.sessionActivity,
            isShuffleEnabled = isShuffleEnabled,
            repeatMode = repeatMode,
            queueIndex = queueIndex,
            queueSize = queueSize,
            queueItems = queueItemsList
        )

        MediaPlaybackRepository.updatePlaybackState(state)
        ticker.onPlaybackStateChanged()
        WidgetUpdateHelper.updateAllWidgets(this)
    }

    private fun handleSessionDestroyed() {
        detachActiveController()
        queryActiveSessions()
        WidgetUpdateHelper.updateAllWidgets(this)
    }

    private fun setupActionHandler(controller: MediaController?) {
        if (controller == null) {
            MediaPlaybackRepository.setActionHandler(null)
            return
        }

        MediaPlaybackRepository.setActionHandler(object : MediaPlaybackRepository.ActionHandler {
            override fun onPlayPause() {
                val repositoryState = MediaPlaybackRepository.playbackState.value.isPlaying
                val sessionState = controller.playbackState?.state
                val isCurrentlyPlaying = repositoryState ||
                        sessionState == PlaybackState.STATE_PLAYING ||
                        sessionState == PlaybackState.STATE_BUFFERING ||
                        sessionState == PlaybackState.STATE_CONNECTING

                if (isCurrentlyPlaying) {
                    controller.transportControls.pause()
                } else {
                    controller.transportControls.play()
                }
            }

            override fun onSkipToNext() {
                controller.transportControls.skipToNext()
            }

            override fun onSkipToPrevious() {
                controller.transportControls.skipToPrevious()
            }

            override fun onSeekTo(positionMs: Long) {
                controller.transportControls.seekTo(positionMs)
            }

            override fun onToggleShuffle() {
                val current = MediaPlaybackRepository.playbackState.value
                val newShuffle = !current.isShuffleEnabled
                MediaPlaybackRepository.updatePlaybackState(current.copy(isShuffleEnabled = newShuffle))
                try {
                    controller.transportControls.sendCustomAction("ACTION_TOGGLE_SHUFFLE", null)
                } catch (_: Exception) {}
            }

            override fun onToggleRepeat() {
                val current = MediaPlaybackRepository.playbackState.value
                val newRepeat = (current.repeatMode + 1) % 3
                MediaPlaybackRepository.updatePlaybackState(current.copy(repeatMode = newRepeat))
                try {
                    controller.transportControls.sendCustomAction("ACTION_TOGGLE_REPEAT", null)
                } catch (_: Exception) {}
            }
        })
    }

    companion object {
        private const val TAG = "MediaNotifListener"
    }
}
