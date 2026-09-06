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
import io.github.windsekirun.hibiku.feature.widget.WidgetUpdateHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel

class MediaNotificationListenerService : NotificationListenerService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private lateinit var mediaSessionManager: MediaSessionManager
    private lateinit var audioManager: android.media.AudioManager
    private lateinit var listenerComponent: ComponentName

    private var activeController: MediaController? = null
    private var isAudioCallbackRegistered = false
    private val controllerCallbacks = mutableMapOf<MediaSession.Token, Pair<MediaController, MediaController.Callback>>()

    private val audioPlaybackCallback = object : android.media.AudioManager.AudioPlaybackCallback() {
        override fun onPlaybackConfigChanged(configs: MutableList<android.media.AudioPlaybackConfiguration>?) {
            super.onPlaybackConfigChanged(configs)
            handleAudioPlaybackConfigChanged(configs)
        }
    }

    private val sessionsChangedListener = MediaSessionManager.OnActiveSessionsChangedListener { controllers ->
        updateActiveSession(controllers)
    }

    private val latestMediaNotifications = java.util.concurrent.ConcurrentHashMap<String, StatusBarNotification>()

    private fun isMediaNotification(sbn: StatusBarNotification): Boolean {
        val notif = sbn.notification ?: return false
        val extras = notif.extras
        if (extras?.containsKey(Notification.EXTRA_MEDIA_SESSION) == true) return true
        if (notif.category == Notification.CATEGORY_TRANSPORT) return true
        val template = extras?.getString(Notification.EXTRA_TEMPLATE) ?: ""
        if (template.contains("MediaStyle", ignoreCase = true)) return true
        val channelId = notif.channelId ?: ""
        if (channelId.contains("media", ignoreCase = true)) return true
        if (controllerCallbacks.values.any { it.first.packageName == sbn.packageName }) return true
        try {
            if (mediaSessionManager.getActiveSessions(listenerComponent).any { it.packageName == sbn.packageName }) return true
        } catch (_: Exception) {}
        val actions = notif.actions
        if (actions != null && actions.isNotEmpty()) {
            val mediaWords = listOf("play", "pause", "next", "prev", "skip", "재생", "정지", "다음", "이전")
            if (actions.any { a -> a.title != null && mediaWords.any { w -> a.title.toString().contains(w, ignoreCase = true) } }) {
                return true
            }
        }
        return false
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null) return
        val isMedia = isMediaNotification(sbn)
        Log.i(TAG, "[onNotificationPosted] pkg=${sbn.packageName}, isMedia=$isMedia, cat=${sbn.notification.category}, ch=${sbn.notification.channelId}")
        if (isMedia) {
            latestMediaNotifications[sbn.packageName] = sbn
            val matchingController = controllerCallbacks.values.firstOrNull { it.first.packageName == sbn.packageName }?.first
                ?: try {
                    mediaSessionManager.getActiveSessions(listenerComponent).firstOrNull { it.packageName == sbn.packageName }
                } catch (_: Exception) { null }

            if (matchingController != null) {
                if (activeController?.sessionToken != matchingController.sessionToken) {
                    Log.i(TAG, "[onNotificationPosted] Switching activeController to ${matchingController.packageName}")
                    activeController = matchingController
                    setupActionHandler(matchingController)
                }
                updatePlaybackFromController(matchingController, sbn)
            } else {
                queryActiveSessions()
            }
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
        if (sbn != null) {
            latestMediaNotifications.remove(sbn.packageName)
        }
        Log.i(TAG, "[onNotificationRemoved] pkg=${sbn?.packageName}")
        queryActiveSessions()
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        Log.i(TAG, "[onCreate] Service created")
        mediaSessionManager = getSystemService(MEDIA_SESSION_SERVICE) as MediaSessionManager
        audioManager = getSystemService(AUDIO_SERVICE) as android.media.AudioManager
        listenerComponent = ComponentName(this, MediaNotificationListenerService::class.java)
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        Log.i(TAG, "[onListenerConnected] Connected to NotificationManagerService")
        try {
            mediaSessionManager.addOnActiveSessionsChangedListener(sessionsChangedListener, listenerComponent)
        } catch (e: SecurityException) {
            Log.e(TAG, "Failed to register active sessions changed listener", e)
        }

        registerAudioPlaybackCallback()
        registerPowerReceiver()

        queryActiveSessions()
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        Log.i(TAG, "[onListenerDisconnected] Disconnected from NotificationManagerService")
        try {
            mediaSessionManager.removeOnActiveSessionsChangedListener(sessionsChangedListener)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to remove active sessions changed listener", e)
        }

        unregisterAudioPlaybackCallback()
        unregisterPowerReceiver()
        detachAllControllers()
        setupActionHandler(null)
        MediaPlaybackRepository.reset()
        WidgetUpdateHelper.updateAllWidgets(this)
    }

    override fun onDestroy() {
        if (instance == this) {
            instance = null
        }
        unregisterAudioPlaybackCallback()
        unregisterPowerReceiver()
        detachAllControllers()
        setupActionHandler(null)
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun registerAudioPlaybackCallback() {
        if (!isAudioCallbackRegistered) {
            try {
                audioManager.registerAudioPlaybackCallback(audioPlaybackCallback, android.os.Handler(android.os.Looper.getMainLooper()))
                isAudioCallbackRegistered = true
                Log.i(TAG, "[registerAudioPlaybackCallback] Registered AudioPlaybackCallback successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to register audio playback callback", e)
            }
        }
    }

    private fun unregisterAudioPlaybackCallback() {
        if (isAudioCallbackRegistered) {
            try {
                audioManager.unregisterAudioPlaybackCallback(audioPlaybackCallback)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to unregister audio playback callback", e)
            }
            isAudioCallbackRegistered = false
        }
    }

    private fun handleAudioPlaybackConfigChanged(configs: List<android.media.AudioPlaybackConfiguration>?) {
        if (configs == null) return
        Log.i(TAG, "[AudioPlaybackCallback] configs size=${configs.size}")
        var hasActiveMedia = false
        var activeUid: Int? = null

        val uidPattern = Regex("""u/pid:(\d+)/\d+""")
        for (config in configs) {
            val usage = config.audioAttributes?.usage
            val isMediaUsage = usage == android.media.AudioAttributes.USAGE_MEDIA ||
                    usage == android.media.AudioAttributes.USAGE_GAME ||
                    usage == android.media.AudioAttributes.USAGE_UNKNOWN

            val configStr = config.toString()
            val isStarted = configStr.contains("state:started", ignoreCase = true)

            if (isMediaUsage && isStarted) {
                hasActiveMedia = true
                val match = uidPattern.find(configStr)
                if (match != null) {
                    activeUid = match.groupValues[1].toIntOrNull()
                    Log.i(TAG, "[AudioPlaybackCallback] Found active media track with UID=$activeUid: $configStr")
                    break
                }
            }
        }

        if (hasActiveMedia) {
            var activePackageNames: List<String> = emptyList()
            if (activeUid != null) {
                try {
                    val pkgs = packageManager.getPackagesForUid(activeUid)
                    if (pkgs != null) {
                        activePackageNames = pkgs.toList()
                        Log.i(TAG, "[AudioPlaybackCallback] Active UID $activeUid maps to packages: $activePackageNames")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to get packages for UID $activeUid", e)
                }
            }

            try {
                val sessions = mediaSessionManager.getActiveSessions(listenerComponent)
                if (activePackageNames.isNotEmpty()) {
                    val matchingController = sessions.firstOrNull { activePackageNames.contains(it.packageName) }
                    if (matchingController != null) {
                        if (activeController?.sessionToken != matchingController.sessionToken) {
                            Log.i(TAG, "[AudioPlaybackCallback] Switching activeController to hardware-playing session: ${matchingController.packageName}")
                            activeController = matchingController
                            setupActionHandler(matchingController)
                        }
                        updatePlaybackFromController(matchingController)
                        return
                    }
                }
                updateActiveSession(sessions)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to query active sessions on audio playback config change", e)
            }
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

    private fun queryActiveSessions(forceWidgetUpdate: Boolean = false) {
        try {
            val sessions = mediaSessionManager.getActiveSessions(listenerComponent)
            Log.i(TAG, "[queryActiveSessions] count=${sessions.size}, pkgs=${sessions.map { it.packageName }}")
            updateActiveSession(sessions, forceWidgetUpdate = forceWidgetUpdate)
        } catch (e: SecurityException) {
            Log.e(TAG, "Notification listener permission not granted or session query failed", e)
        }
    }

    private fun updateActiveSession(controllers: List<MediaController>?, forceWidgetUpdate: Boolean = false) {
        val currentTokens = controllers?.map { it.sessionToken }?.toSet() ?: emptySet()
        val tokensToRemove = controllerCallbacks.keys - currentTokens
        tokensToRemove.forEach { token ->
            controllerCallbacks.remove(token)?.let { (controller, callback) ->
                try {
                    Log.i(TAG, "[unregisterCallback] pkg=${controller.packageName}")
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
                        Log.i(TAG, "[Callback.onPlaybackStateChanged] pkg=${controller.packageName}, state=${state?.state}, pos=${state?.position}, speed=${state?.playbackSpeed}, updated=${state?.lastPositionUpdateTime}")
                        handleControllerEvent(controller)
                    }

                    override fun onMetadataChanged(metadata: MediaMetadata?) {
                        Log.i(TAG, "[Callback.onMetadataChanged] pkg=${controller.packageName}, title='${metadata?.getString(MediaMetadata.METADATA_KEY_TITLE)}', descTitle='${metadata?.description?.title}', artist='${metadata?.getString(MediaMetadata.METADATA_KEY_ARTIST)}', duration=${metadata?.getLong(MediaMetadata.METADATA_KEY_DURATION)}")
                        handleControllerEvent(controller, isMetadataEvent = true)
                    }

                    override fun onQueueChanged(queue: MutableList<MediaSession.QueueItem>?) {
                        Log.i(TAG, "[Callback.onQueueChanged] pkg=${controller.packageName}, size=${queue?.size}")
                        handleControllerEvent(controller)
                    }

                    override fun onExtrasChanged(extras: Bundle?) {
                        Log.i(TAG, "[Callback.onExtrasChanged] pkg=${controller.packageName}")
                        handleControllerEvent(controller)
                    }

                    override fun onQueueTitleChanged(title: CharSequence?) {
                        Log.i(TAG, "[Callback.onQueueTitleChanged] pkg=${controller.packageName}, title=$title")
                        handleControllerEvent(controller)
                    }

                    override fun onSessionDestroyed() {
                        Log.i(TAG, "[Callback.onSessionDestroyed] pkg=${controller.packageName}")
                        handleSessionDestroyed()
                    }
                }
                try {
                    Log.i(TAG, "[registerCallback] pkg=${controller.packageName}, token=$token")
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

        Log.i(TAG, "[updateActiveSession] totalControllers=${controllers?.size}, activePkg=${activeController?.packageName}, newPkg=${newController?.packageName}, newState=${newController?.playbackState?.state}")

        if (newController?.sessionToken != activeController?.sessionToken) {
            Log.i(TAG, "[updateActiveSession] Switching activeController from ${activeController?.packageName} to ${newController?.packageName}")
            activeController = newController
            setupActionHandler(newController)
        }

        if (newController != null) {
            updatePlaybackFromController(newController, forceWidgetUpdate = forceWidgetUpdate)
        } else {
            Log.i(TAG, "[updateActiveSession] No active controller found, resetting")
            activeController = null
            setupActionHandler(null)
            MediaPlaybackRepository.reset()
            WidgetUpdateHelper.updateAllWidgets(this)
        }
    }

    private fun handleControllerEvent(controller: MediaController, isMetadataEvent: Boolean = false) {
        val state = controller.playbackState?.state
        val isPlaying = state == PlaybackState.STATE_PLAYING ||
                state == PlaybackState.STATE_BUFFERING ||
                state == PlaybackState.STATE_CONNECTING

        val shouldHandle = isMetadataEvent || isPlaying || activeController == null ||
                activeController?.playbackState?.state != PlaybackState.STATE_PLAYING ||
                activeController?.sessionToken == controller.sessionToken

        Log.i(TAG, "[handleControllerEvent] pkg=${controller.packageName}, state=$state, isPlaying=$isPlaying, isMetadataEvent=$isMetadataEvent, shouldHandle=$shouldHandle (activePkg=${activeController?.packageName})")

        if (shouldHandle) {
            if (activeController?.sessionToken != controller.sessionToken) {
                Log.i(TAG, "[handleControllerEvent] Switching activeController to ${controller.packageName}")
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

    private fun getMediaNotification(packageName: String): Notification? {
        latestMediaNotifications[packageName]?.notification?.let { return it }
        return try {
            activeNotifications?.firstOrNull { it.packageName == packageName && isMediaNotification(it) }?.notification
                ?: activeNotifications?.firstOrNull { it.packageName == packageName }?.notification
        } catch (_: Exception) {
            null
        }
    }

    private fun getActiveNotificationLargeIcon(packageName: String): Bitmap? {
        return try {
            getMediaNotification(packageName)?.let { notif ->
                notif.getLargeIcon()?.loadDrawable(this)?.toBitmap()
                    ?: notif.extras?.getParcelable(Notification.EXTRA_LARGE_ICON, Bitmap::class.java)
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun getActiveNotificationTitle(packageName: String): String? {
        return try {
            val extras = getMediaNotification(packageName)?.extras
            extras?.getCharSequence(Notification.EXTRA_TITLE)?.toString()?.ifBlank { null }
                ?: extras?.getCharSequence(Notification.EXTRA_TITLE_BIG)?.toString()?.ifBlank { null }
        } catch (_: Exception) {
            null
        }
    }

    private fun getActiveNotificationText(packageName: String): String? {
        return try {
            val extras = getMediaNotification(packageName)?.extras
            extras?.getCharSequence(Notification.EXTRA_TEXT)?.toString()?.ifBlank { null }
                ?: extras?.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()?.ifBlank { null }
                ?: extras?.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString()?.ifBlank { null }
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

    private fun updatePlaybackFromController(
        controller: MediaController,
        sbn: StatusBarNotification? = null,
        forceWidgetUpdate: Boolean = false
    ) {
        val playbackState = controller.playbackState
        val metadata = controller.metadata
        val current = MediaPlaybackRepository.playbackState.value

        val notif = sbn?.notification ?: getMediaNotification(controller.packageName)
        val notifExtras = notif?.extras

        val isPlaying = when (playbackState?.state) {
            PlaybackState.STATE_PLAYING -> true
            PlaybackState.STATE_PAUSED, PlaybackState.STATE_STOPPED, PlaybackState.STATE_NONE, PlaybackState.STATE_ERROR -> false
            PlaybackState.STATE_BUFFERING, PlaybackState.STATE_CONNECTING, PlaybackState.STATE_FAST_FORWARDING, PlaybackState.STATE_REWINDING -> current.isPlaying
            else -> playbackState?.state == PlaybackState.STATE_PLAYING
        }

        val title = metadata?.getString(MediaMetadata.METADATA_KEY_TITLE)?.ifBlank { null }
            ?: metadata?.getString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE)?.ifBlank { null }
            ?: metadata?.description?.title?.toString()?.ifBlank { null }
            ?: notifExtras?.getCharSequence(Notification.EXTRA_TITLE)?.toString()?.ifBlank { null }
            ?: notifExtras?.getCharSequence(Notification.EXTRA_TITLE_BIG)?.toString()?.ifBlank { null }
            ?: getActiveNotificationTitle(controller.packageName)
            ?: ""
        val artist = metadata?.getString(MediaMetadata.METADATA_KEY_ARTIST)?.ifBlank { null }
            ?: metadata?.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST)?.ifBlank { null }
            ?: metadata?.getString(MediaMetadata.METADATA_KEY_AUTHOR)?.ifBlank { null }
            ?: metadata?.getString(MediaMetadata.METADATA_KEY_DISPLAY_SUBTITLE)?.ifBlank { null }
            ?: metadata?.getString(MediaMetadata.METADATA_KEY_DISPLAY_DESCRIPTION)?.ifBlank { null }
            ?: metadata?.description?.subtitle?.toString()?.ifBlank { null }
            ?: notifExtras?.getCharSequence(Notification.EXTRA_TEXT)?.toString()?.ifBlank { null }
            ?: notifExtras?.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()?.ifBlank { null }
            ?: notifExtras?.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString()?.ifBlank { null }
            ?: getActiveNotificationText(controller.packageName)
            ?: ""
        val albumArt = metadata?.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
            ?: metadata?.getBitmap(MediaMetadata.METADATA_KEY_ART)
            ?: metadata?.getBitmap(MediaMetadata.METADATA_KEY_DISPLAY_ICON)
            ?: metadata?.description?.iconBitmap
            ?: loadBitmapFromUri(metadata?.getString(MediaMetadata.METADATA_KEY_ALBUM_ART_URI))
            ?: loadBitmapFromUri(metadata?.getString(MediaMetadata.METADATA_KEY_ART_URI))
            ?: loadBitmapFromUri(metadata?.description?.iconUri?.toString())
            ?: notif?.getLargeIcon()?.loadDrawable(this)?.toBitmap()
            ?: notifExtras?.getParcelable(Notification.EXTRA_LARGE_ICON, Bitmap::class.java)
            ?: getActiveNotificationLargeIcon(controller.packageName)
        val durationMs = metadata?.getLong(MediaMetadata.METADATA_KEY_DURATION) ?: 0L

        val isNewSong = title.isNotBlank() && current.title.isNotBlank() && title != current.title
        val realPosition = calculateRealtimePosition(playbackState)
        val positionMs = if (isNewSong && realPosition > 5_000L) {
            0L
        } else if (durationMs > 0L) {
            realPosition.coerceAtMost(durationMs)
        } else {
            realPosition
        }

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

        val hasChanged = current.title != title ||
                current.artist != artist ||
                current.isPlaying != isPlaying ||
                current.albumArt != albumArt ||
                current.packageName != controller.packageName

        Log.i(TAG, "[updatePlaybackFromController] pkg=${controller.packageName}, isPlaying=$isPlaying, title='$title', artist='$artist', hasChanged=$hasChanged, force=$forceWidgetUpdate, hasArt=${albumArt != null}")

        MediaPlaybackRepository.updatePlaybackState(state)
        if (hasChanged || forceWidgetUpdate) {
            WidgetUpdateHelper.updateAllWidgets(this)
        }
    }

    private fun handleSessionDestroyed() {
        Log.i(TAG, "[handleSessionDestroyed]")
        queryActiveSessions()
        WidgetUpdateHelper.updateAllWidgets(this)
    }

    private fun setupActionHandler(controller: MediaController?) {
        if (controller == null) {
            Log.i(TAG, "[setupActionHandler] Cleared actionHandler")
            MediaPlaybackRepository.setActionHandler(null)
            return
        }

        Log.i(TAG, "[setupActionHandler] Setting actionHandler for ${controller.packageName}")
        MediaPlaybackRepository.setActionHandler(object : MediaPlaybackRepository.ActionHandler {
            override fun onPlayPause() {
                val repositoryState = MediaPlaybackRepository.playbackState.value.isPlaying
                val sessionState = controller.playbackState?.state
                val isCurrentlyPlaying = repositoryState ||
                        sessionState == PlaybackState.STATE_PLAYING ||
                        sessionState == PlaybackState.STATE_BUFFERING ||
                        sessionState == PlaybackState.STATE_CONNECTING

                Log.i(TAG, "[ActionHandler.onPlayPause] pkg=${controller.packageName}, isCurrentlyPlaying=$isCurrentlyPlaying")
                if (isCurrentlyPlaying) {
                    controller.transportControls.pause()
                } else {
                    controller.transportControls.play()
                }
            }

            override fun onSkipToNext() {
                Log.i(TAG, "[ActionHandler.onSkipToNext] pkg=${controller.packageName}")
                controller.transportControls.skipToNext()
            }

            override fun onSkipToPrevious() {
                Log.i(TAG, "[ActionHandler.onSkipToPrevious] pkg=${controller.packageName}")
                controller.transportControls.skipToPrevious()
            }

            override fun onSkipToQueueItem(queueId: Long) {
                Log.i(TAG, "[ActionHandler.onSkipToQueueItem] pkg=${controller.packageName}, queueId=$queueId")
                if (queueId >= 0) {
                    try {
                        controller.transportControls.skipToQueueItem(queueId)
                    } catch (_: Exception) {}
                }
            }

            override fun onSeekTo(positionMs: Long) {
                Log.i(TAG, "[ActionHandler.onSeekTo] pkg=${controller.packageName}, pos=$positionMs")
                MediaPlaybackRepository.updatePosition(positionMs)
                controller.transportControls.seekTo(positionMs)
                WidgetUpdateHelper.updateAllWidgets(this@MediaNotificationListenerService)
            }
        })
    }

    companion object {
        private const val TAG = "MediaNotifListener"
        @Volatile
        private var instance: MediaNotificationListenerService? = null

        fun requestSync(context: Context, forceWidgetUpdate: Boolean = false) {
            val service = instance
            if (service != null) {
                Log.i(TAG, "[requestSync] Triggering queryActiveSessions from existing service instance (force=$forceWidgetUpdate)")
                service.queryActiveSessions(forceWidgetUpdate = forceWidgetUpdate)
            } else {
                Log.i(TAG, "[requestSync] Service instance is null, updating widgets directly")
                WidgetUpdateHelper.updateAllWidgets(context)
            }
        }
    }
}
