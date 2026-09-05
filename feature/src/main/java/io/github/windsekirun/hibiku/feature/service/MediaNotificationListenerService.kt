package io.github.windsekirun.hibiku.feature.service

import android.content.ComponentName
import android.content.Context
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.PowerManager
import android.service.notification.NotificationListenerService
import android.util.Log
import androidx.core.content.ContextCompat
import io.github.windsekirun.hibiku.domain.model.MediaPlaybackState
import io.github.windsekirun.hibiku.domain.repository.MediaPlaybackRepository
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
            handlePlaybackStateChanged(state)
        }

        override fun onMetadataChanged(metadata: MediaMetadata?) {
            handleMetadataChanged(metadata)
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
        detachActiveController()
        setupActionHandler(null)
        ticker.stop()
        MediaPlaybackRepository.reset()
        WidgetUpdateHelper.updateAllWidgets(this)
    }

    override fun onDestroy() {
        unregisterScreenReceiver()
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

        val isPlaying = playbackState?.state == PlaybackState.STATE_PLAYING
        val positionMs = playbackState?.position ?: 0L

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

        val isShuffleEnabled = playbackState?.extras?.getBoolean("SHUFFLE_ENABLED") ?: false
        val repeatMode = playbackState?.extras?.getInt("REPEAT_MODE") ?: 0

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
            repeatMode = repeatMode
        )

        MediaPlaybackRepository.updatePlaybackState(state)
        ticker.onPlaybackStateChanged()
        WidgetUpdateHelper.updateAllWidgets(this)
    }

    private fun handlePlaybackStateChanged(state: PlaybackState?) {
        val isPlaying = state?.state == PlaybackState.STATE_PLAYING
        val positionMs = state?.position ?: 0L
        val current = MediaPlaybackRepository.playbackState.value
        val updated = current.copy(
            isPlaying = isPlaying,
            positionMs = positionMs
        )
        MediaPlaybackRepository.updatePlaybackState(updated)
        ticker.onPlaybackStateChanged()
        WidgetUpdateHelper.updateAllWidgets(this)
    }

    private fun handleMetadataChanged(metadata: MediaMetadata?) {
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

        val current = MediaPlaybackRepository.playbackState.value
        val updated = current.copy(
            title = title,
            artist = artist,
            albumArt = albumArt,
            durationMs = durationMs
        )
        MediaPlaybackRepository.updatePlaybackState(updated)
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
                val isPlaying = controller.playbackState?.state == PlaybackState.STATE_PLAYING
                if (isPlaying) {
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
