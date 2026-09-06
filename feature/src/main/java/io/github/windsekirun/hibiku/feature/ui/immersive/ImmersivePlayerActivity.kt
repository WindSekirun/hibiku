package io.github.windsekirun.hibiku.feature.ui.immersive

import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import io.github.windsekirun.hibiku.domain.repository.MediaPlaybackRepository
import io.github.windsekirun.hibiku.feature.widget.WidgetUpdateHelper

class ImmersivePlayerActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        window.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))
        enableEdgeToEdge()

        setContent {
            val playbackState by MediaPlaybackRepository.playbackState.collectAsState()

            ImmersivePlayerScreen(
                playbackState = playbackState,
                onClose = { finish() },
                onPlayPause = { MediaPlaybackRepository.playPause() },
                onSkipPrevious = { MediaPlaybackRepository.skipToPrevious() },
                onSkipNext = { MediaPlaybackRepository.skipToNext() },
                onSeek = {
                    MediaPlaybackRepository.seekTo(it)
                    WidgetUpdateHelper.updateAllWidgets(this)
                }
            )
        }
    }

    override fun onPause() {
        super.onPause()
        WidgetUpdateHelper.updateAllWidgets(this)
    }
}
