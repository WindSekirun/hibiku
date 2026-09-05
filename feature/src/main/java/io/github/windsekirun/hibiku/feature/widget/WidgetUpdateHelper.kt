package io.github.windsekirun.hibiku.feature.widget

import android.content.Context
import android.util.Log
import androidx.glance.appwidget.updateAll
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

object WidgetUpdateHelper {
    private const val TAG = "WidgetUpdateHelper"
    private val helperScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    fun updateAllWidgets(context: Context) {
        val appContext = context.applicationContext
        helperScope.launch {
            updateAllWidgetsSuspend(appContext)
        }
    }

    suspend fun updateAllWidgetsSuspend(context: Context) = coroutineScope {
        val j4x2 = async {
            try {
                MusicWidget4x2().updateAll(context)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update MusicWidget4x2", e)
            }
        }

        val j2x2Std = async {
            try {
                MusicWidget2x2Standard().updateAll(context)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update MusicWidget2x2Standard", e)
            }
        }

        val j2x2Pure = async {
            try {
                MusicWidget2x2Pure().updateAll(context)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update MusicWidget2x2Pure", e)
            }
        }

        j4x2.await()
        j2x2Std.await()
        j2x2Pure.await()
    }
}

fun updateAllMusicWidgets(context: Context) {
    WidgetUpdateHelper.updateAllWidgets(context)
}
