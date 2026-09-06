package io.github.windsekirun.hibiku.feature.widget

import io.github.windsekirun.hibiku.domain.repository.MediaPlaybackRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import org.junit.After
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class MusicWidgetTest {

    private lateinit var testScope: CoroutineScope

    @Before
    fun setUp() {
        testScope = CoroutineScope(Job() + Dispatchers.Unconfined)
        MediaPlaybackRepository.reset()
    }

    @After
    fun tearDown() {
        testScope.cancel()
        MediaPlaybackRepository.reset()
    }

    @Test
    fun widgetReceivers_provideCorrectGlanceAppWidgets() {
        val receiver4x2 = MusicWidget4x2Receiver()
        assertNotNull(receiver4x2.glanceAppWidget)
        assertTrue(receiver4x2.glanceAppWidget is MusicWidget4x2)

        val receiver2x2Std = MusicWidget2x2StandardReceiver()
        assertNotNull(receiver2x2Std.glanceAppWidget)
        assertTrue(receiver2x2Std.glanceAppWidget is MusicWidget2x2Standard)
    }

    @Test
    fun mediaPlaybackRepository_actionsTriggerHandlers() {
        var playPauseCalled = false
        var nextCalled = false
        var prevCalled = false

        MediaPlaybackRepository.setActions(
            playPause = { playPauseCalled = true },
            skipToNext = { nextCalled = true },
            skipToPrevious = { prevCalled = true }
        )

        MediaPlaybackRepository.playPause()
        assertTrue("PlayPause should be called", playPauseCalled)

        MediaPlaybackRepository.skipToNext()
        assertTrue("SkipToNext should be called", nextCalled)

        MediaPlaybackRepository.skipToPrevious()
        assertTrue("SkipToPrevious should be called", prevCalled)
    }
}
