package io.github.windsekirun.hibiku.feature.widget

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.action.ActionCallback
import io.github.windsekirun.hibiku.domain.repository.MediaPlaybackRepository
import io.github.windsekirun.hibiku.feature.data.WidgetPreferencesRepository

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
        val sessionActivity = state.sessionActivity
        if (sessionActivity != null) {
            try {
                sessionActivity.send()
                return
            } catch (e: Exception) {
                Log.e("WidgetActions", "Failed to launch sessionActivity PendingIntent", e)
            }
        }

        try {
            val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)
                ?: Intent().apply {
                    component = ComponentName(context.packageName, "io.github.windsekirun.hibiku.MainActivity")
                }
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
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

class ToggleMinimalOverlayActionCallback : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val appWidgetId = runCatching {
            GlanceAppWidgetManager(context).getAppWidgetId(glanceId)
        }.getOrDefault(-1)

        val prefs = WidgetPreferencesRepository(context)
        prefs.toggleMinimalOverlay(appWidgetId)
        WidgetUpdateHelper.updateAllWidgets(context)
    }
}
