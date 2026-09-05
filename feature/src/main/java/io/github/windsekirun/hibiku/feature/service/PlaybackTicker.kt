package io.github.windsekirun.hibiku.feature.service

import io.github.windsekirun.hibiku.domain.repository.MediaPlaybackRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class PlaybackTicker(
    private val repository: MediaPlaybackRepository = MediaPlaybackRepository,
    private val coroutineScope: CoroutineScope,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default,
    private val tickIntervalMs: Long = 1_000L,
    private val delayProvider: suspend (Long) -> Unit = { delay(it) },
    private val onTick: (() -> Unit)? = null
) {
    var isScreenOn: Boolean = true
        private set

    val isRunning: Boolean
        get() = tickerJob?.isActive == true

    private var tickerJob: Job? = null

    fun setScreenOn(screenOn: Boolean) {
        if (isScreenOn != screenOn) {
            isScreenOn = screenOn
            evaluate()
            onTick?.invoke()
        }
    }

    fun onPlaybackStateChanged() {
        evaluate()
        onTick?.invoke()
    }

    fun stop() {
        tickerJob?.cancel()
        tickerJob = null
    }

    fun evaluate() {
        val state = repository.playbackState.value
        val shouldRun = isScreenOn && state.isPlaying

        if (shouldRun) {
            if (tickerJob?.isActive != true) {
                startTicker()
            }
        } else {
            stop()
        }
    }

    fun tick() {
        val state = repository.playbackState.value
        if (state.isPlaying && isScreenOn) {
            val newPosition = if (state.durationMs > 0L) {
                (state.positionMs + tickIntervalMs).coerceAtMost(state.durationMs)
            } else {
                state.positionMs + tickIntervalMs
            }
            repository.updatePosition(newPosition)
            onTick?.invoke()
        }
    }

    private fun startTicker() {
        tickerJob?.cancel()
        tickerJob = coroutineScope.launch(dispatcher) {
            while (true) {
                delayProvider(tickIntervalMs)
                val state = repository.playbackState.value
                if (!state.isPlaying || !isScreenOn) {
                    break
                }
                tick()
            }
        }
    }
}
