package io.github.windsekirun.hibiku.feature.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.util.Log
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.compose
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
                Log.d(TAG, "[WIDGET_PIPELINE] Phase 3: Debouncing update request (120ms)")
                delay(120L.milliseconds)
                // Drain any additional requests queued during debounce
                var latestContext = context
                var drainedCount = 0
                while (true) {
                    val next = updateRequests.tryReceive().getOrNull() ?: break
                    latestContext = next
                    drainedCount++
                }
                if (drainedCount > 0) {
                    Log.d(TAG, "[WIDGET_PIPELINE] Phase 3: Drained $drainedCount burst requests during debounce")
                }

                updateMutex.withLock {
                    try {
                        Log.d(TAG, "[WIDGET_PIPELINE] Phase 4: Executing Glance updateAll()")
                        updateAllWidgetsSuspend(latestContext)
                    } catch (e: Exception) {
                        Log.e(TAG, "[WIDGET_PIPELINE] Error executing Glance updateAll()", e)
                    }
                }
            }
        }
    }

    fun updateAllWidgets(context: Context) {
        val appContext = context.applicationContext
        Log.d(TAG, "[WIDGET_PIPELINE] Phase 3: Request queued in Channel.CONFLATED")
        updateRequests.trySend(appContext)
    }

    suspend fun updateAllWidgetsSuspend(context: Context) {
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val glanceManager = GlanceAppWidgetManager(context)

        val ids4x2 = appWidgetManager.getAppWidgetIds(
            ComponentName(context, MusicWidget4x2Receiver::class.java)
        )
        val ids2x2Std = appWidgetManager.getAppWidgetIds(
            ComponentName(context, MusicWidget2x2StandardReceiver::class.java)
        )
        val ids2x2Pure = appWidgetManager.getAppWidgetIds(
            ComponentName(context, MusicWidget2x2PureReceiver::class.java)
        )

        Log.d(TAG, "[WIDGET_PIPELINE] Phase 4: has4x2=${ids4x2.isNotEmpty()}, has2x2Std=${ids2x2Std.isNotEmpty()}, has2x2Pure=${ids2x2Pure.isNotEmpty()}")

        if (ids4x2.isNotEmpty()) {
            val widget = MusicWidget4x2()
            for (id in ids4x2) {
                try {
                    val glanceId = glanceManager.getGlanceIdBy(id)
                    val options = appWidgetManager.getAppWidgetOptions(id)
                    val rv = widget.compose(context, glanceId, options = options)
                    appWidgetManager.updateAppWidget(id, rv)
                    Log.d(TAG, "[WIDGET_PIPELINE] Phase 4 -> MusicWidget4x2.compose id=$id applied immediately with options")
                } catch (e: Exception) {
                    Log.e(TAG, "[WIDGET_PIPELINE] Direct compose failed for MusicWidget4x2 id=$id", e)
                }
            }
            try {
                widget.updateAll(context)
            } catch (_: Exception) {}
        }

        if (ids2x2Std.isNotEmpty()) {
            val widget = MusicWidget2x2Standard()
            for (id in ids2x2Std) {
                try {
                    val glanceId = glanceManager.getGlanceIdBy(id)
                    val options = appWidgetManager.getAppWidgetOptions(id)
                    val rv = widget.compose(context, glanceId, options = options)
                    appWidgetManager.updateAppWidget(id, rv)
                    Log.d(TAG, "[WIDGET_PIPELINE] Phase 4 -> MusicWidget2x2Standard.compose id=$id applied immediately with options")
                } catch (e: Exception) {
                    Log.e(TAG, "[WIDGET_PIPELINE] Direct compose failed for MusicWidget2x2Standard id=$id", e)
                }
            }
            try {
                widget.updateAll(context)
            } catch (_: Exception) {}
        }

        if (ids2x2Pure.isNotEmpty()) {
            val widget = MusicWidget2x2Pure()
            for (id in ids2x2Pure) {
                try {
                    val glanceId = glanceManager.getGlanceIdBy(id)
                    val options = appWidgetManager.getAppWidgetOptions(id)
                    val rv = widget.compose(context, glanceId, options = options)
                    appWidgetManager.updateAppWidget(id, rv)
                    Log.d(TAG, "[WIDGET_PIPELINE] Phase 4 -> MusicWidget2x2Pure.compose id=$id applied immediately with options")
                } catch (e: Exception) {
                    Log.e(TAG, "[WIDGET_PIPELINE] Direct compose failed for MusicWidget2x2Pure id=$id", e)
                }
            }
            try {
                widget.updateAll(context)
            } catch (_: Exception) {}
        }
    }
}

