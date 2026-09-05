package io.github.windsekirun.hibiku.domain.repository

import io.github.windsekirun.hibiku.domain.model.MediaPlaybackState
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MediaPlaybackRepositoryTest {

    @After
    fun tearDown() {
        MediaPlaybackRepository.reset()
    }

    @Test
    fun defaultPlaybackState_hasDefaultValues() {
        val repository: MediaPlaybackRepository = DefaultMediaPlaybackRepository()
        val state = repository.playbackState.value

        assertEquals(false, state.isPlaying)
        assertEquals("", state.title)
        assertEquals("", state.artist)
        assertEquals(0L, state.positionMs)
        assertEquals(0L, state.durationMs)
    }

    @Test
    fun updatePlaybackState_updatesFlowValue() {
        val repository: MediaPlaybackRepository = DefaultMediaPlaybackRepository()
        val newState = MediaPlaybackState(
            isPlaying = true,
            title = "Supernova",
            artist = "aespa",
            positionMs = 15_000L,
            durationMs = 180_000L
        )

        repository.updatePlaybackState(newState)
        assertEquals(newState, repository.playbackState.value)
    }

    @Test
    fun updatePosition_updatesOnlyPositionMsAtomically() {
        val repository: MediaPlaybackRepository = DefaultMediaPlaybackRepository()
        val initialState = MediaPlaybackState(
            isPlaying = true,
            title = "Drama",
            artist = "aespa",
            positionMs = 10_000L,
            durationMs = 200_000L
        )
        repository.updatePlaybackState(initialState)

        repository.updatePosition(25_000L)

        val updated = repository.playbackState.value
        assertEquals(25_000L, updated.positionMs)
        assertEquals(true, updated.isPlaying)
        assertEquals("Drama", updated.title)
        assertEquals("aespa", updated.artist)
        assertEquals(200_000L, updated.durationMs)
    }

    @Test
    fun actionHandler_invokesCallbacksViaInterface() {
        val repository: MediaPlaybackRepository = DefaultMediaPlaybackRepository()
        var playPauseCalled = false
        var skipNextCalled = false
        var skipPrevCalled = false
        var seekPosition: Long? = null

        repository.setActionHandler(object : MediaPlaybackRepository.ActionHandler {
            override fun onPlayPause() { playPauseCalled = true }
            override fun onSkipToNext() { skipNextCalled = true }
            override fun onSkipToPrevious() { skipPrevCalled = true }
            override fun onSeekTo(positionMs: Long) { seekPosition = positionMs }
        })

        repository.playPause()
        repository.skipToNext()
        repository.skipToPrevious()
        repository.seekTo(42_000L)

        assertTrue(playPauseCalled)
        assertTrue(skipNextCalled)
        assertTrue(skipPrevCalled)
        assertEquals(42_000L, seekPosition)
    }

    @Test
    fun reset_clearsPlaybackStateAndActionHandler() {
        val repository: MediaPlaybackRepository = DefaultMediaPlaybackRepository()
        var playPauseCalled = false
        repository.setActions(playPause = { playPauseCalled = true })
        repository.updatePlaybackState(MediaPlaybackState(title = "Whiplash", isPlaying = true))

        repository.reset()

        assertEquals(MediaPlaybackState(), repository.playbackState.value)
        repository.playPause()
        assertEquals(false, playPauseCalled)
    }

    @Test
    fun companionSingleton_worksAsMediaPlaybackRepository() {
        var playPauseInvoked = false
        MediaPlaybackRepository.setActions(
            playPause = { playPauseInvoked = true }
        )

        MediaPlaybackRepository.playPause()
        assertTrue(playPauseInvoked)

        val testState = MediaPlaybackState(title = "Singleton Song")
        MediaPlaybackRepository.updatePlaybackState(testState)
        assertEquals("Singleton Song", MediaPlaybackRepository.playbackState.value.title)

        MediaPlaybackRepository.reset()
        assertEquals("", MediaPlaybackRepository.playbackState.value.title)
    }
}
