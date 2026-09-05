package io.github.windsekirun.hibiku.feature.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import io.github.windsekirun.hibiku.domain.repository.MediaPlaybackRepository
import io.github.windsekirun.hibiku.feature.ui.immersive.ImmersivePlayerActivity

class PowerConnectionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_POWER_CONNECTED) {
            val isPlaying = MediaPlaybackRepository.playbackState.value.isPlaying
            if (isPlaying) {
                val playerIntent = Intent(context, ImmersivePlayerActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                }
                context.startActivity(playerIntent)
            }
        }
    }
}
