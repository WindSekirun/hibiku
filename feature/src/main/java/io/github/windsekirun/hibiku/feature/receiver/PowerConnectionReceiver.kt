package io.github.windsekirun.hibiku.feature.receiver

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import io.github.windsekirun.hibiku.domain.repository.MediaPlaybackRepository
import io.github.windsekirun.hibiku.feature.data.WidgetPreferencesRepository
import io.github.windsekirun.hibiku.feature.ui.immersive.ImmersivePlayerActivity

class PowerConnectionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_POWER_CONNECTED) {
            val repository = WidgetPreferencesRepository(context)
            if (!repository.isAutoLaunchOnChargingEnabled()) {
                return
            }

            val state = MediaPlaybackRepository.playbackState.value
            if (state.isPlaying || state.title.isNotBlank()) {
                val playerIntent = Intent(context, ImmersivePlayerActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
                val launched = runCatching {
                    context.startActivity(playerIntent)
                    true
                }.getOrDefault(false)

                if (!launched) {
                    showChargingNotification(context, playerIntent)
                }
            }
        }
    }

    private fun showChargingNotification(context: Context, intent: Intent) {
        try {
            val channelId = "charging_standby_channel"
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                ?: return

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    channelId,
                    "StandBy Mode Notification",
                    NotificationManager.IMPORTANCE_HIGH
                )
                notificationManager.createNotificationChannel(channel)
            }

            val pendingIntent = PendingIntent.getActivity(
                context,
                1001,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notification = NotificationCompat.Builder(context, channelId)
                .setSmallIcon(android.R.drawable.ic_lock_idle_charging)
                .setContentTitle("⚡ 충전기 연결됨")
                .setContentText("탭하여 스탠바이 이머시브 모드를 시작합니다.")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .build()

            notificationManager.notify(1001, notification)
        } catch (e: Exception) {
            Log.e("PowerConnectionReceiver", "Failed to show charging notification", e)
        }
    }
}
