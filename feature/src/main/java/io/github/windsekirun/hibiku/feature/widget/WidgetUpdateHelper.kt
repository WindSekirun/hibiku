package io.github.windsekirun.hibiku.feature.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.util.Log
import androidx.glance.appwidget.updateAll
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.time.Duration.Companion.milliseconds

object WidgetUpdateHelper {
    private const val TAG = "WidgetUpdateHelper"
    private val helperScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val updateMutex = Mutex()
    private val updateRequests = Channel<Context>(Channel.CONFLATED)

    init {
        helperScope.launch {
            for (context in updateRequests) {
                // Debounce rapid bursts (e.g. metadata + playbackstate + queue changes all in <100ms)
                delay(120L.milliseconds)
                // Drain any additional requests queued during debounce
                var latestContext = context
                while (true) {
                    val next = updateRequests.tryReceive().getOrNull() ?: break
                    latestContext = next
                }

                updateMutex.withLock {
                    try {
                        updateAllWidgetsSuspend(latestContext)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error updating widgets", e)
                    }
                }
            }
        }
    }

    fun updateAllWidgets(context: Context) {
        val appContext = context.applicationContext
        updateRequests.trySend(appContext)
    }

    suspend fun updateAllWidgetsSuspend(context: Context) {
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val has4x2 = appWidgetManager.getAppWidgetIds(
            ComponentName(context, MusicWidget4x2Receiver::class.java)
        ).isNotEmpty()
        val has2x2Std = appWidgetManager.getAppWidgetIds(
            ComponentName(context, MusicWidget2x2StandardReceiver::class.java)
        ).isNotEmpty()
        val has2x2Pure = appWidgetManager.getAppWidgetIds(
            ComponentName(context, MusicWidget2x2PureReceiver::class.java)
        ).isNotEmpty()

        Log.d(TAG, "[updateAllWidgetsSuspend] has4x2=$has4x2, has2x2Std=$has2x2Std, has2x2Pure=$has2x2Pure")

        if (has4x2) {
            try {
                MusicWidget4x2().updateAll(context)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update MusicWidget4x2", e)
            }
        }

        if (has2x2Std) {
            try {
                MusicWidget2x2Standard().updateAll(context)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update MusicWidget2x2Standard", e)
            }
        }

        if (has2x2Pure) {
            try {
                MusicWidget2x2Pure().updateAll(context)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update MusicWidget2x2Pure", e)
            }
        }
    }
}

