package io.github.windsekirun.hibiku.feature.service

import android.app.Notification
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSession
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.net.Uri
import android.os.Bundle
import android.os.PowerManager
import android.os.SystemClock
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
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
    private val controllerCallbacks = mutableMapOf<MediaSession.Token, Pair<MediaController, MediaController.Callback>>()

    private val sessionsChangedListener = MediaSessionManager.OnActiveSessionsChangedListener { controllers ->
        updateActiveSession(controllers)
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null) return
        val extras = sbn.notification.extras ?: return
        val hasMediaSession = extras.containsKey(Notification.EXTRA_MEDIA_SESSION)
        val isMediaCategory = sbn.notification.category == Notification.CATEGORY_TRANSPORT
        if (hasMediaSession || isMediaCategory) {
            queryActiveSessions()
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
        queryActiveSessions()
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
        detachAllControllers()
        setupActionHandler(null)
        ticker.stop()
        MediaPlaybackRepository.reset()
        WidgetUpdateHelper.updateAllWidgets(this)
    }

    override fun onDestroy() {
        unregisterScreenReceiver()
        unregisterPowerReceiver()
        detachAllControllers()
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
        val currentTokens = controllers?.map { it.sessionToken }?.toSet() ?: emptySet()
        val tokensToRemove = controllerCallbacks.keys - currentTokens
        tokensToRemove.forEach { token ->
            controllerCallbacks.remove(token)?.let { (controller, callback) ->
                try {
                    controller.unregisterCallback(callback)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to unregister controller callback", e)
                }
            }
        }

        controllers?.forEach { controller ->
            val token = controller.sessionToken
            if (!controllerCallbacks.containsKey(token)) {
                val callback = object : MediaController.Callback() {
                    override fun onPlaybackStateChanged(state: PlaybackState?) {
                        handleControllerEvent(controller)
                    }

                    override fun onMetadataChanged(metadata: MediaMetadata?) {
                        handleControllerEvent(controller)
                    }

                    override fun onQueueChanged(queue: MutableList<MediaSession.QueueItem>?) {
                        handleControllerEvent(controller)
                    }

                    override fun onExtrasChanged(extras: Bundle?) {
                        handleControllerEvent(controller)
                    }

                    override fun onQueueTitleChanged(title: CharSequence?) {
                        handleControllerEvent(controller)
                    }

                    override fun onSessionDestroyed() {
                        handleSessionDestroyed()
                    }
                }
                try {
                    controller.registerCallback(callback)
                    controllerCallbacks[token] = Pair(controller, callback)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to register controller callback", e)
                }
            }
        }

        val newController = controllers?.firstOrNull { it.playbackState?.state == PlaybackState.STATE_PLAYING }
            ?: controllers?.firstOrNull {
                val s = it.playbackState?.state
                s == PlaybackState.STATE_BUFFERING || s == PlaybackState.STATE_CONNECTING
            }
            ?: controllers?.firstOrNull()

        if (newController?.sessionToken != activeController?.sessionToken) {
            activeController = newController
            setupActionHandler(newController)
        }

        if (newController != null) {
            updatePlaybackFromController(newController)
        } else {
            activeController = null
            setupActionHandler(null)
            ticker.stop()
            MediaPlaybackRepository.reset()
            WidgetUpdateHelper.updateAllWidgets(this)
        }
    }

    private fun handleControllerEvent(controller: MediaController) {
        val state = controller.playbackState?.state
        val isPlaying = state == PlaybackState.STATE_PLAYING ||
                state == PlaybackState.STATE_BUFFERING ||
                state == PlaybackState.STATE_CONNECTING

        if (isPlaying || activeController == null || activeController?.playbackState?.state != PlaybackState.STATE_PLAYING || activeController?.sessionToken == controller.sessionToken) {
            if (activeController?.sessionToken != controller.sessionToken) {
                activeController = controller
                setupActionHandler(controller)
            }
            updatePlaybackFromController(controller)
        }
    }

    private fun detachAllControllers() {
        controllerCallbacks.values.forEach { (controller, callback) ->
            try {
                controller.unregisterCallback(callback)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to unregister controller callback", e)
            }
        }
        controllerCallbacks.clear()
        activeController = null
    }

    private fun loadBitmapFromUri(uriString: String?): Bitmap? {
        if (uriString.isNullOrBlank()) return null
        return try {
            val uri = Uri.parse(uriString)
            if (uri.scheme == "content" || uri.scheme == "android.resource" || uri.scheme == "file") {
                contentResolver.openInputStream(uri)?.use { stream ->
                    BitmapFactory.decodeStream(stream)
                }
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun getActiveNotificationLargeIcon(packageName: String): Bitmap? {
        return try {
            activeNotifications?.firstOrNull { it.packageName == packageName }?.notification?.let { notif ->
                notif.getLargeIcon()?.loadDrawable(this)?.toBitmap()
                    ?: notif.extras?.getParcelable(Notification.EXTRA_LARGE_ICON, Bitmap::class.java)
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun getActiveNotificationTitle(packageName: String): String? {
        return try {
            activeNotifications?.firstOrNull { it.packageName == packageName }?.notification?.extras
                ?.getCharSequence(Notification.EXTRA_TITLE)?.toString()?.ifBlank { null }
        } catch (_: Exception) {
            null
        }
    }

    private fun getActiveNotificationText(packageName: String): String? {
        return try {
            activeNotifications?.firstOrNull { it.packageName == packageName }?.notification?.extras
                ?.getCharSequence(Notification.EXTRA_TEXT)?.toString()?.ifBlank { null }
        } catch (_: Exception) {
            null
        }
    }

    private fun calculateRealtimePosition(playbackState: PlaybackState?): Long {
        if (playbackState == null) return 0L
        val rawPosition = playbackState.position
        if (playbackState.state != PlaybackState.STATE_PLAYING) {
            return rawPosition.coerceAtLeast(0L)
        }
        val updateTime = playbackState.lastPositionUpdateTime
        if (updateTime > 0) {
            val elapsed = SystemClock.elapsedRealtime() - updateTime
            if (elapsed > 0) {
                val speed = if (playbackState.playbackSpeed > 0f) playbackState.playbackSpeed else 1.0f
                return (rawPosition + (elapsed * speed).toLong()).coerceAtLeast(0L)
            }
        }
        return rawPosition.coerceAtLeast(0L)
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
            ?: metadata?.description?.title?.toString()
            ?: getActiveNotificationTitle(controller.packageName)
            ?: ""
        val artist = metadata?.getString(MediaMetadata.METADATA_KEY_ARTIST)
            ?: metadata?.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST)
            ?: metadata?.getString(MediaMetadata.METADATA_KEY_AUTHOR)
            ?: metadata?.getString(MediaMetadata.METADATA_KEY_DISPLAY_SUBTITLE)
            ?: metadata?.getString(MediaMetadata.METADATA_KEY_DISPLAY_DESCRIPTION)
            ?: metadata?.description?.subtitle?.toString()
            ?: getActiveNotificationText(controller.packageName)
            ?: ""
        val albumArt = metadata?.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
            ?: metadata?.getBitmap(MediaMetadata.METADATA_KEY_ART)
            ?: metadata?.getBitmap(MediaMetadata.METADATA_KEY_DISPLAY_ICON)
            ?: metadata?.description?.iconBitmap
            ?: loadBitmapFromUri(metadata?.getString(MediaMetadata.METADATA_KEY_ALBUM_ART_URI))
            ?: loadBitmapFromUri(metadata?.getString(MediaMetadata.METADATA_KEY_ART_URI))
            ?: loadBitmapFromUri(metadata?.description?.iconUri?.toString())
            ?: getActiveNotificationLargeIcon(controller.packageName)
        val durationMs = metadata?.getLong(MediaMetadata.METADATA_KEY_DURATION) ?: 0L

        val realPosition = calculateRealtimePosition(playbackState)
        val positionMs = if (durationMs > 0L) realPosition.coerceAtMost(durationMs) else realPosition

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
            queueIndex = queueIndex,
            queueSize = queueSize,
            queueItems = queueItemsList
        )

        MediaPlaybackRepository.updatePlaybackState(state)
        ticker.onPlaybackStateChanged()
        WidgetUpdateHelper.updateAllWidgets(this)
    }

    private fun handleSessionDestroyed() {
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

            override fun onSkipToQueueItem(queueId: Long) {
                if (queueId >= 0) {
                    try {
                        controller.transportControls.skipToQueueItem(queueId)
                    } catch (_: Exception) {}
                }
            }

            override fun onSeekTo(positionMs: Long) {
                MediaPlaybackRepository.updatePosition(positionMs)
                controller.transportControls.seekTo(positionMs)
                WidgetUpdateHelper.updateAllWidgets(this@MediaNotificationListenerService)
            }
        })
    }

    companion object {
        private const val TAG = "MediaNotifListener"
    }
}
