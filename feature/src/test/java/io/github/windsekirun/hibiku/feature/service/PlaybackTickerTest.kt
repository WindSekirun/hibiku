package io.github.windsekirun.hibiku.feature.service

import io.github.windsekirun.hibiku.domain.model.MediaPlaybackState
import io.github.windsekirun.hibiku.domain.repository.DefaultMediaPlaybackRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class PlaybackTickerTest {

    private lateinit var repository: DefaultMediaPlaybackRepository
    private lateinit var testScope: CoroutineScope

    @Before
    fun setUp() {
        repository = DefaultMediaPlaybackRepository()
        testScope = CoroutineScope(Job() + Dispatchers.Unconfined)
    }

    @After
    fun tearDown() {
        testScope.cancel()
        repository.reset()
    }

    @Test
    fun ticker_startsWhenScreenOnAndPlaying() {
        repository.updatePlaybackState(
            MediaPlaybackState(
                isPlaying = true,
                positionMs = 1_000L,
                durationMs = 100_000L
            )
        )

        val ticker = PlaybackTicker(
            repository = repository,
            coroutineScope = testScope,
            dispatcher = Dispatchers.Unconfined
        )

        ticker.evaluate()

        assertTrue("Ticker should be running when screen is on and music is playing", ticker.isRunning)
    }

    @Test
    fun ticker_doesNotStartWhenNotPlaying() {
        repository.updatePlaybackState(
            MediaPlaybackState(
                isPlaying = false,
                positionMs = 1_000L,
                durationMs = 100_000L
            )
        )

        val ticker = PlaybackTicker(
            repository = repository,
            coroutineScope = testScope,
            dispatcher = Dispatchers.Unconfined
        )

        ticker.evaluate()

        assertFalse("Ticker should not run when playback is paused", ticker.isRunning)
    }

    @Test
    fun ticker_doesNotStartWhenScreenOff() {
        repository.updatePlaybackState(
            MediaPlaybackState(
                isPlaying = true,
                positionMs = 1_000L,
                durationMs = 100_000L
            )
        )

        val ticker = PlaybackTicker(
            repository = repository,
            coroutineScope = testScope,
            dispatcher = Dispatchers.Unconfined
        )

        ticker.setScreenOn(false)

        assertFalse("Ticker should not run when screen is off", ticker.isRunning)
    }

    @Test
    fun ticker_stopsWhenScreenTurnsOff() {
        repository.updatePlaybackState(
            MediaPlaybackState(
                isPlaying = true,
                positionMs = 1_000L,
                durationMs = 100_000L
            )
        )

        val ticker = PlaybackTicker(
            repository = repository,
            coroutineScope = testScope,
            dispatcher = Dispatchers.Unconfined
        )

        ticker.evaluate()
        assertTrue(ticker.isRunning)

        ticker.setScreenOn(false)
        assertFalse("Ticker should stop when screen turns off", ticker.isRunning)
    }

    @Test
    fun ticker_stopsWhenPlaybackPauses() {
        repository.updatePlaybackState(
            MediaPlaybackState(
                isPlaying = true,
                positionMs = 1_000L,
                durationMs = 100_000L
            )
        )

        val ticker = PlaybackTicker(
            repository = repository,
            coroutineScope = testScope,
            dispatcher = Dispatchers.Unconfined
        )

        ticker.evaluate()
        assertTrue(ticker.isRunning)

        repository.updatePlaybackState(
            repository.playbackState.value.copy(isPlaying = false)
        )
        ticker.onPlaybackStateChanged()

        assertFalse("Ticker should stop when playback is paused", ticker.isRunning)
    }

    @Test
    fun ticker_resumesWhenScreenTurnsBackOn() {
        repository.updatePlaybackState(
            MediaPlaybackState(
                isPlaying = true,
                positionMs = 1_000L,
                durationMs = 100_000L
            )
        )

        val ticker = PlaybackTicker(
            repository = repository,
            coroutineScope = testScope,
            dispatcher = Dispatchers.Unconfined
        )

        ticker.setScreenOn(false)
        assertFalse(ticker.isRunning)

        ticker.setScreenOn(true)
        assertTrue("Ticker should resume when screen turns back on while playing", ticker.isRunning)
    }

    @Test
    fun tick_advancesPositionByInterval() {
        repository.updatePlaybackState(
            MediaPlaybackState(
                isPlaying = true,
                positionMs = 5_000L,
                durationMs = 180_000L
            )
        )

        val ticker = PlaybackTicker(
            repository = repository,
            coroutineScope = testScope,
            dispatcher = Dispatchers.Unconfined,
            tickIntervalMs = 1_000L
        )

        ticker.tick()

        assertEquals(6_000L, repository.playbackState.value.positionMs)
    }

    @Test
    fun tick_clampsToDuration() {
        repository.updatePlaybackState(
            MediaPlaybackState(
                isPlaying = true,
                positionMs = 179_500L,
                durationMs = 180_000L
            )
        )

        val ticker = PlaybackTicker(
            repository = repository,
            coroutineScope = testScope,
            dispatcher = Dispatchers.Unconfined,
            tickIntervalMs = 1_000L
        )

        ticker.tick()

        assertEquals(180_000L, repository.playbackState.value.positionMs)
    }

    @Test
    fun tick_doesNotAdvanceWhenPausedOrScreenOff() {
        repository.updatePlaybackState(
            MediaPlaybackState(
                isPlaying = false,
                positionMs = 5_000L,
                durationMs = 180_000L
            )
        )

        val ticker = PlaybackTicker(
            repository = repository,
            coroutineScope = testScope,
            dispatcher = Dispatchers.Unconfined
        )

        ticker.tick()
        assertEquals(5_000L, repository.playbackState.value.positionMs)

        repository.updatePlaybackState(
            repository.playbackState.value.copy(isPlaying = true)
        )
        ticker.setScreenOn(false)
        ticker.tick()
        assertEquals(5_000L, repository.playbackState.value.positionMs)
    }

    @Test
    fun ticker_runsLoopAndUpdatesPosition() = runBlocking {
        repository.updatePlaybackState(
            MediaPlaybackState(
                isPlaying = true,
                positionMs = 10_000L,
                durationMs = 60_000L
            )
        )

        var tickCount = 0
        val ticker = PlaybackTicker(
            repository = repository,
            coroutineScope = this,
            dispatcher = Dispatchers.Unconfined,
            tickIntervalMs = 1_000L,
            delayProvider = {
                if (tickCount >= 3) {
                    repository.updatePlaybackState(
                        repository.playbackState.value.copy(isPlaying = false)
                    )
                } else {
                    tickCount++
                }
            }
        )

        ticker.evaluate()

        // 3 ticks executed
        assertEquals(13_000L, repository.playbackState.value.positionMs)
        assertFalse(ticker.isRunning)
    }
}
