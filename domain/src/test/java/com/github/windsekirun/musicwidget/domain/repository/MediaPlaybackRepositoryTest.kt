package com.github.windsekirun.musicwidget.domain.repository

import com.github.windsekirun.musicwidget.domain.model.MediaPlaybackState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MediaPlaybackRepositoryTest {

    @Test
    fun defaultPlaybackState_hasDefaultValues() {
        val repository = DefaultMediaPlaybackRepository()
        val state = repository.playbackState.value

        assertEquals(false, state.isPlaying)
        assertEquals("", state.title)
        assertEquals("", state.artist)
        assertEquals(0L, state.positionMs)
        assertEquals(0L, state.durationMs)
    }

    @Test
    fun updatePlaybackState_updatesFlowValue() {
        val repository = DefaultMediaPlaybackRepository()
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
    fun updatePosition_updatesOnlyPositionMs() {
        val repository = DefaultMediaPlaybackRepository()
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
    fun actionHandler_invokesCallbacks() {
        val repository = DefaultMediaPlaybackRepository()
        var playPauseCalled = false
        var skipNextCalled = false
        var skipPrevCalled = false
        var seekPosition: Long? = null

        repository.setActions(
            playPause = { playPauseCalled = true },
            skipToNext = { skipNextCalled = true },
            skipToPrevious = { skipPrevCalled = true },
            seekTo = { seekPosition = it }
        )

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
    }
}
