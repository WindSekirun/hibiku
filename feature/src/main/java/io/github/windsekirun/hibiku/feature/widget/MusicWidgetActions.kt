package io.github.windsekirun.hibiku.feature.widget

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import io.github.windsekirun.hibiku.domain.repository.MediaPlaybackRepository

class PlayPauseActionCallback : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        MediaPlaybackRepository.playPause()
        WidgetUpdateHelper.updateAllWidgets(context)
    }
}

class NextActionCallback : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        MediaPlaybackRepository.skipToNext()
        WidgetUpdateHelper.updateAllWidgets(context)
    }
}

class PrevActionCallback : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        MediaPlaybackRepository.skipToPrevious()
        WidgetUpdateHelper.updateAllWidgets(context)
    }
}

class LaunchPlayerActionCallback : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val state = MediaPlaybackRepository.playbackState.value

        // 1순위: sessionActivity (MediaSession이 제공하는 PendingIntent)
        val sessionActivity = state.sessionActivity
        if (sessionActivity != null) {
            try {
                sessionActivity.send()
                return
            } catch (e: Exception) {
                Log.e("WidgetActions", "Failed to launch sessionActivity PendingIntent", e)
            }
        }

        // 2순위: 현재 재생 중인 앱 패키지로 직접 런치 (YouTube Music 등 대응)
        val musicPackage = state.packageName
        if (!musicPackage.isNullOrBlank()) {
            try {
                val launchIntent = context.packageManager.getLaunchIntentForPackage(musicPackage)
                if (launchIntent != null) {
                    launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(launchIntent)
                    return
                }
            } catch (e: Exception) {
                Log.e("WidgetActions", "Failed to launch music app: $musicPackage", e)
            }
        }

        // 3순위: 자기 앱 MainActivity fallback
        try {
            val launchIntent = Intent().apply {
                component = ComponentName(context.packageName, "io.github.windsekirun.hibiku.MainActivity")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(launchIntent)
        } catch (e: Exception) {
            Log.e("WidgetActions", "Failed to launch main activity", e)
        }
    }
}

class LaunchImmersiveActionCallback : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        try {
            val intent = Intent().apply {
                component = ComponentName(
                    context.packageName,
                    "io.github.windsekirun.hibiku.feature.ui.immersive.ImmersivePlayerActivity"
                )
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e("WidgetActions", "Failed to launch ImmersivePlayerActivity", e)
        }
    }
}

