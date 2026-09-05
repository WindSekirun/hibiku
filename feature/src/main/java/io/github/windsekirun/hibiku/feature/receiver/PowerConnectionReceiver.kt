package io.github.windsekirun.hibiku.feature.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import io.github.windsekirun.hibiku.domain.repository.MediaPlaybackRepository
import io.github.windsekirun.hibiku.feature.ui.immersive.ImmersivePlayerActivity

class PowerConnectionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_POWER_CONNECTED) {
            val state = MediaPlaybackRepository.playbackState.value
            // Launch Immersive StandBy Player when power connected and media session is active or playing
            if (state.isPlaying || state.title.isNotBlank()) {
                val playerIntent = Intent(context, ImmersivePlayerActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
                runCatching {
                    context.startActivity(playerIntent)
                }
            }
        }
    }
}
